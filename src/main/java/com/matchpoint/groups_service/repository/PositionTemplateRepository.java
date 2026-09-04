package com.matchpoint.groups_service.repository;

import com.matchpoint.groups_service.domain.PositionTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PositionTemplateRepository extends JpaRepository<PositionTemplate, Long> {
    List<PositionTemplate> findByCategoryId(Long categoryId);
}
