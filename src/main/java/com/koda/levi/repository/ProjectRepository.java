package com.koda.levi.repository;

import com.koda.levi.entity.Projects;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface ProjectRepository extends JpaRepository<Projects,Long> {

    Optional<Projects> findById(Long id);
    Optional<Projects> findByName(String name);
    Optional<Projects> findByIdAndOrganizationId(Long id, Long organizationId);
    Optional<Projects> findByNameAndOrganizationId(String name, Long organizationId);
    List<Projects> findByOrganizationId(Long orgId);

}
