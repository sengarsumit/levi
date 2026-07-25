package com.koda.levi.repository;

import com.koda.levi.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {
    Optional<Board> findByIdAndOrganizationId(Long id, Long organizationId);
    List<Board> findByProjectIdAndOrganizationId(Long projectId, Long organizationId);
    List<Board> findByOrganizationId(Long organizationId);
}
