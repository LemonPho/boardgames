package com.motomutterers.boardgames.auth;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.motomutterers.boardgames.auth.models.RefreshToken;
import com.motomutterers.boardgames.user.model.User;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByUser(User user);
    void deleteByUser(User user);
    Optional<RefreshToken> findByToken(String token);
}
