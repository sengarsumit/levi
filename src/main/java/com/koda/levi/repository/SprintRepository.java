package com.koda.levi.repository;

import com.koda.levi.entity.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, Long> {
    Optional<Sprint> findByIdAndOrganizationId(Long id, Long organizationId);
    List<Sprint> findByProjectIdAndOrganizationId(Long projectId, Long organizationId);
    List<Sprint> findByOrganizationId(Long organizationId);
}
