package com.jeju.ormicamp.model.dto.map.transit;

import java.util.ArrayList;
import java.util.List;

public class TransitInstructionBuilder {

    private TransitInstructionBuilder() {}

    public static List<String> build(TransitRawResponse.Itinerary itinerary) {

        List<String> instructions = new ArrayList<>();

        if (itinerary.legs == null || itinerary.legs.isEmpty()) {
            return List.of("대중교통 경로 정보가 없습니다.");
        }

        for (TransitRawResponse.Leg leg : itinerary.legs) {

            switch (leg.mode) {

                case "WALK" -> {
                    int meters = leg.distance;
                    instructions.add("도보 " + meters + "m 이동");
                }

                case "BUS", "SUBWAY" -> {
                    String routeName =
                            leg.route != null ? leg.route.routeName : "노선 정보 없음";
                    int minutes = Math.max(1, leg.sectionTime / 60);

                    instructions.add(
                            (leg.mode.equals("BUS") ? "버스 " : "지하철 ")
                                    + routeName
                                    + " 탑승 (" + minutes + "분)"
                    );
                }

                default -> instructions.add("이동");
            }
        }

        return instructions;
    }
}
