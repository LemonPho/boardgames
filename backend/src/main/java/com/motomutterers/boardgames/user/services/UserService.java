package com.motomutterers.boardgames.user.services;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.motomutterers.boardgames.auth.exceptions.UserUnauthorizedException;
import com.motomutterers.boardgames.exceptions.BadActionException;
import com.motomutterers.boardgames.user.UserRepository;
import com.motomutterers.boardgames.user.dto.UserResponse;
import com.motomutterers.boardgames.user.exceptions.UserNotFoundException;
import com.motomutterers.boardgames.user.model.User;
import com.motomutterers.boardgames.user.model.UserStatus;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(
        UserRepository userRepository
    ) {
        this.userRepository = userRepository;
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

    @Transactional
    public void updateUsername(UUID id, String username) {
        User user = getUserById(id);

        if (!user.canChangeUsername()) {
            throw new BadActionException("User can't change username");
        }

        user.setUsername(username);
        user.setUsernameLastEdited(LocalDateTime.now());
        userRepository.save(user);
    }

    public UserResponse getAuthenticatedUser(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return new UserResponse(getUserById(userId));
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
