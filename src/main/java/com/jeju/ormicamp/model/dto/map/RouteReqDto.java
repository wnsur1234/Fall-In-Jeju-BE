package com.jeju.ormicamp.model.dto.map;

import java.util.List;

public record RouteReqDto(
        TransportMode mode,
        PointDto origin,
        PointDto destination,
        List<PointDto> waypoints // 최대 3개, CAR에서만 사용
) {
}
