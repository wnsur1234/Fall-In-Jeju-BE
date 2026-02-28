package com.jeju.ormicamp.model.dto.map;

import com.jeju.ormicamp.model.dto.map.transit.RouteInstructionDto;

import java.util.List;

public record RouteResponse(
        List<PointDto> path,
        int distance,
        int duration,
        List<RouteInstructionDto> instructions,
        boolean available
) {
    public static RouteResponse unavailable() {
        return new RouteResponse(
                List.of(),
                0,
                0,
                List.of(),
                false
        );
    }
}
