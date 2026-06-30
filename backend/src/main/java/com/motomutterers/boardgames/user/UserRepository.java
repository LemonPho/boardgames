package com.motomutterers.boardgames.user;

import org.springframework.data.jpa.repository.JpaRepository;

import com.motomutterers.boardgames.user.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByEmailAndVerified(String email, boolean verified);
    Optional<User> findByUsername(String username);
    List<User> findByUsernameContainingIgnoreCase(String username);
}
