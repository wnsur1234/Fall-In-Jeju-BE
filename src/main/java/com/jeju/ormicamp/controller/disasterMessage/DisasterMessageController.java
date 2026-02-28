package com.jeju.ormicamp.controller.disasterMessage;

import com.jeju.ormicamp.common.customUserDetail.UserPrincipal;
import com.jeju.ormicamp.model.dto.disasterMessage.DisasterMessageResponse;
import com.jeju.ormicamp.service.disasterMessage.DisasterMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/disasters")
public class DisasterMessageController {

    private final DisasterMessageService disasterMessageService;

    @GetMapping
    public ResponseEntity<List<DisasterMessageResponse>> getMessages(
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(disasterMessageService.getDisasterMessages());
    }
}
