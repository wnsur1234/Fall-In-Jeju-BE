package com.jeju.ormicamp.model.dto.map.car;

import com.jeju.ormicamp.model.dto.map.PointDto;
import com.jeju.ormicamp.model.dto.map.RouteResponse;

import java.util.ArrayList;
import java.util.List;

public class TmapCarRouteMapper {

    private TmapCarRouteMapper() {}

    @SuppressWarnings("unchecked")
    public static SimpleRouteResponse toRouteResponse(TmapCarRawResponse raw) {

        if (raw == null || raw.getFeatures() == null) {
            return SimpleRouteResponse.unavailable();
        }

        List<PointDto> path = new ArrayList<>();
        List<String> instructions = new ArrayList<>();
        int distance = 0;
        int duration = 0;

        for (var f : raw.getFeatures()) {

            var geometry = f.getGeometry();
            if (geometry != null && geometry.getCoordinates() != null) {

                Object coords = geometry.getCoordinates();

                /* =========================
                   LineString
                   [[lng, lat], [lng, lat], ...]
                ========================= */
                if ("LineString".equals(geometry.getType())
                        && coords instanceof List<?> list) {

                    for (Object o : list) {
                        if (!(o instanceof List<?> c) || c.size() < 2) continue;

                        double lng = ((Number) c.get(0)).doubleValue();
                        double lat = ((Number) c.get(1)).doubleValue();
                        path.add(new PointDto(lat, lng));
                    }
                }

                /* =========================
                   Point
                   [lng, lat]
                ========================= */
                else if ("Point".equals(geometry.getType())
                        && coords instanceof List<?> c
                        && c.size() >= 2) {

                    double lng = ((Number) c.get(0)).doubleValue();
                    double lat = ((Number) c.get(1)).doubleValue();
                    path.add(new PointDto(lat, lng));
                }
            }

            var props = f.getProperties();
            if (props != null) {
                if (props.getDistance() != null)
                    distance += props.getDistance();
                if (props.getTime() != null)
                    duration += props.getTime();
                if (props.getDescription() != null
                        && !props.getDescription().isBlank())
                    instructions.add(props.getDescription());
            }
        }

        if (path.isEmpty()) return SimpleRouteResponse.unavailable();

        return new SimpleRouteResponse(path, distance, duration, instructions, true);
    }
}
