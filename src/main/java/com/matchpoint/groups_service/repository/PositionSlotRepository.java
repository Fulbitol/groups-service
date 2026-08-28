package com.matchpoint.groups_service.repository;

import com.matchpoint.groups_service.domain.PositionSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PositionSlotRepository extends JpaRepository<PositionSlot, Long> {
    List<PositionSlot> findByGroupId(Long groupId);
}