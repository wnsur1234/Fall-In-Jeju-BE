package com.jeju.ormicamp.model.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "disaster_message_read")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Getter
public class DisasterMessageRead {

    @EmbeddedId
    private DisasterMessageReadId id;

    @MapsId("disasterMessageId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disaster_message_id", nullable = false)
    private DisasterMessage disasterMessage;

    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt;
}
