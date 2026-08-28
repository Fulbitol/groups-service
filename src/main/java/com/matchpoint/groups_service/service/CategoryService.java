package com.matchpoint.groups_service.service;

import com.matchpoint.groups_service.domain.Category;
import com.matchpoint.groups_service.domain.Position;
import com.matchpoint.groups_service.domain.PositionTemplate;
import com.matchpoint.groups_service.dto.CategoryRequest;
import com.matchpoint.groups_service.dto.CategoryResponse;
import com.matchpoint.groups_service.repository.CategoryRepository;
import com.matchpoint.groups_service.repository.PositionTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final PositionTemplateRepository positionTemplateRepository;

    public CategoryService(CategoryRepository categoryRepository,
                           PositionTemplateRepository positionTemplateRepository) {
        this.categoryRepository = categoryRepository;
        this.positionTemplateRepository = positionTemplateRepository;
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        log.info(
                "Creating category '{}' with {} position templates",
                request.name(),
                request.positionCounts().size()
        );

        Category category = new Category();
        category.setName(request.name());
        category.setPlayersPerTeam(request.playersPerTeam());
        category.setAllowedSurfaces(request.allowedSurfaces());

        Category saved = categoryRepository.save(category);
        log.debug("Category persisted with id={}", saved.getId());

        for (Map.Entry<Position, Integer> entry : request.positionCounts().entrySet()) {
            PositionTemplate template = new PositionTemplate();
            template.setCategory(saved);
            template.setPosition(entry.getKey());
            template.setQuantity(entry.getValue());
            positionTemplateRepository.save(template);
        }

        log.info(
                "Category '{}' created successfully with id={}",
                saved.getName(),
                saved.getId()
        );
        return toResponse(saved);
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getPlayersPerTeam(),
                category.getAllowedSurfaces()
        );
    }
}
