package com.example.poverty.config;

import com.example.poverty.model.Role;
import com.example.poverty.model.SysUser;
import com.example.poverty.repository.RoleRepository;
import com.example.poverty.repository.SysUserRepository;
import com.example.poverty.security.RoleConstants;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final SysUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository,
                           SysUserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // 检查并初始化角色数据
        initializeRole(RoleConstants.ROLE_CITIZEN_NAME);
        initializeRole(RoleConstants.ROLE_ANALYST_NAME);
        initializeRole(RoleConstants.ROLE_ADMIN_NAME);

        // 确保默认账号存在
        initializeAdminUser();
        initializeDefaultUser("citizen01", "Citizen@123", RoleConstants.ROLE_CITIZEN_NAME, "示例群众用户", "citizen01@example.com");
        initializeDefaultUser("analyst01", "Analyst@123", RoleConstants.ROLE_ANALYST_NAME, "示例分析师", "analyst01@example.com");

        System.out.println("✅ 基础数据初始化完成");
    }

    private void initializeRole(String roleName) {
        if (roleRepository.findByRoleName(roleName).isPresent()) {
            System.out.println("✅ 角色已存在: " + roleName);
            return;
        }

        Role role = Role.builder()
                .roleName(roleName)
                .build();
        roleRepository.save(role);
        System.out.println("✅ 创建角色: " + roleName);
    }

    private void initializeAdminUser() {
        if (userRepository.findByUsername("admin").isPresent()) {
            System.out.println("✅ 管理员账号已存在");
            return;
        }

        Role adminRole = roleRepository.findByRoleName(RoleConstants.ROLE_ADMIN_NAME)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .roleName(RoleConstants.ROLE_ADMIN_NAME)
                        .build()));

        SysUser admin = SysUser.builder()
                .username("admin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .role(adminRole)
                .fullname("系统管理员")
                .email("admin@example.com")
                .build();

        userRepository.save(admin);
        System.out.println("✅ 默认管理员账号已创建（admin / admin123）");
    }

    private void initializeDefaultUser(String username, String rawPassword, String roleName, String fullname, String email) {
        if (userRepository.findByUsername(username).isPresent()) {
            return;
        }
        Role role = roleRepository.findByRoleName(roleName)
                .orElseGet(() -> roleRepository.save(Role.builder().roleName(roleName).build()));
        SysUser user = SysUser.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(role)
                .fullname(fullname)
                .email(email)
                .build();
        userRepository.save(user);
        System.out.printf("✅ 默认%s账号已创建（%s / %s）%n", roleName, username, rawPassword);
    }
}