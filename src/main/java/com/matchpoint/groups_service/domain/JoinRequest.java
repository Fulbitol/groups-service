package com.matchpoint.groups_service.domain;

import com.matchpoint.groups_service.domain.enums.JoinRequestStatus;
import com.matchpoint.groups_service.domain.enums.Position;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "join_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JoinRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_position") // sin nullable=false: es opcional ("comodín" si viene null)
    private Position requestedPosition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JoinRequestStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}