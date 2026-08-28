package com.matchpoint.groups_service.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    @Id
    @GeneratedValue(strategy =  GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // Futbol5, Futbol7, Futbol8, Futbol11

    @Column(name = "players_per_team", nullable = false)
    private Integer playersPerTeam;

    @ElementCollection
    @CollectionTable(name = "category_surfaces", joinColumns = @JoinColumn(name = "category_id"))
    @Column(name = "surface")
    private List<String> allowedSurfaces;
}
