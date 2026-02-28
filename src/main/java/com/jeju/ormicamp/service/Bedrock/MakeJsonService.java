package com.jeju.ormicamp.service.Bedrock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeju.ormicamp.model.code.TravelInfoSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MakeJsonService {

    private final ObjectMapper objectMapper;

    public String createJsonPayload(
            String conversationId,
            String userMessage,
            TravelInfoSnapshot info
    ) {
        try {
            // theme을 한글로 변환
            java.util.List<String> themeList = info.getThemes() != null ? info.getThemes().stream()
                    .map(theme -> {
                        // Theme Enum을 한글 이름으로 변환
                        return switch (theme) {
                            case NATURE -> "자연";
                            case HEALING -> "힐링";
                            case CULTURE -> "문화";
                            case FOOD -> "맛집";
                            case ACTIVITY -> "액티비티";
                            case PHOTO -> "사진";
                            case FAMILY -> "가족";
                            case COUPLE -> "커플";
                            case FRIEND -> "친구";
                        };
                    })
                    .toList() : java.util.List.of();
            
            // travelInfo 객체 생성 (language, budget, people, startDate, endDate 포함, theme 제외)
            Map<String, Object> travelInfoMap = new HashMap<>();
            travelInfoMap.put("language", info.getLanguage() != null ? info.getLanguage().name().toLowerCase() : "ko");
            travelInfoMap.put("budget", info.getMoney() != null ? info.getMoney() : 0);
            travelInfoMap.put("people", info.getCapacity() != null ? info.getCapacity() : 1);
            travelInfoMap.put("startDate", info.getStartDate() != null ? info.getStartDate().toString() : "");
            travelInfoMap.put("endDate", info.getEndDate() != null ? info.getEndDate().toString() : "");
            
            // 최종 payload 생성 (theme은 최상위 레벨로 분리)
            Map<String, Object> payload = new HashMap<>();
            payload.put("conversationId", conversationId);
            payload.put("theme", themeList);
            payload.put("content", userMessage);
            payload.put("travelInfo", travelInfoMap);
            
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 직렬화 실패", e);
        }
    }

}
