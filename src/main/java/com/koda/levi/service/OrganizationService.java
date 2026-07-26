package com.koda.levi.service;

import com.koda.levi.dto.OrganizationDto;
import com.koda.levi.entity.Organization;
import com.koda.levi.exception.BadRequestException;
import com.koda.levi.exception.ResourceNotFoundException;
import com.koda.levi.repository.OrganizationRepository;
import com.koda.levi.repository.OrganizationUserRepository;
import com.koda.levi.repository.RoleRepository;
import com.koda.levi.repository.UserRepository;
import com.koda.levi.tenancy.TenantAccessService;
import com.koda.levi.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.UUID;
import com.koda.levi.entity.OrganizationUser;
import com.koda.levi.entity.OrgUserStatus;
import com.koda.levi.entity.Role;
import com.koda.levi.entity.Users;
import com.koda.levi.dto.UserShiftDto;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import jakarta.annotation.PostConstruct;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final TenantAccessService tenantAccessService;

    @PostConstruct
    public void backfillSlugs() {
        List<Organization> orgs = organizationRepository.findAll();
        for (Organization org : orgs) {
            if (org.getSlug() == null || org.getSlug().trim().isEmpty()) {
                String name = org.getName() != null ? org.getName() : "org-" + org.getId();
                String slug = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
                if (slug.isEmpty()) {
                    slug = "org-" + org.getId();
                }
                org.setSlug(slug);
                organizationRepository.save(org);
            }
        }
    }

    @PostConstruct
    public void backfillRoles() {
        List<Organization> orgs = organizationRepository.findAll();
        for (Organization org : orgs) {
            List<Role> roles = roleRepository.findByOrganization(org);
            if (roles.isEmpty()) {
                Role userRole = new Role();
                userRole.setName("User");
                userRole.setOrganization(org);
                userRole.setPermissions(new java.util.HashSet<>(List.of("ISSUE_READ", "ISSUE_UPDATE")));
                roleRepository.save(userRole);

                Role adminRole = new Role();
                adminRole.setName("Admin");
                adminRole.setOrganization(org);
                adminRole.setPermissions(new java.util.HashSet<>(List.of("ISSUE_READ", "ISSUE_CREATE", "ISSUE_UPDATE", "ISSUE_DELETE", "ORG_ADMIN")));
                roleRepository.save(adminRole);
            }
        }
    }

    public Organization createOrganization(OrganizationDto organizationDto) {
        organizationRepository.findByName(organizationDto.getName())
                .ifPresent(org -> {
                    throw new BadRequestException("Organization with name " + organizationDto.getName() + " already exists");
                });

        Organization org = new Organization();
        org.setName(organizationDto.getName());
        String generatedSlug = organizationDto.getName().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        org.setSlug(generatedSlug);
        org.setCreatedAt(LocalDateTime.now());
        org.setUpdatedAt(LocalDateTime.now());
        Organization savedOrg = organizationRepository.save(org);

        // Create default roles
        Role userRole = new Role();
        userRole.setName("User");
        userRole.setOrganization(savedOrg);
        userRole.setPermissions(new java.util.HashSet<>(List.of("ISSUE_READ", "ISSUE_UPDATE")));
        roleRepository.save(userRole);

        Role adminRole = new Role();
        adminRole.setName("Admin");
        adminRole.setOrganization(savedOrg);
        adminRole.setPermissions(new java.util.HashSet<>(List.of("ISSUE_READ", "ISSUE_CREATE", "ISSUE_UPDATE", "ISSUE_DELETE", "ORG_ADMIN")));
        roleRepository.save(adminRole);

        return savedOrg;
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
        org.setName(organizationDto.getName());
        org.setUpdatedAt(LocalDateTime.now());
        return organizationRepository.save(org);
    }

    public void deleteOrganization(Long id) {
        tenantAccessService.requireTenant(id);
        Organization org = getOrganizationById(id);
        organizationRepository.delete(org);
    }
    
    public void joinOrganization(String orgSlug, UUID userId) {
        Organization org = organizationRepository.findBySlug(orgSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                
        if (organizationUserRepository.findByUserIdAndOrganizationId(userId, org.getId()).isPresent()) {
            throw new BadRequestException("User already requested to join or is a member");
        }
        
        OrganizationUser orgUser = new OrganizationUser();
        orgUser.setOrganization(org);
        orgUser.setUser(user);
        orgUser.setStatus(OrgUserStatus.PENDING);
        organizationUserRepository.save(orgUser);
    }
    
    public List<OrganizationUser> getPendingMembers(Long orgId) {
        tenantAccessService.requireTenant(orgId);
        return organizationUserRepository.findByOrganizationIdAndStatus(orgId, OrgUserStatus.PENDING);
    }
    
    public List<OrganizationUser> getActiveMembers(Long orgId) {
        tenantAccessService.requireTenant(orgId);
        return organizationUserRepository.findByOrganizationIdAndStatus(orgId, OrgUserStatus.APPROVED);
    }
    
    public OrganizationUser approveMember(Long orgId, UUID userId, Long roleId) {
        tenantAccessService.requireTenant(orgId);
        OrganizationUser orgUser = organizationUserRepository.findByUserIdAndOrganizationId(userId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization user not found"));
                
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
                
        orgUser.setStatus(OrgUserStatus.APPROVED);
        orgUser.setRole(role);
        return organizationUserRepository.save(orgUser);
    }
    
    public void rejectMember(Long orgId, UUID userId) {
        tenantAccessService.requireTenant(orgId);
        OrganizationUser orgUser = organizationUserRepository.findByUserIdAndOrganizationId(userId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization user not found"));
                
        organizationUserRepository.delete(orgUser);
    }
    
    public OrganizationUser inviteMember(Long orgId, String email, Long roleId) {
        tenantAccessService.requireTenant(orgId);
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                
        Organization org = getOrganizationById(orgId);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
                
        OrganizationUser orgUser = organizationUserRepository.findByUserIdAndOrganizationId(user.getId(), orgId)
                .orElse(new OrganizationUser());
                
        orgUser.setOrganization(org);
        orgUser.setUser(user);
        orgUser.setRole(role);
        orgUser.setStatus(OrgUserStatus.APPROVED);
        return organizationUserRepository.save(orgUser);
    }
    
    public List<Role> getRoles(Long orgId) {
        tenantAccessService.requireTenant(orgId);
        Organization org = getOrganizationById(orgId);
        return roleRepository.findByOrganization(org);
    }
    
    public Role createRole(Long orgId, String name, List<String> permissions) {
        tenantAccessService.requireTenant(orgId);
        Organization org = getOrganizationById(orgId);
        Role role = new Role();
        role.setName(name);
        role.setOrganization(org);
        role.setPermissions(new java.util.HashSet<>(permissions));
        return roleRepository.save(role);
    }

    public void updateUserShift(String orgSlug, UUID userId, UserShiftDto shiftDto) {
        Organization org = organizationRepository.findBySlug(orgSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        OrganizationUser orgUser = organizationUserRepository.findByUserIdAndOrganizationId(userId, org.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found in organization"));

        if (shiftDto.getShiftStartTime() != null && !shiftDto.getShiftStartTime().isEmpty()) {
            orgUser.setShiftStartTime(LocalTime.parse(shiftDto.getShiftStartTime()));
        }
        if (shiftDto.getShiftEndTime() != null && !shiftDto.getShiftEndTime().isEmpty()) {
            orgUser.setShiftEndTime(LocalTime.parse(shiftDto.getShiftEndTime()));
        }
        if (shiftDto.getWorkingDays() != null && !shiftDto.getWorkingDays().isEmpty()) {
            orgUser.setWorkingDays(shiftDto.getWorkingDays());
        }

        organizationUserRepository.save(orgUser);
    }
}
