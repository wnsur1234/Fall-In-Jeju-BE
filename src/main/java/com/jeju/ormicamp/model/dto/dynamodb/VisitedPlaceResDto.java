package com.jeju.ormicamp.model.dto.dynamodb;

import lombok.Builder;
import lombok.Getter;

import java.util.Objects;

@Getter
@Builder
public class VisitedPlaceResDto {
    private String placeName;
    private Double lat;
    private Double lng;

    // Set에서 중복 제거를 위한 equals, hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VisitedPlaceResDto that = (VisitedPlaceResDto) o;
        return Objects.equals(placeName, that.placeName) &&
               Objects.equals(lat, that.lat) &&
               Objects.equals(lng, that.lng);
    }

    @Override
    public int hashCode() {
        return Objects.hash(placeName, lat, lng);
    }
}

