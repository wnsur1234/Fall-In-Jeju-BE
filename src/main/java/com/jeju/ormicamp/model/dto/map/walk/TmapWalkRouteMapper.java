package com.jeju.ormicamp.model.dto.map.walk;

import com.jeju.ormicamp.model.dto.map.PointDto;
import com.jeju.ormicamp.model.dto.map.car.SimpleRouteResponse;

import java.util.ArrayList;
import java.util.List;

public class TmapWalkRouteMapper {

    private TmapWalkRouteMapper() {}

    @SuppressWarnings("unchecked")
    public static SimpleRouteResponse toRouteResponse(TmapWalkRawResponse raw) {

        if (raw == null || raw.getFeatures() == null) {
            return SimpleRouteResponse.unavailable();
        }

        List<PointDto> path = new ArrayList<>();
        List<String> instructions = new ArrayList<>();

        int distance = 0;
        int duration = 0;

        for (var f : raw.getFeatures()) {

            /* ===== geometry (LineString만 path로) ===== */
            var geometry = f.getGeometry();
            if (geometry != null
                    && "LineString".equals(geometry.getType())
                    && geometry.getCoordinates() instanceof List<?> list) {

                for (Object o : list) {
                    if (!(o instanceof List<?> c) || c.size() < 2) continue;

                    double lng = ((Number) c.get(0)).doubleValue();
                    double lat = ((Number) c.get(1)).doubleValue();
                    path.add(new PointDto(lat, lng));
                }
            }

            /* ===== properties ===== */
            var props = f.getProperties();
            if (props == null) continue;

            // 총 거리 / 시간 (Point에만 있음)
            if (props.getTotalDistance() != null)
                distance = props.getTotalDistance();

            if (props.getTotalTime() != null)
                duration = props.getTotalTime();

            // 안내 문구는 Point에서만
            if (geometry != null
                    && "Point".equals(geometry.getType())
                    && props.getDescription() != null
                    && !props.getDescription().isBlank()) {

                instructions.add(props.getDescription());
            }
        }

        if (distance == 0 && duration == 0) {
            return SimpleRouteResponse.unavailable();
        }

        return new SimpleRouteResponse(
                path,
                distance,
                duration,
                instructions,
                true
        );
    }
}
