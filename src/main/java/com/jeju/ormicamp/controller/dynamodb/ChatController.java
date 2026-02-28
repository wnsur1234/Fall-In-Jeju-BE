package com.jeju.ormicamp.controller.dynamodb;

import com.jeju.ormicamp.common.customUserDetail.UserPrincipal;
import com.jeju.ormicamp.common.dto.BaseResponse;
import com.jeju.ormicamp.model.dto.dynamodb.ChatConversationResDto;
import com.jeju.ormicamp.model.dto.dynamodb.ChatReqDto;
import com.jeju.ormicamp.model.dto.dynamodb.ChatResDto;
import com.jeju.ormicamp.model.dto.dynamodb.PlanDayResDto;
import com.jeju.ormicamp.model.dto.dynamodb.MyPagePlanResDto;
import com.jeju.ormicamp.model.dto.dynamodb.VisitedPlaceResDto;
import com.jeju.ormicamp.service.dynamodb.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/sessions")
    public ResponseEntity<BaseResponse<ChatResDto>> startChat(
            @RequestBody ChatReqDto req,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        Long userId = user.userId();
        ChatResDto result = chatService.startChat(req,userId);
        return ResponseEntity.ok()
                .body(BaseResponse.success("대화 시작", result));
    }

    @PostMapping("/sessions/{conversationId}/messages")
    public ResponseEntity<BaseResponse<ChatResDto>> chatMessage(
            @PathVariable String conversationId,
            @RequestBody ChatReqDto req,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        Long userId = user.userId();
        String content = req.getContent();
        ChatResDto result = chatService.sendMessage(conversationId,content,userId);
        return ResponseEntity.ok()
                .body(BaseResponse.success("소통중..", result));
    }

    // 채팅방 조회
    @GetMapping("/sessions/{conversationId}")
    public ResponseEntity<BaseResponse<ChatConversationResDto>> getConversation(
            @PathVariable String conversationId
    ) {
        return ResponseEntity.ok(
                BaseResponse.success(
                        "채팅 조회 성공",
                        chatService.getConversation(conversationId)
                )
        );
    }

    // 날짜별 플래너 조회
    @GetMapping("/sessions/{conversationId}/plans/{date}")
    public ResponseEntity<BaseResponse<List<PlanDayResDto>>> getPlansByDate(
            @PathVariable String conversationId,
            @PathVariable String date
    ) {
        return ResponseEntity.ok(
                BaseResponse.success(
                        "날짜별 플래너 조회 성공",
                        chatService.getPlansByDate(conversationId, date)
                )
        );
    }

    // 마이페이지 날짜 검색
    @GetMapping("/mypage/plans/{date}")
    public ResponseEntity<BaseResponse<List<MyPagePlanResDto>>> getPlansByDateForMyPage(
            @PathVariable String date,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        Long userId = user.userId();
        return ResponseEntity.ok(
                BaseResponse.success(
                        "마이페이지 날짜별 플래너 조회 성공",
                        chatService.getPlansByDateForMyPage(userId, date)
                )
        );
    }

    // 방문 장소 목록 조회
    @GetMapping("/mypage/places")
    public ResponseEntity<BaseResponse<List<VisitedPlaceResDto>>> getVisitedPlaces(
            @AuthenticationPrincipal UserPrincipal user
    ) {
        Long userId = user.userId();
        return ResponseEntity.ok(
                BaseResponse.success(
                        "방문 장소 목록 조회 성공",
                        chatService.getVisitedPlaces(userId)
                )
        );
    }

}
