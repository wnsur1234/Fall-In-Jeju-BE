package com.jeju.ormicamp.model.domain;

import com.jeju.ormicamp.common.constants.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false,unique = true)
    private String cognitoSub;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    // 게시판 api 개발 전까지 안씀
    private UserRole role;

    @Builder
    public User(String cognitoSub, String email, UserRole role) {
        this.cognitoSub = cognitoSub;
        this.email = email;
        this.role = role;
    }




}
