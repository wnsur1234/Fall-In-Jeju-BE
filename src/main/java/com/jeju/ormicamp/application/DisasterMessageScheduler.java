package com.jeju.ormicamp.application;

import com.jeju.ormicamp.infrastructure.service.SseService;
import com.jeju.ormicamp.service.disasterMessage.DisasterMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DisasterMessageScheduler {

    private final DisasterMessageService disasterMessageService;

    /*
    * 1분 후 재실행
     */
    @Scheduled(fixedDelay = 300_000)
    public void fetchDisasterMessages() {
        System.out.println("스케줄러 실행!");
        disasterMessageService.fetchAndSaveDisasterMessages();
    }

    private final SseService sseService;

    @Scheduled(fixedRate = 30_000) // 15초 권장
    public void heartbeat() {
        System.out.println("SSE Ping 호출");
        sseService.sendPing();
    }
}
