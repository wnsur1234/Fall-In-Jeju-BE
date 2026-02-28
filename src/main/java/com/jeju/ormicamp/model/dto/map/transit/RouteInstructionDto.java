package com.jeju.ormicamp.model.dto.map.transit;

public record RouteInstructionDto(
        String type,        // WALK, BUS
        String text,        // 표시 문구
        String routeName,   // 버스 번호 (BUS만)
        String routeId      // 버스 노선 ID (BUS만)
) {}
