package com.matchpoint.groups_service.service;

import com.matchpoint.groups_service.domain.Category;
import com.matchpoint.groups_service.domain.PositionTemplate;
import com.matchpoint.groups_service.domain.enums.Position;
import com.matchpoint.groups_service.dto.request.CategoryRequest;
import com.matchpoint.groups_service.dto.response.CategoryResponse;
import com.matchpoint.groups_service.repository.CategoryRepository;
import com.matchpoint.groups_service.repository.PositionTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private PositionTemplateRepository positionTemplateRepository;

    @InjectMocks
    private CategoryService categoryService;

    private CategoryRequest request;
    private Category savedCategory;

    @BeforeEach
    void setUp() {
        request = new CategoryRequest(
                "F5",
                5,
                List.of("cesped_sintetico", "cemento"),
                Map.of(
                        Position.ARQUERO, 1,
                        Position.DEFENSOR, 2
                )
        );

        savedCategory = new Category();
        savedCategory.setId(1L);
        savedCategory.setName(request.name());
        savedCategory.setPlayersPerTeam(request.playersPerTeam());
        savedCategory.setAllowedSurfaces(request.allowedSurfaces());
    }

    @Test
    @DisplayName("Should persist the Category and one PositionTemplate per positionCounts entry")
    void createCategory_shouldPersistCategoryAndPositionTemplates() {
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        CategoryResponse response = categoryService.createCategory(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("F5");
        assertThat(response.playersPerTeam()).isEqualTo(5);
        assertThat(response.allowedSurfaces()).containsExactlyInAnyOrder("cesped_sintetico", "cemento");

        verify(categoryRepository, times(1)).save(any(Category.class));

        verify(positionTemplateRepository, times(request.positionCounts().size()))
                .save(any(PositionTemplate.class));
    }

    @Test
    @DisplayName("Each saved PositionTemplate should have the correct Category, Position and quantity")
    void createCategory_shouldBuildPositionTemplatesWithCorrectFields() {
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        ArgumentCaptor<PositionTemplate> captor = ArgumentCaptor.forClass(PositionTemplate.class);

        categoryService.createCategory(request);

        verify(positionTemplateRepository, times(2)).save(captor.capture());
        List<PositionTemplate> capturedTemplates = captor.getAllValues();

        assertThat(capturedTemplates).hasSize(2);

        assertThat(capturedTemplates)
                .allSatisfy(template -> assertThat(template.getCategory()).isEqualTo(savedCategory));

        Map<Position, Integer> expected = request.positionCounts();
        capturedTemplates.forEach(template ->
                assertThat(template.getQuantity()).isEqualTo(expected.get(template.getPosition()))
        );
    }

    @Test
    @DisplayName("When positionCounts is empty, the Category is created but no PositionTemplate is saved")
     void createCategory_withEmptyPositionCounts_shouldNotCreateTemplates() {
        CategoryRequest requestWithoutPositions = new CategoryRequest(
                "F5",
                5,
                List.of("cesped_sintetico"),
                Map.of()
        );
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        CategoryResponse response = categoryService.createCategory(requestWithoutPositions);

        assertThat(response.id()).isEqualTo(1L);
        verify(categoryRepository, times(1)).save(any(Category.class));
        verifyNoInteractions(positionTemplateRepository);
    }
}