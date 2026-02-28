package com.jeju.ormicamp.model.s3;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TouristPlace {
    private final String name;
    private final double lat;
    private final double lon;
    private final String roadAddress;
    private final String imageUrl;
    private final PlaceType type;
}
