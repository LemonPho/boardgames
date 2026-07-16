package com.motomutterers.boardgames.user.services;

import com.motomutterers.boardgames.auth.exceptions.UserUnauthorizedException;
import com.motomutterers.boardgames.exceptions.BadActionException;
import com.motomutterers.boardgames.user.UserRepository;
import com.motomutterers.boardgames.user.dto.UserResponse;
import com.motomutterers.boardgames.user.exceptions.UserNotFoundException;
import com.motomutterers.boardgames.user.model.User;
import com.motomutterers.boardgames.user.model.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    // helpers
    private User user() {
        return new User("test@test.com", "testuser", "hash");
    }

    // --- getUserById ---

    @Test
    void getUserById_exists_returnsUser() {
        UUID id = UUID.randomUUID();
        User user = user();
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        assertEquals(user, userService.getUserById(id));
    }

    @Test
    void getUserById_missing_throws() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserById(id));
    }

    // --- getUserByUsername ---

    @Test
    void getUserByUsername_exists_returnsUser() {
        User user = user();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        assertEquals(user, userService.getUserByUsername("testuser"));
    }

    @Test
    void getUserByUsername_missing_throws() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserByUsername("ghost"));
    }

    // --- getAuthenticatedUser ---

    @Test
    void getAuthenticatedUser_resolvesFromAuthName() {
        UUID id = UUID.randomUUID();
        User user = user();
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(id.toString());
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        assertEquals(user, userService.getAuthenticatedUser(auth));
    }

    @Test
    void getAuthenticatedUserResponse_returnsResponseForAuthUser() {
        UUID id = UUID.randomUUID();
        User user = user();
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(id.toString());
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        UserResponse response = userService.getAuthenticatedUserResponse(auth);

        assertEquals("testuser", response.getUsername());
        assertEquals("test@test.com", response.getEmail());
    }

    // --- findAllUserContainingUsername / matchAllByUsername ---

    @Test
    void findAllUserContainingUsername_delegatesToRepository() {
        List<User> users = List.of(user());
        when(userRepository.findByUsernameContainingIgnoreCase("test")).thenReturn(users);

        assertEquals(users, userService.findAllUserContainingUsername("test"));
    }

    @Test
    void matchAllByUsername_mapsToResponses() {
        when(userRepository.findByUsernameContainingIgnoreCase("test"))
            .thenReturn(List.of(user()));

        List<UserResponse> result = userService.matchAllByUsername("test");

        assertEquals(1, result.size());
        assertEquals("testuser", result.get(0).getUsername());
    }

    // --- updateUsername ---

    @Test
    void updateUsername_cannotChange_throwsBadAction() {
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(user.canChangeUsername()).thenReturn(false);

        assertThrows(BadActionException.class, () -> userService.updateUsername(userId, "newname"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUsername_allowed_setsNameTimestampAndSaves() {
        UUID userId = UUID.randomUUID();
        User user = user();  // fresh user: usernameLastEdited null -> canChangeUsername true
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.updateUsername(userId, "newname");

        assertEquals("newname", user.getUsername());
        assertNotNull(user.getUsernameLastEdited());
        verify(userRepository).save(user);
    }

    // --- deleteUser ---

    @Test
    void deleteUser_requesterIsNotOwner_throwsUnauthorized() {
        UUID requesterId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        assertThrows(UserUnauthorizedException.class, () -> userService.deleteUser(requesterId, targetId));
        verify(userRepository, never()).findById(any());
    }

    @Test
    void deleteUser_alreadyDeleted_throwsBadAction() {
        UUID userId = UUID.randomUUID();
        User user = user();
        user.setIsDeleted();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(BadActionException.class, () -> userService.deleteUser(userId, userId));
        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteUser_valid_setsDeletedAndSaves() {
        UUID userId = UUID.randomUUID();
        User user = user();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.deleteUser(userId, userId);

        assertEquals(UserStatus.DELETED, user.getStatus());
        verify(userRepository).save(user);
    }
}
