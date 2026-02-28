package com.jeju.ormicamp.model.dto.map.transit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TransitRawResponse {

    public MetaData metaData;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MetaData {
        public Plan plan;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Plan {
        public List<Itinerary> itineraries;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Itinerary {
        public int totalDistance;
        public int totalTime;
        public List<Leg> legs;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Leg {
        public String mode;              // WALK, BUS
        public int distance;
        public int sectionTime;

        public Route route;              // BUS 정보
        public Place start;
        public Place end;
        public Integer passStopCount;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Route {
        public String routeName;         // 예: 365
        public String routeId;           // 노선 ID (있으면)
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Place {
        public String name;
    }
}