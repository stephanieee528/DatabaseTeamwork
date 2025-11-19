package com.example.poverty.controller;

import com.example.poverty.model.SysUser;
import com.example.poverty.model.Role;
import com.example.poverty.repository.SysUserRepository;
import com.example.poverty.repository.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final SysUserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;

    public UserController(SysUserRepository userRepo, RoleRepository roleRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.passwordEncoder = passwordEncoder;
    }

    // 注册接口
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest request) {
        try {
            // 检查用户名是否已存在
            Optional<SysUser> existingUser = userRepo.findByUsername(request.username());
            if (existingUser.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "用户名已存在"
                ));
            }

            // 查找角色（默认为普通用户）
            Role role = roleRepo.findByRoleName(request.role())
                    .orElse(roleRepo.findByRoleName("普通用户")
                            .orElseThrow(() -> new RuntimeException("默认角色不存在")));

            // 创建新用户
            SysUser newUser = SysUser.builder()
                    .username(request.username())
                    .passwordHash(passwordEncoder.encode(request.password()))
                    .email(request.email())
                    .role(role)
                    .fullname(request.fullname() != null ? request.fullname() : request.username())
                    .build();

            userRepo.save(newUser);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "注册成功，请登录"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "注册失败: " + e.getMessage()
            ));
        }
    }

    @GetMapping
    public ResponseEntity<?> listUsers(Authentication authentication) {
        try {
            // 检查认证
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "未认证"
                ));
            }

            List<SysUser> users = userRepo.findAll();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "获取用户列表失败: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "未认证"
                ));
            }

            userRepo.deleteById(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "用户删除成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "删除用户失败: " + e.getMessage()
            ));
        }
    }
   
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        try {
            // 检查认证对象是否存在
            if (authentication == null) {
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "未认证 - Authentication 对象为 null"
                ));
            }

            // 检查是否已认证
            if (!authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "未认证 - 用户未通过认证"
                ));
            }

            String username = authentication.getName();
            SysUser user = userRepo.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            String roleName = user.getRole() != null ? user.getRole().getRoleName() : "普通用户";

            return ResponseEntity.ok(new SysUserDTO(
                user.getUserId(),
                user.getUsername(),
                user.getFullname(),
                user.getEmail(),
                roleName,
                LocalDateTime.now()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "获取用户信息失败: " + e.getMessage()
            ));
        }
    }

    // 调试接口 - 检查认证状态
    @GetMapping("/debug/auth")
    public ResponseEntity<?> debugAuth(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.ok(Map.of(
                    "authenticated", false,
                    "message", "未认证 - Authentication 对象为 null"
                ));
            }
            
            return ResponseEntity.ok(Map.of(
                "authenticated", authentication.isAuthenticated(),
                "username", authentication.getName(),
                "authorities", authentication.getAuthorities().stream()
                        .map(a -> a.getAuthority())
                        .collect(Collectors.toList()),
                "principal", authentication.getPrincipal().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }

    // 修改当前用户信息
    @PutMapping("/current")
    public ResponseEntity<?> updateCurrentUser(
            Authentication authentication,
            @RequestBody UserUpdateRequest request
    ) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "未认证"
                ));
            }

            String username = authentication.getName();
            SysUser user = userRepo.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            user.setFullname(request.fullname());
            user.setEmail(request.email());
            userRepo.save(user);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "用户信息更新成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "更新用户信息失败: " + e.getMessage()
            ));
        }
    }

    // DTO 调整为匹配实体类字段
    public record SysUserDTO(
        Long userId,
        String username,
        String fullname,
        String email,
        String roleName,
        LocalDateTime registerTime
    ) {}

    // 注册请求DTO
    public record RegisterRequest(
        String username,
        String password,
        String email,
        String role,
        String fullname
    ) {}

    public record UserUpdateRequest(String fullname, String email) {}
}