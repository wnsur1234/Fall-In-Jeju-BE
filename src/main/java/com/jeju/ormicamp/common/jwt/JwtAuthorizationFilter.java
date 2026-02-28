package com.jeju.ormicamp.common.jwt;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.jeju.ormicamp.common.customUserDetail.UserPrincipal;
import com.jeju.ormicamp.common.jwt.util.JWTUtil;
import com.jeju.ormicamp.model.domain.User;
import com.jeju.ormicamp.service.user.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * AcessToken 검증 필터
 */
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;
    private final UserService userService;

    public JwtAuthorizationFilter(JWTUtil jwtUtil,UserService userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            DecodedJWT jwt = jwtUtil.verify(token);

            String sub = jwtUtil.getSub(jwt);
            String email = jwtUtil.getEmail(jwt);

            User user = userService.getOrCreateUser(sub, email);

            UserPrincipal principal = new UserPrincipal(
                    user.getId(),
                    user.getCognitoSub(),
                    user.getRole()
            );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String accept = request.getHeader("Accept");

        return path.startsWith("/api/sse")
                || (accept != null && accept.contains("text/event-stream"));
    }
}
