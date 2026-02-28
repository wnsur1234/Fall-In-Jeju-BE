package com.jeju.ormicamp.model.dto.map.car;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmapCarRawResponse {

    private List<Feature> features;

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Feature {
        private Geometry geometry;
        private Properties properties;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Geometry {
        private String type;

        /**
         * LineString  -> List<List<Double>>
         * Point       -> List<Double>
         * Edge case   -> Double
         */
        private Object coordinates;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Properties {
        private Integer distance;
        private Integer time;
        private String description;
    }
}
