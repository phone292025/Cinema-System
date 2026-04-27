package com.cinema.auth;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.cinema.user.User;
import com.cinema.user.UserRole;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record AuthUser(UUID id, String name, String email, UserRole role) implements UserDetails {
    public static AuthUser from(User user) {
        return new AuthUser(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return email;
    }
}
