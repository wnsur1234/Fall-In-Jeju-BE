package com.jeju.ormicamp.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 양식
 * <p>
 * 1. 이름: 도메인_상황
 * 2. 내용: http 상태코드, 분류코드(이름_상태코드), 사용자 친화적 메시지
 * 각 도메인 별로 ErrorCode를 분리해주세요.
 */
@Getter
public enum ErrorCode {
    /*
    GlobalExceptionHandler에서 사용
    얘네는 ENUM임
     */
    // 400 에러 기본
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "지원하지 않는 HTTP Method입니다."),
    NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE, "NOT_ACCEPTABLE", "요청된 리소스를 제공할 수 없습니다."),
    // 500에러 기본
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),

    // 여기부터 에러 커스텀
    // Date 도메인
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "시작날짜와 종료날짜를 확인해 주세요"),

    // Chat 도메인
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_NOT_FOUND", "채팅방을 찾을 수 없습니다."),
    CHAT_MESSAGE_EMPTY(HttpStatus.BAD_REQUEST, "CHAT_MSG_EMPTY", "메시지 내용은 비어있을 수 없습니다."),
    CHAT_SESSION_MISSING(HttpStatus.BAD_REQUEST, "CHAT_SESSION_MISSING", "세션 ID가 누락되었습니다."),

    // Planner 도메인
    PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "PLAN_NOT_FOUND", "해당 날짜의 여행 일정을 찾을 수 없습니다."),
    PLAN_SESSION_MISSING(HttpStatus.BAD_REQUEST, "PLAN_SESSION_MISSING", "세션 ID가 누락되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
