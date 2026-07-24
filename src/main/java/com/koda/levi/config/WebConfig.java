package com.koda.levi.config;

import com.koda.levi.tenancy.TenantContextInterceptor;
import com.koda.levi.tenancy.TenantHibernateFilterInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final TenantContextInterceptor tenantContextInterceptor;
    private final TenantHibernateFilterInterceptor tenantHibernateFilterInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantContextInterceptor);
        registry.addInterceptor(tenantHibernateFilterInterceptor);
    }
}
