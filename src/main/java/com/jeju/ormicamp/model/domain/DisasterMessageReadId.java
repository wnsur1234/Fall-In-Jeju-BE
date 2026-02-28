package com.jeju.ormicamp.model.domain;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class DisasterMessageReadId {
    private Long userId;
    private Long disasterMessageId;
}
