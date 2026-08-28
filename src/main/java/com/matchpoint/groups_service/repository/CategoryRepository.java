package com.matchpoint.groups_service.repository;

import com.matchpoint.groups_service.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}