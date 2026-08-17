package com.koda.levi.service;

import com.koda.levi.dto.OrganizationDto;
import com.koda.levi.dto.RoleSummaryDto;
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
import org.springframework.transaction.annotation.Transactional;
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

    private static final java.util.Set<String> DEFAULT_USER_PERMISSIONS =
            new java.util.HashSet<>(List.of("ISSUE_READ", "ISSUE_UPDATE", "TASK_TRANSITION"));
    private static final java.util.Set<String> DEFAULT_ADMIN_PERMISSIONS =
            new java.util.HashSet<>(List.of("ISSUE_READ", "ISSUE_CREATE", "ISSUE_UPDATE", "ISSUE_DELETE",
                    "TASK_TRANSITION", "PROJECT_CREATE", "ORG_ADMIN"));

    @PostConstruct
    public void backfillRoles() {
        List<Organization> orgs = organizationRepository.findAll();
        for (Organization org : orgs) {
            List<Role> roles = roleRepository.findByOrganization(org);
            if (roles.isEmpty()) {
                Role userRole = new Role();
                userRole.setName("User");
                userRole.setOrganization(org);
                userRole.setPermissions(new java.util.HashSet<>(DEFAULT_USER_PERMISSIONS));
                roleRepository.save(userRole);

                Role adminRole = new Role();
                adminRole.setName("Admin");
                adminRole.setOrganization(org);
                adminRole.setPermissions(new java.util.HashSet<>(DEFAULT_ADMIN_PERMISSIONS));
                roleRepository.save(adminRole);
            }
        }
    }

    @Transactional
    public Organization createOrganization(OrganizationDto organizationDto, Users creator) {
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
        userRole.setPermissions(new java.util.HashSet<>(DEFAULT_USER_PERMISSIONS));
        roleRepository.save(userRole);

        Role adminRole = new Role();
        adminRole.setName("Admin");
        adminRole.setOrganization(savedOrg);
        adminRole.setPermissions(new java.util.HashSet<>(DEFAULT_ADMIN_PERMISSIONS));
        roleRepository.save(adminRole);

        // The creator becomes an approved Admin member of the org they just created —
        // without this there is no admin left to approve anyone else who joins.
        OrganizationUser creatorMembership = new OrganizationUser();
        creatorMembership.setOrganization(savedOrg);
        creatorMembership.setUser(creator);
        creatorMembership.setRole(adminRole);
        creatorMembership.setStatus(OrgUserStatus.APPROVED);
        organizationUserRepository.save(creatorMembership);

        return savedOrg;
    }

    public Organization getOrganizationById(Long id) {
        if (TenantContext.hasTenant()) {
            tenantAccessService.requireTenant(id);
        }
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + id));
    }

    public List<Organization> getAllOrganizations(UUID callerId) {
        if (TenantContext.hasTenant()) {
            return List.of(getOrganizationById(tenantAccessService.requireTenantId()));
        }
        // No tenant context yet (e.g. right after a plain login, before a workspace is
        // selected): return the orgs this specific user actually belongs to, not every org
        // in the system — that was the original bug, not a "browse all orgs" feature.
        if (callerId == null) {
            return List.of();
        }
        // Fetch real Organization rows via a scalar id projection rather than navigating
        // OrganizationUser.getOrganization() — that association is FetchType.LAZY, and
        // touching it creates a Hibernate proxy that serializes with a Jackson proxy-class
        // error (and, since the persistence context caches by id, a later findAllById for
        // the same ids would just return that same cached proxy instead of a real entity).
        List<Long> orgIds = organizationUserRepository.findOrganizationIdsByUserIdAndStatus(callerId, OrgUserStatus.APPROVED);
        return organizationRepository.findAllById(orgIds);
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
    
    public void joinOrganization(String orgSlug, UUID userId, Long preferredRoleId) {
        Organization org = organizationRepository.findBySlug(orgSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (organizationUserRepository.findByUserIdAndOrganizationId(userId, org.getId()).isPresent()) {
            throw new BadRequestException("User already requested to join or is a member");
        }

        // role_id is NOT NULL in the database even though the entity marks it nullable, so a
        // pending request needs a real role from the start. The approving admin picks/confirms
        // the actual role anyway (approveMember always overwrites this), so this placeholder —
        // the requester's preference if they gave one, else the org's "User" role — is a
        // signal for the admin, not a real grant.
        List<Role> orgRoles = roleRepository.findByOrganization(org);
        Role placeholderRole;
        if (preferredRoleId != null) {
            placeholderRole = orgRoles.stream()
                    .filter(r -> r.getId().equals(preferredRoleId))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("Preferred role does not belong to this organization"));
        } else {
            placeholderRole = orgRoles.stream()
                    .filter(r -> "User".equals(r.getName()))
                    .findFirst()
                    .orElseGet(() -> orgRoles.stream().findFirst()
                            .orElseThrow(() -> new ResourceNotFoundException("Organization has no roles configured")));
        }

        OrganizationUser orgUser = new OrganizationUser();
        orgUser.setOrganization(org);
        orgUser.setUser(user);
        orgUser.setRole(placeholderRole);
        orgUser.setStatus(OrgUserStatus.PENDING);
        organizationUserRepository.save(orgUser);
    }

    public List<RoleSummaryDto> getRolesForJoin(String orgSlug) {
        Organization org = organizationRepository.findBySlug(orgSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        return roleRepository.findByOrganization(org).stream()
                .map(r -> RoleSummaryDto.builder().id(r.getId()).name(r.getName()).build())
                .toList();
    }
    
    public List<OrganizationUser> getPendingMembers(Long orgId) {
        tenantAccessService.requireTenant(orgId);
        return organizationUserRepository.findByOrganizationIdAndStatusFetchAll(orgId, OrgUserStatus.PENDING);
    }

    public List<OrganizationUser> getActiveMembers(Long orgId) {
        tenantAccessService.requireTenant(orgId);
        return organizationUserRepository.findByOrganizationIdAndStatusFetchAll(orgId, OrgUserStatus.APPROVED);
    }

    public OrganizationUser approveMember(Long orgId, UUID userId, Long roleId) {
        tenantAccessService.requireTenant(orgId);
        OrganizationUser orgUser = organizationUserRepository.findByUserIdAndOrganizationIdFetchAll(userId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization user not found"));

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        if (role.getOrganization().getId() != orgId) {
            throw new BadRequestException("Role does not belong to this organization");
        }

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
        if (role.getOrganization().getId() != orgId) {
            throw new BadRequestException("Role does not belong to this organization");
        }

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
        tenantAccessService.requireTenant(org.getId());

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
