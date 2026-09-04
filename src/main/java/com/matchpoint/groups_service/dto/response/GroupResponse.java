package com.matchpoint.groups_service.dto.response;

import java.time.LocalDateTime;

public record GroupResponse(
        Long id,
        Long categoryId,
        String categoryName,
        String surface,
        Double latitude,
        Double longitude,
        String zoneName,
        LocalDateTime scheduledAt,
        Integer minLevel,
        Integer maxLevel,
        Integer maxPlayers,
        Integer currentPlayers,
        String status
) {}