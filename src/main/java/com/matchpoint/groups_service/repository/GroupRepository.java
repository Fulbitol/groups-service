package com.matchpoint.groups_service.repository;

import com.matchpoint.groups_service.domain.Group;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<Group, Long> {
}