package com.example.poverty.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // 公开访问的接口
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/users/register").permitAll()
                        .requestMatchers("/api/users/debug/**").permitAll()
                        
                        // 角色控制
                        .requestMatchers(HttpMethod.GET, "/api/counties/**",
                                "/api/indicators/**",
                                "/api/dashboard/**").hasAnyRole("ADMIN", "ANALYST", "CITIZEN")
                        .requestMatchers(HttpMethod.POST, "/api/counties/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/counties/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/counties/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/users/current").hasAnyRole("ADMIN", "ANALYST", "CITIZEN")
                        .requestMatchers("/api/users/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/alerts/**").hasAnyRole("ADMIN", "ANALYST")
                        .requestMatchers("/api/alerts/**").hasRole("ADMIN")

                        // 需要认证的其他接口
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                // 添加JWT过滤器
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }
}