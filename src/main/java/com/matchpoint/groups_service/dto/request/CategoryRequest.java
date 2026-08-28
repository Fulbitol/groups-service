package com.matchpoint.groups_service.dto.request;

import com.matchpoint.groups_service.domain.enums.Position;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record CategoryRequest(
        @NotBlank String name,
        @NotNull Integer playersPerTeam,
        @NotEmpty List<String> allowedSurfaces,
        @NotEmpty Map<Position, Integer> positionCounts // ej: {"ARQUERO": 1, "DEFENSOR": 2}
) {}
