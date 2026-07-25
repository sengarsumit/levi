package com.koda.levi.service;

import com.koda.levi.dto.WorkflowStateDto;
import com.koda.levi.entity.Board;
import com.koda.levi.entity.Organization;
import com.koda.levi.entity.WorkflowState;
import com.koda.levi.exception.ResourceNotFoundException;
import com.koda.levi.repository.BoardRepository;
import com.koda.levi.repository.OrganizationRepository;
import com.koda.levi.repository.WorkflowStateRepository;
import com.koda.levi.tenancy.TenantAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkflowStateService {

    private final WorkflowStateRepository workflowStateRepository;
    private final BoardRepository boardRepository;
    private final OrganizationRepository organizationRepository;
    private final TenantAccessService tenantAccessService;

    @CacheEvict(value = "workflowStates", key = "#workflowStateDto.boardId")
    public WorkflowState create(WorkflowStateDto workflowStateDto) {
        Long tenantId = tenantAccessService.requireTenantId();

        Organization org = organizationRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + tenantId));

        Board board = boardRepository.findByIdAndOrganizationId(workflowStateDto.getBoardId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found with id: " + workflowStateDto.getBoardId()));

        WorkflowState workflowState = new WorkflowState();
        workflowState.setName(workflowStateDto.getName());
        workflowState.setOrderIndex(workflowStateDto.getOrderIndex());
        workflowState.setBoard(board);
        workflowState.setOrganization(org);
        workflowState.setCreatedAt(LocalDateTime.now());
        workflowState.setUpdatedAt(LocalDateTime.now());

        return workflowStateRepository.save(workflowState);
    }

    public WorkflowState getWorkflowStateById(Long id) {
        Long tenantId = tenantAccessService.requireTenantId();
        return workflowStateRepository.findByIdAndOrganizationId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowState not found with id: " + id));
    }

    @Cacheable(value = "workflowStates", key = "#boardId")
    public List<WorkflowState> getAllWorkflowStatesByBoard(Long boardId) {
        Long tenantId = tenantAccessService.requireTenantId();
        return workflowStateRepository.findByBoardIdAndOrganizationIdOrderByOrderIndexAsc(boardId, tenantId);
    }

    @CacheEvict(value = "workflowStates", key = "#result.board.id")
    public WorkflowState updateWorkflowState(Long id, WorkflowStateDto workflowStateDto) {
        WorkflowState workflowState = getWorkflowStateById(id);
        
        workflowState.setName(workflowStateDto.getName());
        workflowState.setOrderIndex(workflowStateDto.getOrderIndex());
        workflowState.setUpdatedAt(LocalDateTime.now());
        
        return workflowStateRepository.save(workflowState);
    }

    @CacheEvict(value = "workflowStates", allEntries = true)
    public void deleteWorkflowState(Long id) {
        WorkflowState workflowState = getWorkflowStateById(id);
        workflowStateRepository.delete(workflowState);
    }
}
