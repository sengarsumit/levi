package com.koda.levi.repository;

import com.koda.levi.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    List<ProjectMember> findByProjectId(Long projectId);

    boolean existsByProjectIdAndUserId(Long projectId, UUID userId);

    @Transactional
    void deleteByProjectIdAndUserId(Long projectId, UUID userId);

    // Scalar projection: reads the project_id FK directly without materializing Project
    // proxies, mirroring OrganizationUserRepository.findOrganizationIdsByUserIdAndStatus.
    @Query("SELECT pm.project.id FROM ProjectMember pm WHERE pm.userId = :userId")
    List<Long> findProjectIdsByUserId(@Param("userId") UUID userId);
}
