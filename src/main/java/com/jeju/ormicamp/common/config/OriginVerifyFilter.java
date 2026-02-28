package com.jeju.ormicamp.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class OriginVerifyFilter extends OncePerRequestFilter {

    @Value("${origin.verify-secret}")
    private String originVerifySecret;

    private static final String X_ORIGIN_VERIFY_HEADER = "X-Origin-Verify";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

//        String uri = request.getRequestURI();
//        String headerValue = request.getHeader(X_ORIGIN_VERIFY_HEADER);
//
//        // 1️⃣ 외부 서비스가 호출하는 엔드포인트는 무조건 통과
//        if (uri.startsWith("/actuator/") || uri.startsWith("/api/auth/")) {
//            filterChain.doFilter(request, response);
//            return;
//        }
//
//        // 2️⃣ 내부 서비스 요청만 X-Origin-Verify 검증
//        if (originVerifySecret.equals(headerValue)) {
//            filterChain.doFilter(request, response);
//        } else {
//            response.sendError(
//                    HttpServletResponse.SC_FORBIDDEN,
//                    "Forbidden: Invalid X-Origin-Verify header"
//            );
//
//        }
        filterChain.doFilter(request, response);
    }
}
