package com.jeju.ormicamp.controller.sse;

import com.jeju.ormicamp.infrastructure.service.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sse")
public class SseController {

    private final SseService sseService;

    @GetMapping("/disasters")
    public SseEmitter connect() {
        System.out.println("sse 연결");
        Long userId = 1L;
        return sseService.connect(userId);
    }
}
