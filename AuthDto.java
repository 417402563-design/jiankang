package cn.edu.sdua.jiankang.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 认证相关请求/响应 DTO
 */
public class AuthDto {

    @Data
    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
        private String nickname;
        private String email;
        /** 注册时指定角色,默认 ROLE_USER */
        private String role;
    }

    @Data
    public static class AuthResponse {
        private String token;
        private String tokenType = "Bearer";
        private Long expiresAt;
        private UserInfo user;

        public AuthResponse(String token, Long expiresAt, UserInfo user) {
            this.token = token;
            this.expiresAt = expiresAt;
            this.user = user;
        }
    }

    @Data
    public static class UserInfo {
        private Long id;
        private String username;
        private String nickname;
        private String email;
        private java.util.List<String> roles;
    }
}
