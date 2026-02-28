package com.jeju.ormicamp.model.dto.map.car;

import com.jeju.ormicamp.model.dto.map.PointDto;

import java.util.List;

public record SimpleRouteResponse(
        List<PointDto> path,
        int distance,
        int duration,
        List<String> instructions,
        boolean available
) {
    public static SimpleRouteResponse unavailable() {
        return new SimpleRouteResponse(List.of(), 0, 0, List.of(), false);
    }
}