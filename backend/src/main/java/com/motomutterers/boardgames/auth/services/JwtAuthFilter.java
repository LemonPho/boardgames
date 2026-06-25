package com.motomutterers.boardgames.auth.services;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.motomutterers.boardgames.user.UserRepository;
import com.motomutterers.boardgames.user.model.User;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter{
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(
        JwtService jwtService,
        UserRepository userRepository
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if(authorization == null || !authorization.startsWith("Bearer ")){
            filterChain.doFilter(request, response);
            return;
        }

        String bearerToken = authorization.replace("Bearer ", "");
        if(!jwtService.isTokenValid(bearerToken)){
            filterChain.doFilter(request, response);
            return;
        }

        String userId = jwtService.extractUserId(bearerToken);
        UUID userUuid = UUID.fromString(userId);
        Optional<User> result = userRepository.findById(userUuid);
        if(result.isEmpty()){
            filterChain.doFilter(request, response);
            return;
        }
        User user = result.get();

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(user.getId().toString(), null, authorities);
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authToken);
        filterChain.doFilter(request, response);
    }
}
