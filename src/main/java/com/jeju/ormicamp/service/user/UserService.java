package com.jeju.ormicamp.service.user;

import com.jeju.ormicamp.common.constants.UserRole;
import com.jeju.ormicamp.infrastructure.repository.user.UserRepository;
import com.jeju.ormicamp.model.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * JWT 토큰 검증 시 UserEntity를 생성하기 위한 service
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User getOrCreateUser(String sub, String email){

        return userRepository.findByCognitoSub(sub)
                .orElseGet(() ->
                        userRepository.save(
                                User.builder()
                                        .cognitoSub(sub)
                                        .email(email)
                                        .role(UserRole.USER)
                                        .build()
                        )
                );
    }
}
