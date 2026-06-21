package com.motomutterers.boardgames.auth.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.motomutterers.boardgames.auth.models.VerificationToken;
import com.motomutterers.boardgames.user.model.User;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {
    Optional<VerificationToken> findByUser(User user);
    Optional<VerificationToken> deleteByUser(User user);
    Optional<VerificationToken> findByToken(String token);
}
