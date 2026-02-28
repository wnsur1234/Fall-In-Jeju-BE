package com.jeju.ormicamp.model.dto.place;

import com.jeju.ormicamp.model.domain.Place;
import com.jeju.ormicamp.model.s3.PlaceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class PlaceResDto {
    private final String name;
    private final PlaceType type;
    private final Double lat;
    private final Double lon;
    private final String roadAddress;
    private final String imageUrl;

    public static PlaceResDto from(Place place) {
        return PlaceResDto.builder()
                .name(place.getName())
                .type(place.getType())
                .lat(place.getLat())
                .lon(place.getLon())
                .roadAddress(place.getRoadAddress())
                .imageUrl(place.getImageUrl())
                .build();
    }
}
