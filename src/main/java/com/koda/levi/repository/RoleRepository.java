package com.koda.levi.repository;

import com.koda.levi.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import com.koda.levi.entity.Organization;
import java.util.List;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByNameAndOrganizationId(String name, Long organizationId);
    List<Role> findByOrganization(Organization organization);
}
