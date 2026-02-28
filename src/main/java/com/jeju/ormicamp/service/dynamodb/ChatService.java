package com.jeju.ormicamp.service.dynamodb;

import com.jeju.ormicamp.common.exception.CustomException;
import com.jeju.ormicamp.common.exception.ErrorCode;
import com.jeju.ormicamp.infrastructure.repository.dynamoDB.ChatDynamoRepository;
import com.jeju.ormicamp.infrastructure.repository.planner.TravelInfoRepository;
import com.jeju.ormicamp.model.code.ChatRole;
import com.jeju.ormicamp.model.code.ChatType;
import com.jeju.ormicamp.model.code.TravelInfoSnapshot;
import com.jeju.ormicamp.model.domain.ChatEntity;
import com.jeju.ormicamp.model.domain.TravelInfo;
import com.jeju.ormicamp.model.dto.dynamodb.ChatConversationResDto;
import com.jeju.ormicamp.model.dto.dynamodb.ChatMessageResDto;
import com.jeju.ormicamp.model.dto.dynamodb.ChatReqDto;
import com.jeju.ormicamp.model.dto.dynamodb.ChatResDto;
import com.jeju.ormicamp.model.dto.dynamodb.PlanDayResDto;
import com.jeju.ormicamp.model.dto.dynamodb.MyPagePlanResDto;
import com.jeju.ormicamp.model.dto.dynamodb.VisitedPlaceResDto;
import com.jeju.ormicamp.service.Bedrock.BedRockAgentService;
import com.jeju.ormicamp.service.Bedrock.MakeJsonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.time.LocalTime.now;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final TravelInfoRepository travelInfoRepository;
    private final ChatDynamoRepository chatRepository;
    private final BedRockAgentService agentService;
    private final MakeJsonService makeJsonService;
    private final ObjectMapper objectMapper;

    public ChatResDto startChat(ChatReqDto req, Long userId) {

        String conversationId = UUID.randomUUID().toString();

        // 지금 이건 애초에 meta에 저장하기 위해서 한번 필요함\
        // 왜냐 userid기준으로 조회한 값이기 때문
        TravelInfo travelInfo = travelInfoRepository
                .findByUserId(userId).orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        // 대화방 메타 데이터 초기 데이터 조회용
        // 초기 title은 임시값, 나중에 Agent가 생성한 title로 업데이트됨
        ChatEntity meta = ChatEntity.builder()
                .conversationId(conversationId)
                .userId(userId)  // 마이페이지 조회를 위해 userId 저장
                .type(ChatType.PLAN_META)
                .chatTitle("새 여행 계획")  // 임시 제목, Agent 응답으로 업데이트됨
                .travelInfo(TravelInfoSnapshot.toSnapshot(travelInfo))
                .build();

        // TODO : 예외처리
        chatRepository.save(meta);

        chatRepository.save(
                ChatEntity.builder()
                        .conversationId(conversationId)
                        .type(ChatType.CHAT)
                        .role(ChatRole.USER)
                        .prompt(req.getContent())
                        .timestamp(now().toString())
                        .build()
        );

        String payload = makeJsonService.createJsonPayload(
                conversationId,
                req.getContent(),
                meta.getTravelInfo()
        );

        // Agent API Gateway 호출
        String agentResponse = agentService.sendDataToAgent(payload).join();

        // 테스트용 더미 (Agent 연결 전까지 사용)
        // String agentResponse = """
        //         {
        //           "type": "PLAN",
        //           "title": "제주 남부 여행 일정",
        //           "summary": "RAG 원본 데이터와 Tmap 실시간 좌표를 결합한 정확한 일정입니다.",
        //           "plans": [
        //             {
        //               "date": "2025-12-04",
        //               "content": "서귀포시내 → 중문색달해변 → 천지연폭포 → 정방폭포 → 서귀포올레시장 → 서귀포숙소",
        //               "lat": [33.249664, 33.25143, 33.24, 33.24, 33.25, 33.25],
        //               "lng": [126.56034, 126.434824, 126.55, 126.57, 126.56, 126.56]
        //             }
        //           ]
        //         }
        //         """;

        // Agent 응답 파싱 (type에 따라 다르게 처리)
        String content = null;
        String summary = null;
        String title = null;
        String responseType = null;
        JsonNode plansArray = null;

        try {
            JsonNode node = objectMapper.readTree(agentResponse);
            // type이 없으면 null로 두고, Agent 응답 그대로 처리
            responseType = node.has("type") ? node.get("type").asText() : null;
            summary = node.has("summary") ? node.get("summary").asText() : null;

            if ("PLAN".equals(responseType)) {
                // PLAN 타입: title, summary, plans만 있음 (content 필드는 없지만, 원본 JSON 전체를 저장)
                title = node.has("title") ? node.get("title").asText() : null;
                if (node.has("plans") && node.get("plans").isArray()) {
                    plansArray = node.get("plans");
                }
                // PLAN일 때도 Agent 응답 전체(원본 JSON)를 content로 저장
                content = agentResponse;
            } else if ("CHAT".equals(responseType)) {
                // CHAT 타입: content, summary만 있음
                content = node.has("content") ? node.get("content").asText() : agentResponse;
            } else {
                // type이 없거나 다른 값일 때: 원본 그대로 저장
                content = agentResponse;
            }
        } catch (Exception e) {
            log.warn("Agent 응답 JSON 파싱 실패, 원본 저장: {}", e.getMessage());
            content = agentResponse;
            responseType = null;  // 파싱 실패 시 type도 null
        }

        // PLAN 타입이고 title이 있으면 META 업데이트
        if ("PLAN".equals(responseType) && title != null && !title.isEmpty()) {
            meta.setChatTitle(title);
            chatRepository.save(meta);
        }

        // AI 응답 저장
        // CHAT 타입: content 저장
        // PLAN 타입: summary 저장 (화면에서 summary를 보여주기 위해)
        if ("CHAT".equals(responseType) || responseType == null) {
            chatRepository.save(
                    ChatEntity.builder()
                            .conversationId(conversationId)
                            .type(ChatType.CHAT)
                            .role(ChatRole.AI)
                            .prompt(content)
                            .summary(summary)
                            .timestamp(now().toString())
                            .build()
            );
        } else if ("PLAN".equals(responseType)) {
            // PLAN 타입일 때도 ChatType.CHAT 엔티티를 저장하되, summary를 저장
            // 화면에서 PLAN 타입이면 summary를 보여주고, 클릭하면 plans를 보여주기 위해
            chatRepository.save(
                    ChatEntity.builder()
                            .conversationId(conversationId)
                            .type(ChatType.CHAT)
                            .role(ChatRole.AI)
                            .prompt(null)  // PLAN 타입일 때는 content 없음
                            .summary(summary)  // summary 저장
                            .timestamp(now().toString())
                            .build()
            );
        }

        // PLAN 타입일 때 날짜별 플래너 저장 (PLAN_DAY 타입)
        if ("PLAN".equals(responseType) && plansArray != null && plansArray.isArray()) {
            for (JsonNode planNode : plansArray) {
                if (planNode.has("date") && planNode.has("content")) {
                    String planDate = planNode.get("date").asText();
                    String planContent = planNode.get("content").asText();
                    
                    // lat, lng 배열 파싱
                    java.util.List<Double> latList = null;
                    java.util.List<Double> lngList = null;
                    if (planNode.has("lat") && planNode.get("lat").isArray()) {
                        latList = new java.util.ArrayList<>();
                        for (JsonNode latNode : planNode.get("lat")) {
                            latList.add(latNode.asDouble());
                        }
                    }
                    if (planNode.has("lng") && planNode.get("lng").isArray()) {
                        lngList = new java.util.ArrayList<>();
                        for (JsonNode lngNode : planNode.get("lng")) {
                            lngList.add(lngNode.asDouble());
                        }
                    }

                    chatRepository.save(
                            ChatEntity.builder()
                                    .conversationId(conversationId)
                                    .type(ChatType.PLAN_DAY)
                                    .role(ChatRole.AI)
                                    .prompt(planContent)
                                    .planDate(planDate)
                                    .lat(latList)
                                    .lng(lngList)
                                    .timestamp(now().toString())
                                    .build()
                    );
                }
            }
        }

        return ChatResDto.builder()
                .conversationId(conversationId)
                .message(content)
                .summary(summary)
                .title(title)
                .type(responseType)
                .build();
    }

    public ChatResDto sendMessage(String conversationId, String content, Long userId) {

        ChatEntity meta = chatRepository.findMeta(conversationId);

        chatRepository.save(
                ChatEntity.builder()
                        .conversationId(conversationId)
                        .type(ChatType.CHAT)
                        .role(ChatRole.USER)
                        .prompt(content)
                        .timestamp(now().toString())
                        .build()
        );

        String payload = makeJsonService.createJsonPayload(
                conversationId,
                content,
                meta.getTravelInfo()
        );

        // Agent API Gateway 호출
        String agentResponse = agentService.sendDataToAgent(payload).join();

        // 테스트용 더미 (Agent 연결 전까지 사용)
        // String agentResponse = """
        //         {
        //           "type": "PLAN",
        //           "title": "제주 남부 여행 일정",
        //           "summary": "RAG 원본 데이터와 Tmap 실시간 좌표를 결합한 정확한 일정입니다.",
        //           "plans": [
        //             {
        //               "date": "2025-12-04",
        //               "content": "서귀포시내 → 중문색달해변 → 천지연폭포 → 정방폭포 → 서귀포올레시장 → 서귀포숙소",
        //               "lat": [33.249664, 33.25143, 33.24, 33.24, 33.25, 33.25],
        //               "lng": [126.56034, 126.434824, 126.55, 126.57, 126.56, 126.56]
        //             }
        //           ]
        //         }
        //         """;

        // Agent 응답 파싱 (type에 따라 다르게 처리)
        String msgContent = null;
        String summary = null;
        String title = null;
        String responseType = null;
        JsonNode plansArray = null;

        try {
            JsonNode node = objectMapper.readTree(agentResponse);
            // type이 없으면 null로 두고, Agent 응답 그대로 처리
            responseType = node.has("type") ? node.get("type").asText() : null;
            summary = node.has("summary") ? node.get("summary").asText() : null;

            if ("PLAN".equals(responseType)) {
                // PLAN 타입: title, summary, plans만 있음 (content 필드는 없지만, 원본 JSON 전체를 저장)
                title = node.has("title") ? node.get("title").asText() : null;
                if (node.has("plans") && node.get("plans").isArray()) {
                    plansArray = node.get("plans");
                }
                // PLAN일 때도 Agent 응답 전체(원본 JSON)를 content로 저장
                msgContent = agentResponse;
            } else if ("CHAT".equals(responseType)) {
                // CHAT 타입: content, summary만 있음
                msgContent = node.has("content") ? node.get("content").asText() : agentResponse;
            } else {
                // type이 없거나 다른 값일 때: 원본 그대로 저장
                msgContent = agentResponse;
            }
        } catch (Exception e) {
            log.warn("Agent 응답 JSON 파싱 실패, 원본 저장: {}", e.getMessage());
            msgContent = agentResponse;
            responseType = null;  // 파싱 실패 시 type도 null
        }

        // PLAN 타입이고 title이 있으면 META 업데이트 (title이 있을 때만)
        if ("PLAN".equals(responseType) && title != null && !title.isEmpty()) {
            meta.setChatTitle(title);
            chatRepository.save(meta);
        }

        // AI 응답 저장
        // CHAT 타입: content 저장
        // PLAN 타입: summary 저장 (화면에서 summary를 보여주기 위해)
        if ("CHAT".equals(responseType) || responseType == null) {
            chatRepository.save(
                    ChatEntity.builder()
                            .conversationId(conversationId)
                            .type(ChatType.CHAT)
                            .role(ChatRole.AI)
                            .prompt(msgContent)
                            .summary(summary)
                            .timestamp(now().toString())
                            .build()
            );
        } else if ("PLAN".equals(responseType)) {
            // PLAN 타입일 때도 ChatType.CHAT 엔티티를 저장하되, summary를 저장
            // 화면에서 PLAN 타입이면 summary를 보여주고, 클릭하면 plans를 보여주기 위해
            chatRepository.save(
                    ChatEntity.builder()
                            .conversationId(conversationId)
                            .type(ChatType.CHAT)
                            .role(ChatRole.AI)
                            .prompt(null)  // PLAN 타입일 때는 content 없음
                            .summary(summary)  // summary 저장
                            .timestamp(now().toString())
                            .build()
            );
        }

        // PLAN 타입일 때 날짜별 플래너 저장 (PLAN_DAY 타입)
        if ("PLAN".equals(responseType) && plansArray != null && plansArray.isArray()) {
            for (JsonNode planNode : plansArray) {
                if (planNode.has("date") && planNode.has("content")) {
                    String planDate = planNode.get("date").asText();
                    String planContent = planNode.get("content").asText();
                    
                    // lat, lng 배열 파싱
                    java.util.List<Double> latList = null;
                    java.util.List<Double> lngList = null;
                    if (planNode.has("lat") && planNode.get("lat").isArray()) {
                        latList = new java.util.ArrayList<>();
                        for (JsonNode latNode : planNode.get("lat")) {
                            latList.add(latNode.asDouble());
                        }
                    }
                    if (planNode.has("lng") && planNode.get("lng").isArray()) {
                        lngList = new java.util.ArrayList<>();
                        for (JsonNode lngNode : planNode.get("lng")) {
                            lngList.add(lngNode.asDouble());
                        }
                    }

                    chatRepository.save(
                            ChatEntity.builder()
                                    .conversationId(conversationId)
                                    .type(ChatType.PLAN_DAY)
                                    .role(ChatRole.AI)
                                    .prompt(planContent)
                                    .planDate(planDate)
                                    .lat(latList)
                                    .lng(lngList)
                                    .timestamp(now().toString())
                                    .build()
                    );
                }
            }
        }

        return ChatResDto.builder()
                .conversationId(conversationId)
                .message(msgContent)
                .summary(summary)
                .title(title)
                .type(responseType)
                .build();
    }

    // 채팅방 정보 반환
    public ChatConversationResDto getConversation(String conversationId) {

        List<ChatEntity> items =
                chatRepository.findByConversationId(conversationId);

        if (items.isEmpty()) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }

        ChatEntity meta = items.stream()
                .filter(i -> i.getType() == ChatType.PLAN_META)
                .findFirst()
                .orElseThrow();

        List<ChatMessageResDto> messages = items.stream()
                .filter(i -> i.getType() == ChatType.CHAT)
                .sorted(Comparator.comparing(ChatEntity::getTimestamp))
                .map(ChatMessageResDto::from)
                .toList();

        return ChatConversationResDto.builder()
                .conversationId(conversationId)
                .chatTitle(meta.getChatTitle())
                .travelInfo(meta.getTravelInfo())
                .messages(messages)
                .build();
    }

    /**
     * 날짜별 플래너 조회
     *
     * @param conversationId 대화 ID
     * @param date           날짜 (YYYY-MM-DD)
     * @return 해당 날짜의 플래너 목록
     */
    public List<PlanDayResDto> getPlansByDate(String conversationId, String date) {
        List<ChatEntity> plans = chatRepository.findPlansByDate(conversationId, date);
        return PlanDayResDto.fromList(plans);
    }

    /**
     * 마이페이지 날짜 검색: 특정 날짜에 해당하는 여행 계획 조회
     * 
     * @param userId 사용자 ID
     * @param date 검색할 날짜 (YYYY-MM-DD)
     * @return 해당 날짜가 포함된 여행 계획 목록
     */
    public List<MyPagePlanResDto> getPlansByDateForMyPage(Long userId, String date) {
        // 1. TravelInfo에서 해당 날짜가 포함된 여행 정보 찾기
        TravelInfo travelInfo = travelInfoRepository
                .findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        
        LocalDate searchDate = LocalDate.parse(date, DateTimeFormatter.ISO_DATE);
        
        // 2. 해당 날짜가 여행 기간에 포함되는지 확인
        if (travelInfo.getStartDate() == null || travelInfo.getEndDate() == null ||
            searchDate.isBefore(travelInfo.getStartDate()) || 
            searchDate.isAfter(travelInfo.getEndDate())) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
        
        // 3. userId로 모든 META 조회 (해당 사용자의 모든 대화방)
        List<ChatEntity> allMetas = chatRepository.findAllMetaByUserId(userId);
        
        // 4. 각 conversationId에 대해 해당 날짜의 플래너 조회
        return allMetas.stream()
                .map(meta -> {
                    String conversationId = meta.getConversationId();
                    List<ChatEntity> plans = chatRepository.findPlansByDate(conversationId, date);
                    
                    if (!plans.isEmpty()) {
                        return MyPagePlanResDto.builder()
                                .conversationId(conversationId)
                                .title(meta.getChatTitle())
                                .summary(null)  // META에 summary가 없으면 null
                                .plans(PlanDayResDto.fromList(plans))
                                .build();
                    }
                    return null;
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    /**
     * 사용자 방문 장소 목록 조회
     * 모든 플래너에서 방문한 장소를 모아서 반환
     * 
     * @param userId 사용자 ID
     * @return 방문한 장소 목록 (중복 제거)
     */
    public List<VisitedPlaceResDto> getVisitedPlaces(Long userId) {
        // 1. userId로 모든 META 조회
        List<ChatEntity> allMetas = chatRepository.findAllMetaByUserId(userId);
        
        // 2. 각 conversationId의 모든 PLAN_DAY 조회
        Set<VisitedPlaceResDto> placesSet = new HashSet<>();
        
        for (ChatEntity meta : allMetas) {
            String conversationId = meta.getConversationId();
            List<ChatEntity> allPlans = chatRepository.findByConversationId(conversationId)
                    .stream()
                    .filter(item -> item.getType() == ChatType.PLAN_DAY)
                    .toList();
            
            for (ChatEntity plan : allPlans) {
                String content = plan.getPrompt(); // "서귀포시내 → 중문색달해변 → ..."
                List<Double> latList = plan.getLat();
                List<Double> lngList = plan.getLng();
                
                if (content != null && latList != null && lngList != null) {
                    // content 파싱: "→" 기준으로 split
                    String[] placeNames = content.split("→");
                    
                    for (int i = 0; i < placeNames.length && i < latList.size() && i < lngList.size(); i++) {
                        String placeName = placeNames[i].trim();
                        if (!placeName.isEmpty()) {
                            placesSet.add(VisitedPlaceResDto.builder()
                                    .placeName(placeName)
                                    .lat(latList.get(i))
                                    .lng(lngList.get(i))
                                    .build());
                        }
                    }
                }
            }
        }
        
        return new ArrayList<>(placesSet);
    }

}