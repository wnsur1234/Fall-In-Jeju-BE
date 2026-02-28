package com.jeju.ormicamp.infrastructure.repository.client;

import java.util.Arrays;

public enum DisasterType {

    AI("AI"),
    DROUGHT("가뭄"),
    LIVESTOCK_DISEASE("가축질병"),
    STRONG_WIND("강풍"),
    DRY("건조"),
    TRAFFIC("교통"),
    TRAFFIC_ACCIDENT("교통사고"),
    TRAFFIC_CONTROL("교통통제"),
    FINANCE("금융"),
    SNOW("대설"),
    FINE_DUST("미세먼지"),
    CIVIL_DEFENSE("민방공"),
    COLLAPSE("붕괴"),
    FOREST_FIRE("산불"),
    LANDSLIDE("산사태"),
    WATER("수도"),
    FOG("안개"),
    ENERGY("에너지"),
    POWER_OUTAGE("정전"),
    EARTHQUAKE("지진"),
    TSUNAMI("지진해일"),
    TYPHOON("태풍"),
    TERROR("테러"),
    COMMUNICATION("통신"),
    EXPLOSION("폭발"),
    HEAT_WAVE("폭염"),
    STORM_SURGE("풍랑"),
    COLD_WAVE("한파"),
    HEAVY_RAIN("호우"),
    FLOOD("홍수"),
    FIRE("화재"),
    ENVIRONMENT_ACCIDENT("환경오염사고"),
    YELLOW_DUST("황사"),
    ETC("기타"),

    UNKNOWN("UNKNOWN");

    private final String apiValue;

    DisasterType(String apiValue) {
        this.apiValue = apiValue;
    }

    public static DisasterType from(String apiValue) {
        if (apiValue == null) {
            return UNKNOWN;
        }

        return Arrays.stream(values())
                .filter(t -> t.apiValue.equals(apiValue))
                .findFirst()
                .orElse(UNKNOWN);
    }
}
