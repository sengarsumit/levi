package com.koda.levi.repository;

import com.koda.levi.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization,Long> {
    Optional<Organization> findByName(String name);
    Optional<Organization> findBySlug(String slug);
}
