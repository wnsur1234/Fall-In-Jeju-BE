package com.jeju.ormicamp.service.user;

import com.jeju.ormicamp.model.dto.user.CognitoTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class CognitoAuthService {

    @Value("${cognito.client-id}")
    private String clientId;

    @Value("${cognito.client-secret}")
    private String clientSecret;

    @Value("${cognito.redirect-uri}")
    private String redirectUri;

    @Value("${cognito.domain}")
    private String cognitoDomain;

    private final RestClient restClient = RestClient.create();

    // 1) Cognito Authorization Code → Token 교환
    public CognitoTokenResponse exchangeCodeForTokens(String code) {

        try{
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "authorization_code");
            form.add("client_id", clientId);
            form.add("client_secret", clientSecret);
            form.add("redirect_uri", redirectUri);
            form.add("code", code);

            return restClient.post()
                    .uri(cognitoDomain + "/oauth2/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(CognitoTokenResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

}
