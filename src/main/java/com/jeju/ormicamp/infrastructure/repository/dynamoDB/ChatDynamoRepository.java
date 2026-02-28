package com.jeju.ormicamp.infrastructure.repository.dynamoDB;

import com.jeju.ormicamp.common.config.bedrock.AwsProperties;
import com.jeju.ormicamp.common.exception.CustomException;
import com.jeju.ormicamp.common.exception.ErrorCode;
import com.jeju.ormicamp.model.domain.ChatEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;

import java.util.List;


@Repository
@RequiredArgsConstructor
public class ChatDynamoRepository {

    private final DynamoDbEnhancedClient enhancedClient;
    private final AwsProperties awsProperties;


    private DynamoDbTable<ChatEntity> table() {
        return enhancedClient.table(
                awsProperties.getDynamoTableName(),
                TableSchema.fromBean(ChatEntity.class)
        );
    }

    /**
     * 1. 저장 (META / CHAT 공용)
     */
    public void save(ChatEntity entity) {
        table().putItem(entity);
    }

    /**
     * 2. META 조회
     * Service 코드에서 ChatEntity meta = chatRepository.findMeta(conversationId);
     */
    public ChatEntity findMeta(String conversationId) {

        Key key = Key.builder()
                .partitionValue("SESSION#" + conversationId)
                .sortValue("META")
                .build();

        ChatEntity meta = table().getItem(r -> r.key(key));

        if (meta == null) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }

        return meta;
    }

    /**
     * convesationId 기준 채팅방 조회
     * @param conversationId PK값
     * @return 해당 items 전부
     */
    public List<ChatEntity> findByConversationId(String conversationId) {
        QueryConditional condition = QueryConditional.keyEqualTo(
                Key.builder()
                        .partitionValue("SESSION#" + conversationId)
                        .build()
        );

        return table()
                .query(condition)
                .items()
                .stream()
                .toList();
    }

    /**
     * conversationId와 날짜로 날짜별 플래너 조회
     * @param conversationId 대화 ID
     * @param date 날짜 (YYYY-MM-DD)
     * @return 해당 날짜의 플래너 목록
     */
    public List<ChatEntity> findPlansByDate(String conversationId, String date) {
        List<ChatEntity> allItems = findByConversationId(conversationId);
        return allItems.stream()
                .filter(item -> item.getType() == com.jeju.ormicamp.model.code.ChatType.PLAN_DAY
                        && date.equals(item.getPlanDate()))
                .toList();
    }

    /**
     * userId로 모든 META 조회 (마이페이지용)
     * 주의: DynamoDB 스캔 사용 (비효율적, 나중에 GSI 추가 고려)
     * @param userId 사용자 ID
     * @return 해당 userId의 모든 META 목록
     */
    public List<ChatEntity> findAllMetaByUserId(Long userId) {
        return table()
                .scan(ScanEnhancedRequest.builder().build())
                .items()
                .stream()
                .filter(item -> item.getType() == com.jeju.ormicamp.model.code.ChatType.PLAN_META
                        && userId.equals(item.getUserId()))
                .toList();
    }
}