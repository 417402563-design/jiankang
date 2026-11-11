package cn.edu.sdua.jiankang.security;

import cn.edu.sdua.jiankang.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 从数据库加载用户及其角色权限
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .map(u -> User.builder()
                        .username(u.getUsername())
                        .password(u.getPassword())
                        .disabled(!Boolean.TRUE.equals(u.getEnabled()))
                        .authorities(u.getRoles().stream()
                                .map(r -> new SimpleGrantedAuthority(r.getName()))
                                .toList())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));
    }
}
