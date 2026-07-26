package com.koda.levi.repository;

import com.koda.levi.entity.OrganizationUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

import com.koda.levi.entity.OrgUserStatus;
import java.util.List;

public interface OrganizationUserRepository extends JpaRepository<OrganizationUser, Long> {
    Optional<OrganizationUser> findByUserIdAndOrganizationId(UUID userId, Long organizationId);
    Optional<OrganizationUser> findByUserIdAndOrganizationSlug(UUID userId, String slug);
    List<OrganizationUser> findByOrganizationIdAndStatus(Long organizationId, OrgUserStatus status);
}
