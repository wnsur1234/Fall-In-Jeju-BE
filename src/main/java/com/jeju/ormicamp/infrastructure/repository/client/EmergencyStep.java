package com.jeju.ormicamp.infrastructure.repository.client;

import java.util.Arrays;

public enum EmergencyStep {

    EMERGENCY_DISASTER("긴급재난"),
    URGENT_DISASTER("위급재난"),
    SAFETY_NOTICE("안전안내"),

    UNKNOWN("UNKNOWN");

    private final String apiValue;

    EmergencyStep(String apiValue) {
        this.apiValue = apiValue;
    }

    public static EmergencyStep from(String apiValue) {
        if (apiValue == null) {
            return UNKNOWN;
        }

        return Arrays.stream(values())
                .filter(step -> step.apiValue.equals(apiValue))
                .findFirst()
                .orElse(UNKNOWN);
    }
}
