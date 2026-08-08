package com.koda.levi.tenancy;

import com.koda.levi.config.JwtService;
import com.koda.levi.entity.OrganizationUser;
import com.koda.levi.entity.OrgUserStatus;
import com.koda.levi.repository.OrganizationUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TenantContextInterceptor implements HandlerInterceptor {
    public static final String TENANT_HEADER = "X-Tenant-ID";

    private final JwtService jwtService;
    private final OrganizationUserRepository organizationUserRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        TenantContext.clear();

        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isAuthRoute(request) || isOrganizationAccess(request)) {
            return true;
        }

        Optional<Long> headerTenantId = extractFromHeader(request, response);
        if (response.isCommitted()) {
            return false;
        }

        Optional<Long> jwtTenantId = extractFromJwt(request);

        if (headerTenantId.isPresent() && jwtTenantId.isPresent() && !headerTenantId.get().equals(jwtTenantId.get())) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, TENANT_HEADER + " does not match JWT tenant claim");
            return false;
        }

        Optional<Long> tenantId = headerTenantId.or(() -> jwtTenantId);

        if (tenantId.isPresent()) {
            OrganizationUser membership = resolveApprovedMembership(request, tenantId.get());
            if (membership == null) {
                writeError(response, HttpServletResponse.SC_FORBIDDEN, "Not an approved member of this organization");
                return false;
            }
            TenantContext.setTenantId(tenantId.get());
            refreshAuthoritiesForMembership(membership);
            return true;
        }

        if (requiresTenant(request)) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, TENANT_HEADER + " header is required for tenant-scoped routes");
            return false;
        }

        return true;
    }

    /**
     * response.sendError() doesn't commit the response immediately, which previously let
     * Spring's security exception handling override the intended status (documented in
     * FIXES_APPLIED.md for the 400 case) — writing the body directly commits right away and
     * avoids that class of bug for every status this interceptor returns, not just 400.
     */
    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }

    private Optional<Long> extractFromHeader(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String tenantHeader = request.getHeader(TENANT_HEADER);
        if (tenantHeader == null || tenantHeader.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(Long.parseLong(tenantHeader.trim()));
        } catch (NumberFormatException ex) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, TENANT_HEADER + " must be a numeric organization id");
            return Optional.empty();
        }
    }

    private Optional<Long> extractFromJwt(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }

        return jwtService.extractTenantId(authHeader.substring(7));
    }

    /**
     * A resolved tenant id (whether it came from the header or the JWT claim) is only
     * trustworthy if the caller is actually an approved member of that org — otherwise a
     * plain header, or a stale claim from before removal, would grant access on its own.
     */
    private OrganizationUser resolveApprovedMembership(HttpServletRequest request, Long tenantId) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        Optional<UUID> userId = jwtService.extractUserId(authHeader.substring(7));
        if (userId.isEmpty()) {
            return null;
        }

        // Fetch-join variant, not the plain lookup: this interceptor runs before Spring's
        // open-in-view session is established for the request, so a plain lazy fetch of
        // membership.role throws LazyInitializationException ("no Session") the moment
        // refreshAuthoritiesForMembership touches it. Loading role as a real entity in this
        // same query avoids ever needing a later lazy load at all.
        return organizationUserRepository.findByUserIdAndOrganizationIdFetchAll(userId.get(), tenantId)
                .filter(orgUser -> orgUser.getStatus() == OrgUserStatus.APPROVED)
                .orElse(null);
    }

    /**
     * The JWT's "permissions" claim is a snapshot taken at login time — and only populated
     * at all when logging in via the org-scoped /login/:orgSlug route — that never changes
     * for the life of the token. Switching orgs via the workspace switcher, being promoted
     * to Admin mid-session, or just logging in generically all leave it stale or empty, which
     * makes every @PreAuthorize(hasAuthority(...)) check fail for people who are genuinely
     * authorized. Re-deriving authorities from the database on every tenant-scoped request
     * (the same place tenant membership itself is already re-verified live) means those
     * checks always reflect the caller's actual current role for whichever org they're
     * acting on right now, regardless of what their token happened to capture at login.
     */
    private void refreshAuthoritiesForMembership(OrganizationUser membership) {
        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        if (current == null) {
            return;
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (membership.getRole() != null && membership.getRole().getPermissions() != null) {
            for (String permission : membership.getRole().getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(permission));
            }
        }

        Authentication refreshed = new UsernamePasswordAuthenticationToken(
                current.getPrincipal(), current.getCredentials(), authorities);
        SecurityContextHolder.getContext().setAuthentication(refreshed);
    }

    private boolean isAuthRoute(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/auth");
    }

    private boolean isOrganizationAccess(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        if (uri.startsWith("/organization") && uri.endsWith("/join")) {
            return true; // Allow join requests without tenant header
        }
        return ("POST".equalsIgnoreCase(method) || "GET".equalsIgnoreCase(method)) && "/organization".equals(uri);
    }

    private boolean requiresTenant(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/organization")
                || path.startsWith("/projects")
                || path.startsWith("/issues")
                || path.startsWith("/boards")
                || path.startsWith("/workflow-states")
                || path.startsWith("/sprints")
                || path.startsWith("/comments")
                || path.startsWith("/users");
    }
}
