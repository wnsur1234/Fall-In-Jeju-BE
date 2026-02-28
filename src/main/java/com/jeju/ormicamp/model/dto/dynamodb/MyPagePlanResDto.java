package com.jeju.ormicamp.model.dto.dynamodb;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MyPagePlanResDto {
    private String conversationId;
    private String title;
    private String summary;
    private List<PlanDayResDto> plans;  // 해당 날짜의 플래너들
}


