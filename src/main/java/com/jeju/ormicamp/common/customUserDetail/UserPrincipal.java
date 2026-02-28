package com.jeju.ormicamp.common.customUserDetail;

import com.jeju.ormicamp.common.constants.UserRole;

public record UserPrincipal (
        Long userId,
        String cognitoSub,
        UserRole role
){}

