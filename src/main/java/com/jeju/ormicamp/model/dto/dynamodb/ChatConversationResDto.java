package com.jeju.ormicamp.model.dto.dynamodb;

import com.jeju.ormicamp.model.code.TravelInfoSnapshot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatConversationResDto {

    private String conversationId;
    private String chatTitle;
    private TravelInfoSnapshot travelInfo;
    private List<ChatMessageResDto> messages;
}