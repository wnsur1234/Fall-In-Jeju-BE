package com.jeju.ormicamp.model.dto.map.walk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@NoArgsConstructor
public class TmapWalkRawResponse {

    private String type;
    private List<Feature> features;

    @Getter
    @NoArgsConstructor
    public static class Feature {
        private String type;
        private Geometry geometry;
        private Properties properties;
    }

    @Getter
    @NoArgsConstructor
    public static class Geometry {
        private String type;
        private Object coordinates;

        public String getType() {
            return type;
        }

        public Object getCoordinates() {
            return coordinates;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class Properties {

        private Integer totalDistance;
        private Integer totalTime;

        private Integer distance;
        private Integer time;

        private String description;
        private String name;
        private Integer index;
    }
}