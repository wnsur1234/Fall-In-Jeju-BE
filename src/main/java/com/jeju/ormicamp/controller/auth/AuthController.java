package com.jeju.ormicamp.controller.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.jeju.ormicamp.common.dto.BaseResponse;
import com.jeju.ormicamp.common.jwt.util.JWTUtil;
import com.jeju.ormicamp.model.domain.User;
import com.jeju.ormicamp.model.dto.user.CognitoTokenResponse;
import com.jeju.ormicamp.service.user.CognitoAuthService;
import com.jeju.ormicamp.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final CognitoAuthService cognitoAuthService;
    private final JWTUtil jwtUtil;
    private final UserService userService;

    /**
     * Cognito 로그인/회원가입 콜백
     * - 신규 유저면 자동 회원가입
     * - 기존 유저면 로그인
     */
    @GetMapping("/callback")
    public ResponseEntity<BaseResponse<String>> callback(
            @RequestParam("code") String code
    ) {

        // Authorization Code → Token 교환
        CognitoTokenResponse tokenResponse =
                cognitoAuthService.exchangeCodeForTokens(code);

        DecodedJWT idJwt = jwtUtil.verify(tokenResponse.id_token());

        String email = idJwt.getClaim("email").asString();

        String sub = idJwt.getSubject();

        userService.getOrCreateUser(sub, email);

        return ResponseEntity.ok().body(BaseResponse.success("로그인 성공",tokenResponse.id_token()));
    }

}
