package com.example.poverty.model;

import jakarta.persistence.*;

@Entity
@Table(name = "role")
public class Role {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roleId;
    
    @Column(unique = true, nullable = false)
    private String roleName;
    
    // 无参构造函数
    public Role() {}
    
    // 全参构造函数
    public Role(Long roleId, String roleName) {
        this.roleId = roleId;
        this.roleName = roleName;
    }
    
    // Getter 和 Setter 方法
    public Long getRoleId() { 
        return this.roleId; 
    }
    
    public void setRoleId(Long roleId) { 
        this.roleId = roleId; 
    }
    
    public String getRoleName() { 
        return this.roleName; 
    }
    
    public void setRoleName(String roleName) { 
        this.roleName = roleName; 
    }
    
    // 构建器模式
    public static RoleBuilder builder() {
        return new RoleBuilder();
    }
    
    public static class RoleBuilder {
        private Long roleId;
        private String roleName;
        
        public RoleBuilder roleId(Long roleId) { 
            this.roleId = roleId; 
            return this; 
        }
        
        public RoleBuilder roleName(String roleName) { 
            this.roleName = roleName; 
            return this; 
        }
        
        public Role build() {
            Role role = new Role();
            role.setRoleId(this.roleId);
            role.setRoleName(this.roleName);
            return role;
        }
    }
}