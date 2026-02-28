package com.jeju.ormicamp.service.Bedrock;

import com.jeju.ormicamp.common.config.bedrock.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class BedRockAgentService {

    private final AwsProperties awsProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    public CompletableFuture<String> sendDataToAgent(String payloadJson) {
        CompletableFuture<String> resultFuture = new CompletableFuture<>();

        try {
            String url = awsProperties.getAgentApiGatewayUrl(); // 예: http://localhost:9001/api/v1/invoke

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // ✅ 핵심: 랩핑 제거, payloadJson을 그대로 body로 전송
            HttpEntity<String> request = new HttpEntity<>(payloadJson, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            resultFuture.complete(response.getBody());
        } catch (Exception e) {
            log.error("Agent 호출 실패: {}", e.getMessage(), e);
            resultFuture.completeExceptionally(e);
        }

        return resultFuture;
    }
}
