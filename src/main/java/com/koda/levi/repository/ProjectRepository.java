package com.koda.levi.repository;

import com.koda.levi.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project,Long> {
    Optional<Project> findById(Long id);
    Optional<Project> findByName(String name);
    Optional<Project> findByIdAndOrganizationId(Long id, Long organizationId);
    Optional<Project> findByNameAndOrganizationId(String name, Long organizationId);
    List<Project> findByOrganizationId(Long orgId);
}
