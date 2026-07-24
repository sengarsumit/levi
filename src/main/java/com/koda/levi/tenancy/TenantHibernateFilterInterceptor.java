package com.koda.levi.tenancy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TenantHibernateFilterInterceptor implements HandlerInterceptor {
    public static final String TENANT_FILTER = "tenantFilter";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Session session = entityManager.unwrap(Session.class);

        if (TenantContext.hasTenant()) {
            session.enableFilter(TENANT_FILTER)
                    .setParameter("tenantId", TenantContext.getTenantId());
        } else if (session.getEnabledFilter(TENANT_FILTER) != null) {
            session.disableFilter(TENANT_FILTER);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (entityManager.isOpen()) {
            Session session = entityManager.unwrap(Session.class);
            if (session.getEnabledFilter(TENANT_FILTER) != null) {
                session.disableFilter(TENANT_FILTER);
            }
        }
    }
}
