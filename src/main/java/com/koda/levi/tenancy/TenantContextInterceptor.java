package com.koda.levi.tenancy;

import com.koda.levi.config.JwtService;
import com.koda.levi.entity.OrgUserStatus;
import com.koda.levi.repository.OrganizationUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
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
            if (!isApprovedMember(request, tenantId.get())) {
                writeError(response, HttpServletResponse.SC_FORBIDDEN, "Not an approved member of this organization");
                return false;
            }
            TenantContext.setTenantId(tenantId.get());
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
    private boolean isApprovedMember(HttpServletRequest request, Long tenantId) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }

        Optional<UUID> userId = jwtService.extractUserId(authHeader.substring(7));
        if (userId.isEmpty()) {
            return false;
        }

        return organizationUserRepository.findByUserIdAndOrganizationId(userId.get(), tenantId)
                .map(orgUser -> orgUser.getStatus() == OrgUserStatus.APPROVED)
                .orElse(false);
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
