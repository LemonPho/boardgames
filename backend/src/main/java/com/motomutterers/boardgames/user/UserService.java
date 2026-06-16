package com.motomutterers.boardgames.user;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;


public class UserService {
    private final UserRepository userRepository;

    public UserService(
        UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    public Optional<User> getUserById(UUID id){
        return userRepository.findById(id);
    }

    public Optional<User> getUserByUsername(String username){
        return userRepository.findByUsername(username);
    }

    @Transactional
    public void updateUsername(User user, String username){
        user.setUsername(username);
    }
    
}
