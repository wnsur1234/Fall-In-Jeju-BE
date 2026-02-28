package com.jeju.ormicamp.model.dto.map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmapRawRouteResponse {

    private List<Feature> features;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Feature {
        private Geometry geometry;
        private Properties properties;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Geometry {
        private String type;

        /**
         * ✅ 중요한 포인트
         * - Point: [lng, lat]
         * - LineString: [[lng, lat], [lng, lat], ...]
         * 둘 다 올 수 있어서 Object로 받아야 디코딩이 안 터짐
         */
        private Object coordinates;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Properties {
        private Integer distance;
        private Integer time;
        private String description;
    }
}
