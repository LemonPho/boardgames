package com.motomutterers.boardgames.user.services;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.motomutterers.boardgames.auth.exceptions.UserUnauthorizedException;
import com.motomutterers.boardgames.exceptions.BadActionException;
import com.motomutterers.boardgames.exceptions.ValidationBuilder;
import com.motomutterers.boardgames.user.UserRepository;
import com.motomutterers.boardgames.user.dto.UserResponse;
import com.motomutterers.boardgames.user.exceptions.UserNotFoundException;
import com.motomutterers.boardgames.user.model.User;
import com.motomutterers.boardgames.user.model.UserStatus;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User was not found"));
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    public List<User> findAllUserContainingUsername(String username) {
        return userRepository.findByUsernameContainingIgnoreCase(username);
    }

    public User getAuthenticatedUser(Authentication authentication){
        UUID userId = UUID.fromString(authentication.getName());
        return getUserById(userId);
    }

    @Transactional
    public void updateUsername(UUID id, String username) {
        User user = getUserById(id);

        if (!user.canChangeUsername()) {
            throw new BadActionException("User can't change username");
        }

        // No-op if unchanged; otherwise the new name must be free.
        if (!username.equals(user.getUsername())) {
            new ValidationBuilder()
                .addError(userRepository.findByUsername(username).isPresent(), "username", "Username is already taken")
                .validate();
        }

        user.setUsername(username);
        user.setUsernameLastEdited(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public void updatePassword(UUID id, String currentPassword, String newPassword) {
        User user = getUserById(id);

        new ValidationBuilder()
            .addError(!passwordEncoder.matches(currentPassword, user.getPasswordHash()), "currentPassword", "Password is incorrect")
            .validate();

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public UserResponse getAuthenticatedUserResponse(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return new UserResponse(getUserById(userId));
    }

    public UserResponse getUserResponseByUsername(String username) {
        return new UserResponse(getUserByUsername(username));
    }

    public List<UserResponse> matchAllByUsername(String username) {
        List<User> users = findAllUserContainingUsername(username);
        List<UserResponse> response = users.stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());

        return response;
    }

    @Transactional
    public void deleteUser(UUID requesterId, UUID targetUserId) {
        if (!requesterId.equals(targetUserId)) {
            throw new UserUnauthorizedException("A normal user tried to delete another user");
        }

        User user = getUserById(targetUserId);

        if (user.getStatus().equals(UserStatus.DELETED)) {
            throw new BadActionException("User already deleted");
        }

        user.setIsDeleted();
        userRepository.save(user);
    }
}
