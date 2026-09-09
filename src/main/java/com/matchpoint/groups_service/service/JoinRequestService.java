package com.matchpoint.groups_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matchpoint.groups_service.domain.Group;
import com.matchpoint.groups_service.domain.JoinRequest;
import com.matchpoint.groups_service.domain.PositionSlot;
import com.matchpoint.groups_service.domain.enums.GroupStatus;
import com.matchpoint.groups_service.domain.enums.JoinRequestStatus;
import com.matchpoint.groups_service.domain.enums.Position;
import com.matchpoint.groups_service.dto.request.JoinRequestRequest;
import com.matchpoint.groups_service.dto.response.JoinRequestResponse;
import com.matchpoint.groups_service.exception.BusinessRuleException;
import com.matchpoint.groups_service.exception.ResourceNotFoundException;
import com.matchpoint.groups_service.repository.GroupRepository;
import com.matchpoint.groups_service.repository.JoinRequestRepository;
import com.matchpoint.groups_service.repository.PositionSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class JoinRequestService {

    private static final Logger log = LoggerFactory.getLogger(JoinRequestService.class);

    private final JoinRequestRepository joinRequestRepository;
    private final GroupRepository groupRepository;
    private final PositionSlotRepository positionSlotRepository;

    public JoinRequestService(JoinRequestRepository joinRequestRepository,
                              GroupRepository groupRepository,
                              PositionSlotRepository positionSlotRepository) {
        this.joinRequestRepository = joinRequestRepository;
        this.groupRepository = groupRepository;
        this.positionSlotRepository = positionSlotRepository;
    }

    @Transactional
    public JoinRequestResponse createJoinRequest(Long groupId, JoinRequestRequest request) {
        log.info("Creating join request for groupId={}, playerId={}", groupId, request.playerId());

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> ResourceNotFoundException.of("Group", groupId));

        JoinRequest joinRequest = new JoinRequest();
        joinRequest.setGroup(group);
        joinRequest.setPlayerId(request.playerId());
        joinRequest.setRequestedPosition(request.requestedPosition());
        joinRequest.setStatus(JoinRequestStatus.PENDING);
        joinRequest.setCreatedAt(LocalDateTime.now());

        JoinRequest saved = joinRequestRepository.save(joinRequest);
        log.info("JoinRequest id={} created with status PENDING", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public JoinRequestResponse acceptJoinRequest(Long joinRequestId) {
        JoinRequest joinRequest = getPendingOrThrow(joinRequestId);
        Group group = joinRequest.getGroup();

        if (group.isFull()) {
            throw new BusinessRuleException("Group is already full");
        }

        group.setCurrentPlayers(group.getCurrentPlayers() + 1);
        if (group.isFull()) {
            group.setStatus(GroupStatus.FULL);
        }
        groupRepository.save(group);

        Position requestedPosition = joinRequest.getRequestedPosition();
        if (requestedPosition != null) {
            positionSlotRepository.findByGroupIdAndPosition(group.getId(), requestedPosition)
                    .filter(PositionSlot::hasAvailableSlot)
                    .ifPresent(slot -> {
                        slot.setFilledSlots(slot.getFilledSlots() + 1);
                        positionSlotRepository.save(slot);
                    });
        }

        joinRequest.setStatus(JoinRequestStatus.ACCEPTED);
        JoinRequest saved = joinRequestRepository.save(joinRequest);
        log.info("JoinRequest id={} ACCEPTED, group id={} now has {}/{} players",
                saved.getId(), group.getId(), group.getCurrentPlayers(), group.getMaxPlayers());
        return toResponse(saved);
    }

    @Transactional
    public JoinRequestResponse rejectJoinRequest(Long joinRequestId) {
        JoinRequest joinRequest = getPendingOrThrow(joinRequestId);

        joinRequest.setStatus(JoinRequestStatus.REJECTED);
        JoinRequest saved = joinRequestRepository.save(joinRequest);
        log.info("JoinRequest id={} REJECTED", saved.getId());
        return toResponse(saved);
    }

    private JoinRequest getPendingOrThrow(Long joinRequestId) {
        JoinRequest joinRequest = joinRequestRepository.findById(joinRequestId)
                .orElseThrow(() -> ResourceNotFoundException.of("JoinRequest", joinRequestId));

        if (joinRequest.getStatus() != JoinRequestStatus.PENDING) {
            throw new BusinessRuleException("Join request has already been processed");
        }
        return joinRequest;
    }

    private JoinRequestResponse toResponse(JoinRequest joinRequest) {
        return new JoinRequestResponse(
                joinRequest.getId(),
                joinRequest.getGroup().getId(),
                joinRequest.getPlayerId(),
                joinRequest.getRequestedPosition(),
                joinRequest.getStatus().name(),
                joinRequest.getCreatedAt()
        );
    }
}