package com.koda.levi.service;

import com.koda.levi.dto.BoardDto;
import com.koda.levi.entity.Board;
import com.koda.levi.entity.Organization;
import com.koda.levi.entity.Project;
import com.koda.levi.exception.ResourceNotFoundException;
import com.koda.levi.repository.BoardRepository;
import com.koda.levi.repository.OrganizationRepository;
import com.koda.levi.repository.ProjectRepository;
import com.koda.levi.tenancy.TenantAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final ProjectRepository projectRepository;
    private final OrganizationRepository organizationRepository;
    private final TenantAccessService tenantAccessService;

    public Board create(BoardDto boardDto) {
        Long tenantId = tenantAccessService.requireTenantId();

        Organization org = organizationRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + tenantId));

        Project project = projectRepository.findByIdAndOrganizationId(boardDto.getProjectId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + boardDto.getProjectId()));

        Board board = new Board();
        board.setName(boardDto.getName());
        board.setProject(project);
        board.setOrganization(org);
        board.setCreatedAt(LocalDateTime.now());
        board.setUpdatedAt(LocalDateTime.now());

        return boardRepository.save(board);
    }

    public Board getBoardById(Long id) {
        Long tenantId = tenantAccessService.requireTenantId();
        return boardRepository.findByIdAndOrganizationId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found with id: " + id));
    }

    public List<Board> getAllBoardsByProject(Long projectId) {
        Long tenantId = tenantAccessService.requireTenantId();
        return boardRepository.findByProjectIdAndOrganizationId(projectId, tenantId);
    }

    public Board updateBoard(Long id, BoardDto boardDto) {
        Board board = getBoardById(id);
        
        board.setName(boardDto.getName());
        board.setUpdatedAt(LocalDateTime.now());
        
        return boardRepository.save(board);
    }

    public void deleteBoard(Long id) {
        Board board = getBoardById(id);
        boardRepository.delete(board);
    }
}
