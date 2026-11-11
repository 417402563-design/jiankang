package cn.edu.sdua.jiankang.controller;

import cn.edu.sdua.jiankang.dto.ApiResponse;
import cn.edu.sdua.jiankang.dto.AuthDto;
import cn.edu.sdua.jiankang.entity.User;
import cn.edu.sdua.jiankang.repository.UserRepository;
import cn.edu.sdua.jiankang.security.JwtUtil;
import cn.edu.sdua.jiankang.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 认证与用户管理接口
 */
@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public AuthController(AuthService authService,
                          AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          UserRepository userRepository) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @PostMapping("/auth/register")
    public ApiResponse<AuthDto.AuthResponse> register(@Valid @RequestBody AuthDto.RegisterRequest req) {
        return ApiResponse.ok("注册成功", authService.register(req));
    }

    @PostMapping("/auth/login")
    public ApiResponse<AuthDto.AuthResponse> login(@Valid @RequestBody AuthDto.LoginRequest req) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        String token = jwtUtil.generateToken(user);
        long exp = System.currentTimeMillis() + jwtUtil.getExpirationMs();
        AuthDto.AuthResponse resp = new AuthDto.AuthResponse(
                token, exp, toInfo(user));
        return ApiResponse.ok("登录成功", resp);
    }

    @GetMapping("/auth/me")
    public ApiResponse<AuthDto.UserInfo> me() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.ok(authService.me(username));
    }

    // ===== 管理员接口 =====

    @GetMapping("/admin/users")
    public ApiResponse<List<AuthDto.UserInfo>> listUsers() {
        return ApiResponse.ok(authService.listUsers());
    }

    @PutMapping("/admin/users/{id}/role")
    public ApiResponse<AuthDto.UserInfo> updateRole(@PathVariable Long id,
                                                    @RequestParam String role) {
        return ApiResponse.ok(authService.updateRole(id, role));
    }

    private AuthDto.UserInfo toInfo(User u) {
        AuthDto.UserInfo info = new AuthDto.UserInfo();
        info.setId(u.getId());
        info.setUsername(u.getUsername());
        info.setNickname(u.getNickname());
        info.setEmail(u.getEmail());
        info.setRoles(u.getRoles().stream().map(r -> r.getName()).toList());
        return info;
    }
}
