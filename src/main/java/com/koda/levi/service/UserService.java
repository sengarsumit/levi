package com.koda.levi.service;

import com.koda.levi.dto.UserResponseDto;
import com.koda.levi.entity.OrgUserStatus;
import com.koda.levi.entity.Users;
import com.koda.levi.exception.ResourceNotFoundException;
import com.koda.levi.repository.OrganizationUserRepository;
import com.koda.levi.repository.UserRepository;
import com.koda.levi.tenancy.TenantAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final TenantAccessService tenantAccessService;

    /**
     * Scoped to the current org's approved members — this list is only ever consumed for
     * things like assignee/activity lookups within an org, never a platform-wide directory.
     */
    public List<UserResponseDto> getAllUsers() {
        Long tenantId = tenantAccessService.requireTenantId();
        return organizationUserRepository.findByOrganizationIdAndStatus(tenantId, OrgUserStatus.APPROVED).stream()
                .map(orgUser -> mapToDto(orgUser.getUser()))
                .collect(Collectors.toList());
    }

    public UserResponseDto getUserById(UUID id) {
        Users user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapToDto(user);
    }

    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    private UserResponseDto mapToDto(Users user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .systemId(user.getSystemId())
                .name(user.getName())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
