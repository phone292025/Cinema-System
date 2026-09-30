package com.cinema.auth;

import java.io.IOException;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    static final String INVALID_TOKEN_ATTRIBUTE = JwtAuthenticationFilter.class.getName() + ".invalidToken";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            Optional<AuthUser> user = jwtService.parse(header.substring(7));
            if (user.isPresent()) {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user.get(), null, user.get().getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                request.setAttribute(INVALID_TOKEN_ATTRIBUTE, Boolean.TRUE);
            }
        }
        filterChain.doFilter(request, response);
    }
}
