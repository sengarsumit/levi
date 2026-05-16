package com.koda.levi.repository;

import com.koda.levi.entity.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Integer> {
    List<Issue> findByProjectsId(Long projectId);
    List<Issue> findByProjects_Organization_Id(Long organizationId);
}
