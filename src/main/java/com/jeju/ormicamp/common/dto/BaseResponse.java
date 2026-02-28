package com.jeju.ormicamp.common.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.jeju.ormicamp.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"status", "message", "data", "code"})
public class BaseResponse<T> {
    private final int status;
    private final String message;
    private final T data;
    private final String code;   // 성공 시 null, 에러 시 ErrorCode.code

    private BaseResponse(int status, String message, T data, String code) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.code = code;
    }

    public static <T> BaseResponse<T> success(HttpStatus httpStatus, String message, T data) {
        return new BaseResponse<>(httpStatus.value(), message, data, null);
    }

    public static <T> BaseResponse<T> success(String message, T data) {
        return new BaseResponse<>(HttpStatus.OK.value(), message, data, null);
    }

    public static <T> BaseResponse<T> error(ErrorCode errorCode) {
        return new BaseResponse<>(
                errorCode.getHttpStatus().value(),
                errorCode.getMessage(),
                null,
                errorCode.getCode()
        );
    }

    public static <T> BaseResponse<T> error(ErrorCode errorCode, String customMessage) {
        return new BaseResponse<>(
                errorCode.getHttpStatus().value(),
                customMessage,
                null,
                errorCode.getCode()
        );
    }
}