package com.jeju.ormicamp.model.dto.dynamodb;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatResDto {
    private String conversationId;
    private String message;  // content 필드 (CHAT일 때만 있음)
    private String summary;
    private String title;  // Agent가 생성한 제목 (PLAN일 때만 있음)
    private String type;  // "CHAT" 또는 "PLAN"
}
