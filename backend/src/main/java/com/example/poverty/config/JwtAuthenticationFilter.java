package com.example.poverty.config;

import com.example.poverty.security.RoleConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final SecretKey jwtSecret = Keys.hmacShaKeyFor("0123456789abcdef0123456789abcdef".getBytes());

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = getTokenFromRequest(request);
            
            if (token != null && validateToken(token)) {
                Claims claims = parseClaims(token);
                String username = claims.getSubject();
                
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // 创建认证令牌
                    UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(username, null, getAuthoritiesFromClaims(claims));
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception e) {
            logger.error("无法设置用户认证: " + e);
        }
        
        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(jwtSecret)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(jwtSecret)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private List<SimpleGrantedAuthority> getAuthoritiesFromClaims(Claims claims) {
        String authority = claims.get("authority", String.class);
        if (authority == null || authority.isBlank()) {
            String roleName = claims.get("role", String.class);
            authority = RoleConstants.toAuthority(roleName);
        }
        if (authority == null || authority.isBlank()) {
            authority = RoleConstants.AUTHORITY_CITIZEN;
        }
        return Collections.singletonList(new SimpleGrantedAuthority(authority));
    }
}