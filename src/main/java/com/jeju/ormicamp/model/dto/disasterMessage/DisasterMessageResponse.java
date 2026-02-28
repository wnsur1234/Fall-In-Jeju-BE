package com.jeju.ormicamp.model.dto.disasterMessage;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DisasterMessageResponse {
    private String externalId;
    private String region;
    private String emergencyStep;
    private String disasterType;
    private String message;
    private LocalDateTime issuedAt;
}
