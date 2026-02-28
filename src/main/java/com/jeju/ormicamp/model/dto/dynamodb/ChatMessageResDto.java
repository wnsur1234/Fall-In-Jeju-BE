package com.jeju.ormicamp.model.dto.dynamodb;

import com.jeju.ormicamp.model.code.ChatRole;
import com.jeju.ormicamp.model.domain.ChatEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageResDto {

    private ChatRole role;
    private String content;  // CHAT 타입일 때 사용
    private String summary;  // PLAN 타입일 때 사용
    private String timestamp;

    public static ChatMessageResDto from(ChatEntity entity) {
        return ChatMessageResDto.builder()
                .role(entity.getRole())
                .content(entity.getPrompt())  // CHAT 타입일 때 content
                .summary(entity.getSummary())  // PLAN 타입일 때 summary
                .timestamp(entity.getTimestamp())
                .build();
    }
}