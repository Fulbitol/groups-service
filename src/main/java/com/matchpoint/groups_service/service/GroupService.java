package com.matchpoint.groups_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matchpoint.groups_service.domain.Category;
import com.matchpoint.groups_service.domain.Group;
import com.matchpoint.groups_service.domain.PositionSlot;
import com.matchpoint.groups_service.domain.PositionTemplate;
import com.matchpoint.groups_service.domain.enums.GroupStatus;
import com.matchpoint.groups_service.dto.request.GroupRequest;
import com.matchpoint.groups_service.dto.response.GroupResponse;
import com.matchpoint.groups_service.exception.ResourceNotFoundException;
import com.matchpoint.groups_service.repository.CategoryRepository;
import com.matchpoint.groups_service.repository.GroupRepository;
import com.matchpoint.groups_service.repository.PositionSlotRepository;
import com.matchpoint.groups_service.repository.PositionTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GroupService {

    private static final Logger log = LoggerFactory.getLogger(GroupService.class);

    private final GroupRepository groupRepository;
    private final CategoryRepository categoryRepository;
    private final PositionTemplateRepository positionTemplateRepository;
    private final PositionSlotRepository positionSlotRepository;

    public GroupService(GroupRepository groupRepository,
                        CategoryRepository categoryRepository,
                        PositionTemplateRepository positionTemplateRepository,
                        PositionSlotRepository positionSlotRepository) {
        this.groupRepository = groupRepository;
        this.categoryRepository = categoryRepository;
        this.positionTemplateRepository = positionTemplateRepository;
        this.positionSlotRepository = positionSlotRepository;
    }

    @Transactional
    public GroupResponse createGroup(GroupRequest request) {
        log.info("Creating group for categoryId={}", request.categoryId());

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> ResourceNotFoundException.of("Category", request.categoryId()));

        Group group = new Group();
        group.setCategory(category);
        group.setSurface(request.surface());
        group.setLatitude(request.latitude());
        group.setLongitude(request.longitude());
        group.setZoneName(request.zoneName());
        group.setScheduledAt(request.scheduledAt());
        group.setMinLevel(request.minLevel());
        group.setMaxLevel(request.maxLevel());
        group.setMaxPlayers(request.maxPlayers());
        group.setCurrentPlayers(0);
        group.setStatus(GroupStatus.OPEN);

        Group savedGroup = groupRepository.save(group);
        log.debug("Group persisted with id={}", savedGroup.getId());

        List<PositionTemplate> templates = positionTemplateRepository.findByCategoryId(category.getId());

        for (PositionTemplate template : templates) {
            PositionSlot slot = new PositionSlot();
            slot.setGroup(savedGroup);
            slot.setPosition(template.getPosition());
            slot.setTotalSlots(template.getQuantity());
            slot.setFilledSlots(0);
            positionSlotRepository.save(slot);
        }

        log.info(
                "Group id={} created successfully with {} position slots",
                savedGroup.getId(),
                templates.size()
        );
        return toResponse(savedGroup);
    }

    private GroupResponse toResponse(Group group) {
        return new GroupResponse(
                group.getId(),
                group.getCategory().getId(),
                group.getCategory().getName(),
                group.getSurface(),
                group.getLatitude(),
                group.getLongitude(),
                group.getZoneName(),
                group.getScheduledAt(),
                group.getMinLevel(),
                group.getMaxLevel(),
                group.getMaxPlayers(),
                group.getCurrentPlayers(),
                group.getStatus().name()
        );
    }
}