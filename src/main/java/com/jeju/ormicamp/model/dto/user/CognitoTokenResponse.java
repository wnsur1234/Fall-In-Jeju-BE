package com.jeju.ormicamp.model.dto.user;

public record CognitoTokenResponse(
        String access_token,
        String id_token,
        String token_type,
        Integer expires_in
) {}