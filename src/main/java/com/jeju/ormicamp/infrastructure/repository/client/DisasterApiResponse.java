package com.jeju.ormicamp.infrastructure.repository.client;

import lombok.Getter;

import java.util.List;

// 최상위
@Getter
public class DisasterApiResponse {

    private Header header;

    // 핵심: body는 객체가 아니라 배열
    private List<ApiDisasterMessage> body;

    @Getter
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }
}

