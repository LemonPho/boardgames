package com.motomutterers.boardgames.user.services;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.motomutterers.boardgames.auth.exceptions.UserUnauthorizedException;
import com.motomutterers.boardgames.exceptions.BadActionException;
import com.motomutterers.boardgames.user.UserRepository;
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

    public User getUserById(UUID id){
        return userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException("User was not found"));
    }

    public Optional<User> getUserByUsername(String username){
        return userRepository.findByUsername(username);
    }

    @Transactional
    public void updateUsername(UUID id, String username){
        User user = getUserById(id);

        if(!user.canChangeUsername()){
            throw new BadActionException("User can't change username");
        }

        user.setUsername(username);
        user.setUsernameLastEdited(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(UUID requesterId, UUID targetUserId){
        if(!requesterId.equals(targetUserId)){
            throw new UserUnauthorizedException("A normal user tried to delete another user");
        }

        User user = getUserById(targetUserId);

        if(user.getStatus().equals(UserStatus.DELETED)){
            throw new BadActionException("User already deleted");
        }

        user.setIsDeleted();
        userRepository.save(user);
    }
}
