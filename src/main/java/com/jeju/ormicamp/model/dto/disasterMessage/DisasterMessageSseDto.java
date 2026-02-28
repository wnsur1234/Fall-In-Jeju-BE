package com.jeju.ormicamp.model.dto.disasterMessage;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jeju.ormicamp.model.domain.DisasterMessage;

import java.time.LocalDateTime;

public record DisasterMessageSseDto(
        String externalId,
        String region,
        String emergencyStep,
        String disasterType,
        String message,
        @JsonFormat(shape = JsonFormat.Shape.STRING,
                pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime issuedAt
) {
    public static DisasterMessageSseDto from(DisasterMessage m) {
        return new DisasterMessageSseDto(
                m.getExternalId(),
                m.getRegionName(),
                m.getEmergencyStep().name(),
                m.getDisasterType().name(),
                m.getContent(),
                m.getIssuedAt()
        );
    }
}
