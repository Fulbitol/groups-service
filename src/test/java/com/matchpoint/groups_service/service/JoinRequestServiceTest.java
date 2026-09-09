package com.matchpoint.groups_service.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JoinRequestServiceTest {

    @Mock
    private JoinRequestRepository joinRequestRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private PositionSlotRepository positionSlotRepository;

    @InjectMocks
    private JoinRequestService joinRequestService;

    private Group group;
    private JoinRequest pendingJoinRequest;

    @BeforeEach
    void setUp() {
        group = new Group();
        group.setId(10L);
        group.setMaxPlayers(10);
        group.setCurrentPlayers(5);
        group.setStatus(GroupStatus.OPEN);

        pendingJoinRequest = new JoinRequest();
        pendingJoinRequest.setId(1L);
        pendingJoinRequest.setGroup(group);
        pendingJoinRequest.setPlayerId(99L);
        pendingJoinRequest.setStatus(JoinRequestStatus.PENDING);
        pendingJoinRequest.setCreatedAt(LocalDateTime.now());
    }

    // ---------- createJoinRequest ----------

    @Test
    @DisplayName("Should create a JoinRequest with PENDING status")
    void createJoinRequest_shouldPersistWithPendingStatus() {
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(joinRequestRepository.save(any(JoinRequest.class))).thenReturn(pendingJoinRequest);

        JoinRequestRequest request = new JoinRequestRequest(99L, null);
        JoinRequestResponse response = joinRequestService.createJoinRequest(10L, request);

        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.playerId()).isEqualTo(99L);
        verify(joinRequestRepository, times(1)).save(any(JoinRequest.class));
    }

    @Test
    @DisplayName("When the group does not exist, should throw ResourceNotFoundException")
    void createJoinRequest_withNonExistentGroup_shouldThrow() {
        when(groupRepository.findById(10L)).thenReturn(Optional.empty());

        JoinRequestRequest request = new JoinRequestRequest(99L, null);

        assertThatThrownBy(() -> joinRequestService.createJoinRequest(10L, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(joinRequestRepository);
    }

    // ---------- acceptJoinRequest ----------

    @Test
    @DisplayName("Should accept a wildcard (null position) request and just increment currentPlayers")
    void acceptJoinRequest_withNullPosition_shouldIncrementCurrentPlayersOnly() {
        when(joinRequestRepository.findById(1L)).thenReturn(Optional.of(pendingJoinRequest));
        when(joinRequestRepository.save(any(JoinRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        JoinRequestResponse response = joinRequestService.acceptJoinRequest(1L);

        assertThat(response.status()).isEqualTo("ACCEPTED");
        assertThat(group.getCurrentPlayers()).isEqualTo(6);
        assertThat(group.getStatus()).isEqualTo(GroupStatus.OPEN);
        verify(groupRepository, times(1)).save(group);
        verifyNoInteractions(positionSlotRepository);
    }

    @Test
    @DisplayName("Should accept a positional request with an available slot and increment filledSlots")
    void acceptJoinRequest_withAvailableSlot_shouldIncrementFilledSlots() {
        pendingJoinRequest.setRequestedPosition(Position.ARQUERO);

        PositionSlot slot = new PositionSlot();
        slot.setGroup(group);
        slot.setPosition(Position.ARQUERO);
        slot.setTotalSlots(1);
        slot.setFilledSlots(0);

        when(joinRequestRepository.findById(1L)).thenReturn(Optional.of(pendingJoinRequest));
        when(positionSlotRepository.findByGroupIdAndPosition(10L, Position.ARQUERO))
                .thenReturn(Optional.of(slot));
        when(joinRequestRepository.save(any(JoinRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        joinRequestService.acceptJoinRequest(1L);

        assertThat(slot.getFilledSlots()).isEqualTo(1);
        verify(positionSlotRepository, times(1)).save(slot);
    }

    @Test
    @DisplayName("Should accept a positional request whose slot is already full, without touching the slot")
    void acceptJoinRequest_withFullSlot_shouldNotIncrementFilledSlots() {
        pendingJoinRequest.setRequestedPosition(Position.ARQUERO);

        PositionSlot slot = new PositionSlot();
        slot.setGroup(group);
        slot.setPosition(Position.ARQUERO);
        slot.setTotalSlots(1);
        slot.setFilledSlots(1); // ya cubierto

        when(joinRequestRepository.findById(1L)).thenReturn(Optional.of(pendingJoinRequest));
        when(positionSlotRepository.findByGroupIdAndPosition(10L, Position.ARQUERO))
                .thenReturn(Optional.of(slot));
        when(joinRequestRepository.save(any(JoinRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        joinRequestService.acceptJoinRequest(1L);

        assertThat(slot.getFilledSlots()).isEqualTo(1); // sin cambios
        verify(positionSlotRepository, never()).save(any(PositionSlot.class));
    }

    @Test
    @DisplayName("Should mark the group as FULL when accepting reaches maxPlayers")
    void acceptJoinRequest_whenReachingMaxPlayers_shouldMarkGroupAsFull() {
        group.setCurrentPlayers(9); // maxPlayers = 10

        when(joinRequestRepository.findById(1L)).thenReturn(Optional.of(pendingJoinRequest));
        when(joinRequestRepository.save(any(JoinRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        joinRequestService.acceptJoinRequest(1L);

        assertThat(group.getCurrentPlayers()).isEqualTo(10);
        assertThat(group.getStatus()).isEqualTo(GroupStatus.FULL);
    }

    @Test
    @DisplayName("When the group is already full, should throw BusinessRuleException")
    void acceptJoinRequest_whenGroupIsFull_shouldThrowBusinessRuleException() {
        group.setCurrentPlayers(10); // maxPlayers = 10

        when(joinRequestRepository.findById(1L)).thenReturn(Optional.of(pendingJoinRequest));

        assertThatThrownBy(() -> joinRequestService.acceptJoinRequest(1L))
                .isInstanceOf(BusinessRuleException.class);

        verify(groupRepository, never()).save(any(Group.class));
        verify(joinRequestRepository, never()).save(any(JoinRequest.class));
    }

    @Test
    @DisplayName("When the JoinRequest was already processed, should throw BusinessRuleException")
    void acceptJoinRequest_whenAlreadyProcessed_shouldThrowBusinessRuleException() {
        pendingJoinRequest.setStatus(JoinRequestStatus.ACCEPTED);

        when(joinRequestRepository.findById(1L)).thenReturn(Optional.of(pendingJoinRequest));

        assertThatThrownBy(() -> joinRequestService.acceptJoinRequest(1L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("When the JoinRequest does not exist, should throw ResourceNotFoundException")
    void acceptJoinRequest_withNonExistentId_shouldThrowResourceNotFoundException() {
        when(joinRequestRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> joinRequestService.acceptJoinRequest(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- rejectJoinRequest ----------

    @Test
    @DisplayName("Should reject a pending JoinRequest without touching Group or PositionSlot")
    void rejectJoinRequest_shouldMarkAsRejected() {
        when(joinRequestRepository.findById(1L)).thenReturn(Optional.of(pendingJoinRequest));
        when(joinRequestRepository.save(any(JoinRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        JoinRequestResponse response = joinRequestService.rejectJoinRequest(1L);

        assertThat(response.status()).isEqualTo("REJECTED");
        verifyNoInteractions(groupRepository);
        verifyNoInteractions(positionSlotRepository);
    }
}