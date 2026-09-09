// dto/response/JoinRequestResponse.java
package com.matchpoint.groups_service.dto.response;

import com.matchpoint.groups_service.domain.enums.Position;

import java.time.LocalDateTime;

public record JoinRequestResponse(
        Long id,
        Long groupId,
        Long playerId,
        Position requestedPosition,
        String status,
        LocalDateTime createdAt
) {}