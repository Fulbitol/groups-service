package com.matchpoint.groups_service.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "position_slots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PositionSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Position position;

    @Column(name = "total_slots", nullable = false)
    private Integer totalSlots;

    @Column(name = "filled_slots", nullable = false)
    private Integer filledSlots = 0;

    public boolean hasAvailableSlot() {
        return filledSlots < totalSlots;
    }
}