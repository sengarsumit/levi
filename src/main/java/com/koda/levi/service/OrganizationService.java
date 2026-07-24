package com.koda.levi.service;

import com.koda.levi.dto.OrganizationDto;
import com.koda.levi.entity.Organization;
import com.koda.levi.exception.BadRequestException;
import com.koda.levi.exception.ResourceNotFoundException;
import com.koda.levi.repository.OrganizationRepository;
import com.koda.levi.tenancy.TenantAccessService;
import com.koda.levi.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final TenantAccessService tenantAccessService;

    public Organization createOrganization(OrganizationDto organizationDto) {
        organizationRepository.findByName(organizationDto.getOrgName())
                .ifPresent(org -> {
                    throw new BadRequestException("Organization with name " + organizationDto.getOrgName() + " already exists");
                });

        Organization org = new Organization();
        org.setName(organizationDto.getOrgName());
        org.setCreatedAt(LocalDateTime.now());
        org.setUpdatedAt(LocalDateTime.now());
        return organizationRepository.save(org);
    }

    public Organization getOrganizationById(Long id) {
        if (TenantContext.hasTenant()) {
            tenantAccessService.requireTenant(id);
        }
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + id));
    }

    public List<Organization> getAllOrganizations() {
        if (TenantContext.hasTenant()) {
            return List.of(getOrganizationById(tenantAccessService.requireTenantId()));
        }
        return organizationRepository.findAll();
    }

    public Organization updateOrganization(Long id, OrganizationDto organizationDto) {
        tenantAccessService.requireTenant(id);
        Organization org = getOrganizationById(id);
        org.setName(organizationDto.getOrgName());
        org.setUpdatedAt(LocalDateTime.now());
        return organizationRepository.save(org);
    }

    public void deleteOrganization(Long id) {
        tenantAccessService.requireTenant(id);
        Organization org = getOrganizationById(id);
        organizationRepository.delete(org);
    }
}
