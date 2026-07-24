package com.koda.levi.repository;

import com.koda.levi.entity.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Integer>, JpaSpecificationExecutor<Issue> {
    List<Issue> findByProjectsId(Long projectId);
    List<Issue> findByProjects_Organization_Id(Long organizationId);
    List<Issue> findByAssignedTo(UUID assignedTo);
    List<Issue> findByOrganizationId(Long organizationId);
    Optional<Issue> findByIdAndOrganizationId(Integer id, Long organizationId);
    List<Issue> findByProjectsIdAndOrganizationId(Long projectId, Long organizationId);
    List<Issue> findByAssignedToAndOrganizationId(UUID assignedTo, Long organizationId);
}
