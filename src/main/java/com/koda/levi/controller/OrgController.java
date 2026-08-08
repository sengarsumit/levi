package com.koda.levi.controller;

import com.koda.levi.dto.OrganizationDto;
import com.koda.levi.entity.Organization;
import com.koda.levi.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.koda.levi.entity.OrganizationUser;
import com.koda.levi.entity.Role;
import com.koda.levi.entity.Users;
import com.koda.levi.dto.UserShiftDto;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.Map;
import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/organization")
@RequiredArgsConstructor
public class OrgController {

    private final OrganizationService organizationService;

    @PostMapping
    public ResponseEntity<Organization> createOrganization(@Valid @RequestBody OrganizationDto organizationDto, @AuthenticationPrincipal Users user) {
        return ResponseEntity.ok(organizationService.createOrganization(organizationDto, user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Organization> getOrganizationById(@PathVariable Long id) {
        return ResponseEntity.ok(organizationService.getOrganizationById(id));
    }

    @GetMapping
    public ResponseEntity<List<Organization>> getAllOrganizations(@AuthenticationPrincipal Users user) {
        return ResponseEntity.ok(organizationService.getAllOrganizations(user != null ? user.getId() : null));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ORG_ADMIN')")
    public ResponseEntity<Organization> updateOrganization(@PathVariable Long id, @Valid @RequestBody OrganizationDto organizationDto) {
        return ResponseEntity.ok(organizationService.updateOrganization(id, organizationDto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ORG_ADMIN')")
    public ResponseEntity<Void> deleteOrganization(@PathVariable Long id) {
        organizationService.deleteOrganization(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{orgSlug}/join")
    public ResponseEntity<Void> joinOrganization(@PathVariable String orgSlug, @AuthenticationPrincipal Users user) {
        organizationService.joinOrganization(orgSlug, user.getId());
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{orgId}/members/pending")
    @PreAuthorize("hasAuthority('ORG_ADMIN')")
    public ResponseEntity<List<OrganizationUser>> getPendingMembers(@PathVariable Long orgId) {
        return ResponseEntity.ok(organizationService.getPendingMembers(orgId));
    }

    @GetMapping("/{orgId}/members/active")
    @PreAuthorize("hasAuthority('ORG_ADMIN')")
    public ResponseEntity<List<OrganizationUser>> getActiveMembers(@PathVariable Long orgId) {
        return ResponseEntity.ok(organizationService.getActiveMembers(orgId));
    }
    
    @PatchMapping("/{orgId}/members/{userId}/approve")
    @PreAuthorize("hasAuthority('ORG_ADMIN')")
    public ResponseEntity<OrganizationUser> approveMember(@PathVariable Long orgId, @PathVariable UUID userId, @RequestBody Map<String, Long> body) {
        Long roleId = body.get("roleId");
        if (roleId == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(organizationService.approveMember(orgId, userId, roleId));
    }

    @PatchMapping("/{orgId}/members/{userId}/reject")
    @PreAuthorize("hasAuthority('ORG_ADMIN')")
    public ResponseEntity<Void> rejectMember(@PathVariable Long orgId, @PathVariable UUID userId) {
        organizationService.rejectMember(orgId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{orgId}/invite")
    @PreAuthorize("hasAuthority('ORG_ADMIN')")
    public ResponseEntity<OrganizationUser> inviteMember(@PathVariable Long orgId, @RequestBody Map<String, Object> body) {
        String email = (String) body.get("email");
        Long roleId = Long.valueOf(body.get("roleId").toString());
        return ResponseEntity.ok(organizationService.inviteMember(orgId, email, roleId));
    }

    @GetMapping("/{orgId}/roles")
    @PreAuthorize("hasAuthority('ORG_ADMIN')")
    public ResponseEntity<List<Role>> getRoles(@PathVariable Long orgId) {
        return ResponseEntity.ok(organizationService.getRoles(orgId));
    }

    @PostMapping("/{orgId}/roles")
    @PreAuthorize("hasAuthority('ORG_ADMIN')")
    public ResponseEntity<Role> createRole(@PathVariable Long orgId, @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        List<String> permissions = (List<String>) body.get("permissions");
        return ResponseEntity.ok(organizationService.createRole(orgId, name, permissions));
    }

    @PutMapping("/{orgSlug}/users/{userId}/shift")
    public ResponseEntity<Void> updateUserShift(
            @PathVariable String orgSlug,
            @PathVariable UUID userId,
            @RequestBody UserShiftDto shiftDto
    ) {
        organizationService.updateUserShift(orgSlug, userId, shiftDto);
        return ResponseEntity.ok().build();
    }
}
