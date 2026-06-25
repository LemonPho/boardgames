package com.motomutterers.boardgames.user.services;

import com.motomutterers.boardgames.exceptions.BadActionException;
import com.motomutterers.boardgames.user.UserRepository;
import com.motomutterers.boardgames.user.exceptions.UserNotFoundException;
import com.motomutterers.boardgames.user.model.User;
import com.motomutterers.boardgames.user.model.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getUserById_userExists_returnsUser() {
        UUID id = UUID.randomUUID();
        User user = new User("test@test.com", "testuser", "hash");
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        User result = userService.getUserById(id);

        assertEquals(user, result);
    }

    @Test
    void getUserById_userNotFound_throwsUserNotFoundException() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserById(id));
    }

    @Test
    void deleteUser_requesterIsNotOwner_throwsUnauthorizedException() {
        UUID requesterId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        assertThrows(Exception.class, () -> userService.deleteUser(requesterId, targetId));
    }

    @Test
    void deleteUser_userAlreadyDeleted_throwsBadActionException() {
        UUID userId = UUID.randomUUID();
        User user = new User("test@test.com", "testuser", "hash");
        user.setIsDeleted();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(BadActionException.class, () -> userService.deleteUser(userId, userId));
    }

    @Test
    void deleteUser_validRequest_setsUserDeleted() {
        UUID userId = UUID.randomUUID();
        User user = new User("test@test.com", "testuser", "hash");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.deleteUser(userId, userId);

        assertEquals(UserStatus.DELETED, user.getStatus());
        verify(userRepository).save(user);
    }

    @Test
    void updateUsername_cannotChange_throwsBadActionException() {
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(user.canChangeUsername()).thenReturn(false);

        assertThrows(BadActionException.class, () -> userService.updateUsername(userId, "newname"));
    }
}