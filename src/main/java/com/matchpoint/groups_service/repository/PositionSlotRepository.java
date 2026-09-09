package com.matchpoint.groups_service.repository;

import com.matchpoint.groups_service.domain.PositionSlot;
import com.matchpoint.groups_service.domain.enums.Position;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PositionSlotRepository extends JpaRepository<PositionSlot, Long> {
    List<PositionSlot> findByGroupId(Long groupId);
    Optional<PositionSlot> findByGroupIdAndPosition(Long groupId, Position position);
}