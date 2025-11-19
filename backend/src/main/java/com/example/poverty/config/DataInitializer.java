package com.example.poverty.config;

import com.example.poverty.model.Role;
import com.example.poverty.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    public DataInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // 检查并初始化角色数据
        initializeRole("普通用户");
        initializeRole("数据分析师");
        initializeRole("管理员");
        
        System.out.println("✅ 角色数据初始化完成");
    }
    
    private void initializeRole(String roleName) {
        if (!roleRepository.findByRoleName(roleName).isPresent()) {
            Role role = Role.builder()
                    .roleName(roleName)
                    .build();
            roleRepository.save(role);
            System.out.println("✅ 创建角色: " + roleName);
        } else {
            System.out.println("✅ 角色已存在: " + roleName);
        }
    }
}