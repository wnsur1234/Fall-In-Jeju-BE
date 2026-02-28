package com.jeju.ormicamp.model.dto.map;

public record PointDto(
        double lat,
        double lng,
        String name
) {
    public PointDto(double lat, double lng) {
        this(lat, lng, null);
    }
}