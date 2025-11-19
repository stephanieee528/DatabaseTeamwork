package com.example.poverty.controller;

import com.example.poverty.model.SysUser;
import com.example.poverty.model.Role;
import com.example.poverty.repository.SysUserRepository;
import com.example.poverty.repository.RoleRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final SysUserRepository userRepo;
    private final RoleRepository roleRepo; // 统一使用 roleRepo
    private final PasswordEncoder passwordEncoder;

    // 使用 SecretKey 代替普通字符串，保证 HS256 安全
    private final SecretKey jwtSecret = Keys.hmacShaKeyFor("0123456789abcdef0123456789abcdef".getBytes());
    private final long expirationMs = 24 * 60 * 60 * 1000; // 1天

    public AuthController(SysUserRepository userRepo, RoleRepository roleRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo; // 统一使用 roleRepo
        this.passwordEncoder = passwordEncoder;
    }

    // ------------------------
    // 登录
    // ------------------------
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            Optional<SysUser> userOpt = userRepo.findByUsername(req.username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "用户不存在"
                ));
            }

            SysUser user = userOpt.get();
            if (!passwordEncoder.matches(req.password, user.getPasswordHash())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "密码错误"
                ));
            }

            String token = Jwts.builder()
                    .setSubject(user.getUsername())
                    .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                    .signWith(jwtSecret, SignatureAlgorithm.HS256)
                    .compact();

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "登录成功",
                "token", token,
                "user", Map.of(
                    "userId", user.getUserId(),
                    "username", user.getUsername(),
                    "fullname", user.getFullname(),
                    "email", user.getEmail(),
                    "role", user.getRole() != null ? user.getRole().getRoleName() : "普通用户"
                )
            ));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "登录失败: " + e.getMessage()
            ));
        }
    }

    // ------------------------
    // 注册
    // ------------------------
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        try {
            if (userRepo.findByUsername(req.username).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "用户名已存在"
                ));
            }

            // 改进角色查找逻辑
            Role role;
            if (req.role != null && !req.role.trim().isEmpty()) {
                // 尝试查找前端指定的角色
                role = roleRepo.findByRoleName(req.role)
                        .orElse(roleRepo.findByRoleName("普通用户")
                                .orElseGet(() -> createDefaultRole()));
            } else {
                // 如果没有指定角色，使用默认角色
                role = roleRepo.findByRoleName("普通用户")
                        .orElseGet(() -> createDefaultRole());
            }

            SysUser user = SysUser.builder()
                    .username(req.username)
                    .passwordHash(passwordEncoder.encode(req.password))
                    .role(role)
                    .fullname(req.fullname != null ? req.fullname : req.username)
                    .email(req.email)
                    .build();

            userRepo.save(user);

            // 注册成功，返回成功消息
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "注册成功，请登录"
            ));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "注册失败: " + e.getMessage()
            ));
        }
    }

    // 创建默认角色的方法
    private Role createDefaultRole() {
        Role defaultRole = Role.builder()
                .roleName("普通用户")
                .build();
        return roleRepo.save(defaultRole);
    }

    // 调试接口 - 检查角色
    @GetMapping("/debug/roles")
    public ResponseEntity<?> debugRoles() {
        try {
            List<Role> allRoles = roleRepo.findAll();
            List<String> roleNames = allRoles.stream()
                    .map(Role::getRoleName)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "totalRoles", allRoles.size(),
                "roleNames", roleNames
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }

    // 检查数据库状态
    @GetMapping("/check-db")
    public ResponseEntity<?> checkDatabase() {
        try {
            long roleCount = roleRepo.count();
            List<Role> allRoles = roleRepo.findAll();
            long userCount = userRepo.count();
            
            return ResponseEntity.ok(Map.of(
                "roleCount", roleCount,
                "roles", allRoles.stream()
                        .map(role -> Map.of(
                            "id", role.getRoleId(),
                            "name", role.getRoleName()
                        ))
                        .collect(Collectors.toList()),
                "userCount", userCount
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }

    // DTO
    public record LoginRequest(String username, String password) {}
    public record RegisterRequest(String username, String password, String role, String fullname, String email) {}
}