package com.jeju.ormicamp.model.dto.dynamodb;

import com.jeju.ormicamp.model.domain.ChatEntity;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PlanDayResDto {
    private String date;
    private String content;
    private String timestamp;
    private java.util.List<Double> lat;  // 위도 배열
    private java.util.List<Double> lng;  // 경도 배열

    public static PlanDayResDto from(ChatEntity entity) {
        return PlanDayResDto.builder()
                .date(entity.getPlanDate())
                .content(entity.getPrompt())
                .timestamp(entity.getTimestamp())
                .lat(entity.getLat())
                .lng(entity.getLng())
                .build();
    }

    public static List<PlanDayResDto> fromList(List<ChatEntity> entities) {
        return entities.stream()
                .map(PlanDayResDto::from)
                .toList();
    }
}



