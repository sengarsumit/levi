package com.koda.levi.tenancy;

import com.koda.levi.exception.BadRequestException;
import com.koda.levi.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class TenantAccessService {
    public Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BadRequestException(TenantContextInterceptor.TENANT_HEADER + " header is required for tenant-scoped routes");
        }
        return tenantId;
    }

    public void requireTenant(Long organizationId) {
        Long tenantId = requireTenantId();
        if (organizationId == null || !tenantId.equals(organizationId)) {
            throw new ResourceNotFoundException("Resource not found in current tenant");
        }
    }
}
