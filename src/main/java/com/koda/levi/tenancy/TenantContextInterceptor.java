package com.koda.levi.tenancy;

import com.koda.levi.config.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TenantContextInterceptor implements HandlerInterceptor {
    public static final String TENANT_HEADER = "X-Tenant-ID";

    private final JwtService jwtService;

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
            response.sendError(HttpServletResponse.SC_FORBIDDEN, TENANT_HEADER + " does not match JWT tenant claim");
            return false;
        }

        Optional<Long> tenantId = headerTenantId.or(() -> jwtTenantId);

        if (tenantId.isPresent()) {
            TenantContext.setTenantId(tenantId.get());
            return true;
        }

        if (requiresTenant(request)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"" + TENANT_HEADER + " header is required for tenant-scoped routes\"}");
            return false;
        }

        return true;
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
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"" + TENANT_HEADER + " must be a numeric organization id\"}");
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
                || path.startsWith("/comments");
    }
}
