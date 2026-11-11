package cn.edu.sdua.jiankang.service;

import cn.edu.sdua.jiankang.dto.AuthDto;
import cn.edu.sdua.jiankang.entity.Role;
import cn.edu.sdua.jiankang.entity.User;
import cn.edu.sdua.jiankang.repository.RoleRepository;
import cn.edu.sdua.jiankang.repository.UserRepository;
import cn.edu.sdua.jiankang.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 用户认证与注册服务 —— 用户权限管理模块
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User u = new User();
        u.setUsername(req.getUsername());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setNickname(req.getNickname() != null ? req.getNickname() : req.getUsername());
        u.setEmail(req.getEmail());
        u.setEnabled(true);
        String roleName = (req.getRole() == null || req.getRole().isBlank())
                ? "ROLE_USER" : req.getRole();
        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.findByName("ROLE_USER").orElseThrow());
        u.setRoles(Set.of(role));
        u = userRepository.save(u);
        return buildAuthResponse(u);
    }

    public AuthDto.UserInfo me(String username) {
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return toUserInfo(u);
    }

    public List<AuthDto.UserInfo> listUsers() {
        return userRepository.findAll().stream().map(this::toUserInfo).toList();
    }

    public AuthDto.UserInfo updateRole(Long userId, String roleName) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("角色不存在: " + roleName));
        u.setRoles(Set.of(role));
        return toUserInfo(userRepository.save(u));
    }

    private AuthDto.AuthResponse buildAuthResponse(User u) {
        String token = jwtUtil.generateToken(u);
        long exp = System.currentTimeMillis() + jwtUtil.getExpirationMs();
        return new AuthDto.AuthResponse(token, exp, toUserInfo(u));
    }

    private AuthDto.UserInfo toUserInfo(User u) {
        AuthDto.UserInfo info = new AuthDto.UserInfo();
        info.setId(u.getId());
        info.setUsername(u.getUsername());
        info.setNickname(u.getNickname());
        info.setEmail(u.getEmail());
        info.setRoles(u.getRoles().stream().map(Role::getName).toList());
        return info;
    }
}
