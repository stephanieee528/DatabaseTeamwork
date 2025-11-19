package com.example.poverty.controller;

import com.example.poverty.model.SysUser;
import com.example.poverty.repository.SysUserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final SysUserRepository userRepo;

    public UserController(SysUserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @GetMapping
    public List<SysUser> listUsers() {
        return userRepo.findAll();
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        userRepo.deleteById(id);
    }
}