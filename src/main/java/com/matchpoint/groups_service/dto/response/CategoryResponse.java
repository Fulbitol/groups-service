package com.matchpoint.groups_service.dto.response;

import java.util.List;

public record CategoryResponse(
        Long id,
        String name,
        Integer playersPerTeam,
        List<String> allowedSurfaces
) {}