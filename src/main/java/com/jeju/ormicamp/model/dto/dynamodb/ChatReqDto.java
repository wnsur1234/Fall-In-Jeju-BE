package com.jeju.ormicamp.model.dto.dynamodb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // 요청 JSON에 추가 필드가 와도 무시
public class ChatReqDto {
    private String content;
}