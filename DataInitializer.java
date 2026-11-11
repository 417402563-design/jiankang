package cn.edu.sdua.jiankang.config;

import cn.edu.sdua.jiankang.entity.Role;
import cn.edu.sdua.jiankang.entity.User;
import cn.edu.sdua.jiankang.repository.RoleRepository;
import cn.edu.sdua.jiankang.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 初始化角色与默认管理员账号
 * 默认:admin/admin123(管理员),editor/editor123(编辑)
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        Role admin = ensureRole("ROLE_ADMIN", "系统管理员:全部权限");
        Role editor = ensureRole("ROLE_EDITOR", "知识编辑:可维护知识库");
        Role user = ensureRole("ROLE_USER", "普通用户:可问答与搜索");

        ensureUser("admin", "admin123", "系统管理员", Set.of(admin, editor, user));
        ensureUser("editor", "editor123", "知识编辑", Set.of(editor, user));
        ensureUser("user", "user123", "普通用户", Set.of(user));

        log.info("初始化完成,默认账号 admin/admin123, editor/editor123, user/user123");
    }

    private Role ensureRole(String name, String desc) {
        return roleRepository.findByName(name).orElseGet(() -> {
            Role r = new Role();
            r.setName(name);
            r.setDescription(desc);
            return roleRepository.save(r);
        });
    }

    private void ensureUser(String username, String rawPwd, String nickname, Set<Role> roles) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(rawPwd));
        u.setNickname(nickname);
        u.setEnabled(true);
        u.setRoles(roles);
        userRepository.save(u);
    }
}
