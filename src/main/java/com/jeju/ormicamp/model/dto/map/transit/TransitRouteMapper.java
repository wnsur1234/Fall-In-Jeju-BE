package com.jeju.ormicamp.model.dto.map.transit;

import com.jeju.ormicamp.model.dto.map.RouteResponse;

import java.util.ArrayList;
import java.util.List;

public class TransitRouteMapper {

    private TransitRouteMapper() {}

    public static RouteResponse toRouteResponse(TransitRawResponse raw) {

        if (raw == null
                || raw.metaData == null
                || raw.metaData.plan == null
                || raw.metaData.plan.itineraries == null
                || raw.metaData.plan.itineraries.isEmpty()) {
            return RouteResponse.unavailable();
        }

        TransitRawResponse.Itinerary it = raw.metaData.plan.itineraries.stream()
                .filter(i -> i.legs != null && !i.legs.isEmpty())
                .findFirst()
                .orElse(null);

        if (it == null) {
            return new RouteResponse(
                    List.of(),
                    raw.metaData.plan.itineraries.get(0).totalDistance,
                    raw.metaData.plan.itineraries.get(0).totalTime,
                    List.of(),   // 안내 없음
                    true
            );
        }

        List<RouteInstructionDto> instructions = new ArrayList<>();

        for (TransitRawResponse.Leg leg : it.legs) {

            if ("WALK".equals(leg.mode)) {
                instructions.add(
                        new RouteInstructionDto(
                                "WALK",
                                "도보 " + leg.distance + "m 이동 (약 " +
                                        (leg.sectionTime / 60) + "분)",
                                null,
                                null
                        )
                );
            }

            if ("BUS".equals(leg.mode)) {
                instructions.add(
                        new RouteInstructionDto(
                                "BUS",
                                "버스 " + leg.route.routeName + "번 승차 (" +
                                        leg.start.name + ")",
                                leg.route.routeName,
                                leg.route.routeId
                        )
                );

                instructions.add(
                        new RouteInstructionDto(
                                "BUS",
                                leg.passStopCount + "개 정류장 이동 (약 " +
                                        (leg.sectionTime / 60) + "분)",
                                leg.route.routeName,
                                leg.route.routeId
                        )
                );
            }
        }

        instructions.add(
                new RouteInstructionDto(
                        "END",
                        "목적지 도착",
                        null,
                        null
                )
        );

        return new RouteResponse(
                List.of(),
                it.totalDistance,
                it.totalTime,
                instructions,
                true
        );
    }
}