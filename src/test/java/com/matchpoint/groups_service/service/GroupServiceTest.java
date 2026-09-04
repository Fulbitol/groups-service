package com.matchpoint.groups_service.service;

import com.matchpoint.groups_service.domain.Category;
import com.matchpoint.groups_service.domain.Group;
import com.matchpoint.groups_service.domain.PositionSlot;
import com.matchpoint.groups_service.domain.PositionTemplate;
import com.matchpoint.groups_service.domain.enums.GroupStatus;
import com.matchpoint.groups_service.domain.enums.Position;
import com.matchpoint.groups_service.dto.request.GroupRequest;
import com.matchpoint.groups_service.dto.response.GroupResponse;
import com.matchpoint.groups_service.exception.ResourceNotFoundException;
import com.matchpoint.groups_service.repository.CategoryRepository;
import com.matchpoint.groups_service.repository.GroupRepository;
import com.matchpoint.groups_service.repository.PositionSlotRepository;
import com.matchpoint.groups_service.repository.PositionTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private PositionTemplateRepository positionTemplateRepository;

    @Mock
    private PositionSlotRepository positionSlotRepository;

    @InjectMocks
    private GroupService groupService;

    private GroupRequest request;
    private Category category;
    private Group savedGroup;

    @BeforeEach
    void setUp() {
        request = new GroupRequest(
                1L,
                "cesped_sintetico",
                -34.72,
                -58.25,
                "Quilmes",
                LocalDateTime.now().plusDays(1),
                1,
                5,
                10
        );

        category = new Category();
        category.setId(1L);
        category.setName("F5");
        category.setPlayersPerTeam(5);

        savedGroup = new Group();
        savedGroup.setId(100L);
        savedGroup.setCategory(category);
        savedGroup.setSurface(request.surface());
        savedGroup.setLatitude(request.latitude());
        savedGroup.setLongitude(request.longitude());
        savedGroup.setZoneName(request.zoneName());
        savedGroup.setScheduledAt(request.scheduledAt());
        savedGroup.setMinLevel(request.minLevel());
        savedGroup.setMaxLevel(request.maxLevel());
        savedGroup.setMaxPlayers(request.maxPlayers());
        savedGroup.setCurrentPlayers(0);
        savedGroup.setStatus(GroupStatus.OPEN);
    }

    @Test
    @DisplayName("Should persist the Group and one PositionSlot per PositionTemplate of the category")
    void createGroup_shouldPersistGroupAndPositionSlots() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(groupRepository.save(any(Group.class))).thenReturn(savedGroup);

        List<PositionTemplate> templates = List.of(
                templateOf(Position.ARQUERO, 1),
                templateOf(Position.DEFENSOR, 2)
        );
        when(positionTemplateRepository.findByCategoryId(1L)).thenReturn(templates);

        GroupResponse response = groupService.createGroup(request);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.categoryId()).isEqualTo(1L);
        assertThat(response.categoryName()).isEqualTo("F5");
        assertThat(response.status()).isEqualTo("OPEN");
        assertThat(response.currentPlayers()).isZero();

        verify(groupRepository, times(1)).save(any(Group.class));
        verify(positionSlotRepository, times(templates.size())).save(any(PositionSlot.class));
    }

    @Test
    @DisplayName("Each saved PositionSlot should have the correct Group, Position and totalSlots")
    void createGroup_shouldBuildPositionSlotsWithCorrectFields() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(groupRepository.save(any(Group.class))).thenReturn(savedGroup);

        List<PositionTemplate> templates = List.of(
                templateOf(Position.ARQUERO, 1),
                templateOf(Position.DEFENSOR, 2)
        );
        when(positionTemplateRepository.findByCategoryId(1L)).thenReturn(templates);

        ArgumentCaptor<PositionSlot> captor = ArgumentCaptor.forClass(PositionSlot.class);

        groupService.createGroup(request);

        verify(positionSlotRepository, times(2)).save(captor.capture());
        List<PositionSlot> capturedSlots = captor.getAllValues();

        assertThat(capturedSlots).hasSize(2);
        assertThat(capturedSlots).allSatisfy(slot -> {
            assertThat(slot.getGroup()).isEqualTo(savedGroup);
            assertThat(slot.getFilledSlots()).isZero();
        });

        assertThat(capturedSlots)
                .filteredOn(slot -> slot.getPosition() == Position.ARQUERO)
                .singleElement()
                .satisfies(slot -> assertThat(slot.getTotalSlots()).isEqualTo(1));

        assertThat(capturedSlots)
                .filteredOn(slot -> slot.getPosition() == Position.DEFENSOR)
                .singleElement()
                .satisfies(slot -> assertThat(slot.getTotalSlots()).isEqualTo(2));
    }

    @Test
    @DisplayName("When the category has no PositionTemplates, the Group is created but no PositionSlot is saved")
    void createGroup_withNoPositionTemplates_shouldNotCreateSlots() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(groupRepository.save(any(Group.class))).thenReturn(savedGroup);
        when(positionTemplateRepository.findByCategoryId(1L)).thenReturn(List.of());

        GroupResponse response = groupService.createGroup(request);

        assertThat(response.id()).isEqualTo(100L);
        verify(groupRepository, times(1)).save(any(Group.class));
        verifyNoInteractions(positionSlotRepository);
    }

    @Test
    @DisplayName("When the categoryId does not exist, should throw ResourceNotFoundException and not persist anything")
    void createGroup_withNonExistentCategory_shouldThrowResourceNotFoundException() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.createGroup(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(groupRepository);
        verifyNoInteractions(positionTemplateRepository);
        verifyNoInteractions(positionSlotRepository);
    }

    private PositionTemplate templateOf(Position position, int quantity) {
        PositionTemplate template = new PositionTemplate();
        template.setCategory(category);
        template.setPosition(position);
        template.setQuantity(quantity);
        return template;
    }
}