package com.jeju.ormicamp.model.domain;

import com.jeju.ormicamp.model.code.ChatRole;
import com.jeju.ormicamp.model.code.ChatType;
import com.jeju.ormicamp.model.code.TravelInfoSnapshot;
import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamoDbBean
public class ChatEntity {

    private String pk; // 검색 추적 conversationId
    private String sk;

    // user Id 랑 conversationId 시작일 유저iㅇ

    private String conversationId;
    private String timestamp;

    private Long userId;

    private ChatType type; // Meta,chat, plan
    private ChatRole role;

    private String prompt; // 일반응답 -> 만약 플래너를 짜주는 내용이면

    private String summary;  // AI 응답 요약

    private String chatTitle;

    private String planDate;

    private java.util.List<Double> lat;  // 위도 배열
    private java.util.List<Double> lng;  // 경도 배열

    // 마이페이지 조회 api -> type = plan, 조회
    private TravelInfoSnapshot travelInfo;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("PK") // 실제 DB 컬럼 이름
    public String getPk() {
        return "SESSION#" + conversationId;
    }

    @DynamoDbSortKey // 이게 SK라는 뜻
    @DynamoDbAttribute("SK") // 실제 DB 컬럼 이름
    public String getSk() {
        if (type == ChatType.PLAN_META) {
            return "META";
        }
        if (type == ChatType.PLAN_DAY && planDate != null) {
            return type.name() + "#" + planDate + "#" + timestamp;
        }
        return type.name() + "#" + timestamp;
    }
}