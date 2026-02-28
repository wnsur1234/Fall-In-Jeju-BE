package com.jeju.ormicamp.model.dto.map;

import java.util.ArrayList;
import java.util.List;

public class TmapRouteMapper {

    private TmapRouteMapper() {}

    @SuppressWarnings("unchecked")
    public static RouteResponse toRouteResponse(TmapRawRouteResponse raw) {

        if (raw == null || raw.getFeatures() == null || raw.getFeatures().isEmpty()) {
            return RouteResponse.unavailable();
        }

        List<PointDto> path = new ArrayList<>();
        List<String> instructions = new ArrayList<>();
        int totalDistance = 0;
        int totalTime = 0;

        for (var feature : raw.getFeatures()) {
            if (feature == null) continue;

            // =========================
            // GEOMETRY
            // =========================
            var geometry = feature.getGeometry();
            if (geometry != null && geometry.getCoordinates() != null) {

                Object coordsObj = geometry.getCoordinates();
                String type = geometry.getType();

                // LineString: [[lng, lat], [lng, lat], ...]
                if ("LineString".equals(type) && coordsObj instanceof List<?> list) {
                    for (Object o : list) {
                        if (!(o instanceof List<?> coord) || coord.size() < 2) continue;

                        double lng = ((Number) coord.get(0)).doubleValue();
                        double lat = ((Number) coord.get(1)).doubleValue();
                        path.add(new PointDto(lat, lng));
                    }
                }

                // Point: [lng, lat]
                else if ("Point".equals(type) && coordsObj instanceof List<?> coord && coord.size() >= 2) {
                    double lng = ((Number) coord.get(0)).doubleValue();
                    double lat = ((Number) coord.get(1)).doubleValue();
                    path.add(new PointDto(lat, lng));
                }
            }

            // =========================
            // PROPERTIES
            // =========================
            var props = feature.getProperties();
            if (props != null) {
                if (props.getDistance() != null) totalDistance += props.getDistance();
                if (props.getTime() != null) totalTime += props.getTime();

                if (props.getDescription() != null && !props.getDescription().isBlank()) {
                    instructions.add(props.getDescription());
                }
            }
        }

        if (path.isEmpty()) {
            return RouteResponse.unavailable();
        }

        return new RouteResponse(
                path,
                totalDistance,
                totalTime,
                List.of(),
                true
        );
    }
}
