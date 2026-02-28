package com.jeju.ormicamp.common.jwt.util;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.security.interfaces.RSAPublicKey;

@Component
public class JWTUtil {

    private final String issuer;
    private final JwkProvider jwkProvider;

    public JWTUtil(
            @Value("${cognito.issuer}") String issuer,
            @Value("${cognito.jwks-url}") String jwksUrl) throws Exception {
        this.issuer = issuer;
        this.jwkProvider = new UrlJwkProvider(new URL(jwksUrl));
    }

    public String getSub(DecodedJWT jwt) {
        return jwt.getSubject();
    }

    public String getEmail(DecodedJWT jwt) {
        return jwt.getClaim("email").asString();
    }

    public DecodedJWT verify(String token) {
        DecodedJWT decoded = JWT.decode(token);

        try {
            Jwk jwk = jwkProvider.get(decoded.getKeyId());
            RSAPublicKey publicKey = (RSAPublicKey) jwk.getPublicKey();

            Algorithm algorithm = Algorithm.RSA256(publicKey, null);
            JWTVerifier verifier = JWT
                    .require(algorithm)
                    .withIssuer(issuer)
                    .acceptLeeway(60) // 시간 오프셋(Clock Skew) 허용: 60초
                    .build();

            return verifier.verify(token);

        } catch (Exception e) {
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    public String getUserId(String token) {
        return verify(token).getSubject();
    }
}
