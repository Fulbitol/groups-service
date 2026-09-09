// dto/request/JoinRequestRequest.java
package com.matchpoint.groups_service.dto.request;

import com.matchpoint.groups_service.domain.enums.Position;
import jakarta.validation.constraints.NotNull;

public record JoinRequestRequest(
        @NotNull Long playerId,
        Position requestedPosition // opcional: null = "comodín"
) {}