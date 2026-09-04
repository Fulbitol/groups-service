package com.matchpoint.groups_service.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record GroupRequest(
        @NotNull Long categoryId,
        @NotBlank String surface,
        @NotNull Double latitude,
        @NotNull Double longitude,
        String zoneName,
        @NotNull @Future LocalDateTime scheduledAt,
        Integer minLevel,
        Integer maxLevel,
        @NotNull @Min(1) Integer maxPlayers
) {}