package com.matchpoint.groups_service.repository;

import com.matchpoint.groups_service.domain.JoinRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JoinRequestRepository extends JpaRepository<JoinRequest, Long> {
}
