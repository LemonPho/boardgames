package com.motomutterers.boardgames.user.services;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.motomutterers.boardgames.auth.exceptions.UserUnauthorizedException;
import com.motomutterers.boardgames.exceptions.BadActionException;
import com.motomutterers.boardgames.user.UserRepository;
import com.motomutterers.boardgames.user.model.User;

import jakarta.transaction.Transactional;


@Service
public class AdminService {
    private final UserRepository userRepository;
    private final UserService userService;

    public AdminService(
        UserRepository userRepository,
        UserService userService
    ){
        this.userRepository = userRepository;
        this.userService = userService;
    }

    public void sameUserGuard(UUID adminId, UUID targetUserId){
        if(adminId.equals(targetUserId)){
            throw new BadActionException("Admin can't update himself");
        }
    }

    @Transactional
    public void setIsActive(UUID adminId, UUID targetUserId){
        sameUserGuard(adminId, targetUserId);

        User user = userService.getUserById(targetUserId);
        user.setIsActive();
        userRepository.save(user);
    }

    @Transactional
    public void temporarilyBanUser(UUID adminId, UUID targetUserId){
        sameUserGuard(adminId, targetUserId);

        User user = userService.getUserById(targetUserId);
        if(user.isAdmin()){
            throw new UserUnauthorizedException("Admin cannot ban another admin");
        }

        if(user.isTemporarilyBanned()){
            throw new BadActionException("User is already temporarily banned");
        }

        user.setIsTemporarilyBanned();
        userRepository.save(user);
    }

    @Transactional
    public void banUser(UUID adminId, UUID targetUserId){
        sameUserGuard(adminId, targetUserId);

        User user = userService.getUserById(targetUserId);
        if(user.isAdmin()){
            throw new UserUnauthorizedException("Admin cannot ban another admin");
        }

        if(user.isDeleted()){
            throw new BadActionException("User is deleted");
        }

        if(user.isBanned()){
            throw new BadActionException("User is already banned");
        }

        user.setIsBanned();
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(UUID adminId, UUID targetUserId){
        sameUserGuard(adminId, targetUserId);

        User user = userService.getUserById(targetUserId);
        if(user.isDeleted()){
            throw new BadActionException("User is already deleted");
        }

        if(user.isAdmin()){
            throw new BadActionException("An admin cannot delete another admin");
        }

        user.setIsDeleted();
        userRepository.save(user);
    }
}
