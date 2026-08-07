package com.koda.levi.repository;

import com.koda.levi.entity.OrganizationUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

import com.koda.levi.entity.OrgUserStatus;
import java.util.List;

public interface OrganizationUserRepository extends JpaRepository<OrganizationUser, Long> {
    Optional<OrganizationUser> findByUserIdAndOrganizationId(UUID userId, Long organizationId);
    Optional<OrganizationUser> findByUserIdAndOrganizationSlug(UUID userId, String slug);
    List<OrganizationUser> findByOrganizationIdAndStatus(Long organizationId, OrgUserStatus status);
    List<OrganizationUser> findByUserIdAndStatus(UUID userId, OrgUserStatus status);

    // Scalar projection: reads the organization_id FK without ever materializing an
    // Organization proxy in the persistence context (see getAllOrganizations for why that
    // matters — a later direct fetch of the same id would otherwise reuse that cached proxy).
    @Query("SELECT ou.organization.id FROM OrganizationUser ou WHERE ou.user.id = :userId AND ou.status = :status")
    List<Long> findOrganizationIdsByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") OrgUserStatus status);

    // organization/user/role are all FetchType.LAZY and get serialized as nested JSON objects
    // (the frontend reads member.user.name, member.organization.slug, member.role.name,
    // etc.), so — unlike the scalar projection above — these need to come back as real,
    // non-proxy entities. A fetch join loads them all in the same query instead of leaving
    // any of them as lazy proxies that fail Jackson serialization the moment nothing else
    // has already warmed the persistence context for that id. role is LEFT (not INNER)
    // because a freshly-created pending row can still have a null role in principle.
    @Query("SELECT ou FROM OrganizationUser ou " +
            "LEFT JOIN FETCH ou.organization " +
            "LEFT JOIN FETCH ou.user " +
            "LEFT JOIN FETCH ou.role " +
            "WHERE ou.organization.id = :organizationId AND ou.status = :status")
    List<OrganizationUser> findByOrganizationIdAndStatusFetchAll(@Param("organizationId") Long organizationId, @Param("status") OrgUserStatus status);

    @Query("SELECT ou FROM OrganizationUser ou " +
            "LEFT JOIN FETCH ou.organization " +
            "LEFT JOIN FETCH ou.user " +
            "LEFT JOIN FETCH ou.role " +
            "WHERE ou.user.id = :userId AND ou.organization.id = :organizationId")
    Optional<OrganizationUser> findByUserIdAndOrganizationIdFetchAll(@Param("userId") UUID userId, @Param("organizationId") Long organizationId);
}
