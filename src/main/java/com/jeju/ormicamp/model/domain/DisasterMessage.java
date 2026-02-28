package com.jeju.ormicamp.model.domain;

import com.jeju.ormicamp.infrastructure.repository.client.ApiDisasterMessage;
import com.jeju.ormicamp.infrastructure.repository.client.DisasterType;
import com.jeju.ormicamp.infrastructure.repository.client.EmergencyStep;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "disaster_message",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_disaster_message_external_id", columnNames = "external_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Getter
public class DisasterMessage {

    /**
     * 내부 DB PK
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 재난 데이터 일련 코드
     * API 필드: SN
     * 재난문자 고유 식별자 (중복 방지 기준)
     */
    @Column(name = "external_id", nullable = false, length = 22)
    private String externalId;

    /**
     * 재난 발령 지역명
     * API 필드: RCPTN_RGN_NM
     * 예: "제주시", "서귀포시", "제주특별자치도"
     */
    @Column(name = "region_name", nullable = false, length = 255)
    private String regionName;

    /**
     * 재해 구분
     * API 필드: DST_SE_NM
     * 예: 태풍, 호우, 지진, 폭염, 산불 등
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "disaster_type", nullable = false, length = 100)
    private DisasterType disasterType;

    /**
     * 긴급 단계 구분
     * API 필드: EMRG_STEP_NM
     * 예: 긴급재난, 안전안내, 위급재난
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "emergency_step", nullable = false, length = 100)
    private EmergencyStep emergencyStep;

    /**
     * 재난 발령 내용
     * API 필드: MSG_CN
     */
    @Column(name = "content", nullable = false, length = 4000)
    private String content;

    /**
     * 재난 발령 시각
     * API 필드: CRT_DT
     * 외부 시스템 기준 발송/생성 시각
     */
    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    /**
     * 우리 서비스 DB에 저장된 시각
     * (엔티티 생성 시점)
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;


    public static DisasterMessage from(ApiDisasterMessage api) {
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

        return DisasterMessage.builder()
                .externalId(api.getSn())
                .regionName(api.getRegionName())
                .issuedAt(LocalDateTime.parse(api.getCreatedAt(), formatter))
                .content(api.getMessage())
                .disasterType(DisasterType.from(api.getDisasterType())) //
                .emergencyStep(EmergencyStep.from(api.getEmergencyStep()))
                .createdAt(LocalDateTime.now())
                .build();
    }

}
