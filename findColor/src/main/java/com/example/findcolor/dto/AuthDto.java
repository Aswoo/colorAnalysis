package com.example.findcolor.dto;

import lombok.Getter;
import lombok.Setter;

public class AuthDto {

    @Getter
    @Setter
    public static class SignupRequest {
        private String email;
        private String password;
        private String nickname;
    }

    @Getter
    @Setter
    public static class LoginRequest {
        private String email;
        private String password;
    }

    @Getter
    @Setter
    public static class UserResponse {
        private Long id;
        private String email;
        private String nickname;

        public UserResponse(Long id, String email, String nickname) {
            this.id = id;
            this.email = email;
            this.nickname = nickname;
        }
    }
}
