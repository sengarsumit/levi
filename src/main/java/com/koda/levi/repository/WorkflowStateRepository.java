package com.koda.levi.repository;

import com.koda.levi.entity.WorkflowState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowStateRepository extends JpaRepository<WorkflowState, Long> {
    Optional<WorkflowState> findByIdAndOrganizationId(Long id, Long organizationId);
    List<WorkflowState> findByBoardIdAndOrganizationIdOrderByOrderIndexAsc(Long boardId, Long organizationId);
    List<WorkflowState> findByOrganizationId(Long organizationId);
}
