package com.jeju.ormicamp.common.exception;

import com.jeju.ormicamp.common.dto.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<BaseResponse<?>> handleCustomException(CustomException ex) {

        ErrorCode errorCode = ex.getErrorCode();
        log.warn("[CustomException] code={}, message={}", errorCode.getCode(), errorCode.getMessage(), ex);

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(BaseResponse.error(errorCode));
    }

    // 예상치 못한 모든 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<?>> handleException(Exception ex) {

        log.error("[Unhandled Exception] {}", ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
