package com.motomutterers.boardgames.user.services;

import com.motomutterers.boardgames.auth.exceptions.UserUnauthorizedException;
import com.motomutterers.boardgames.exceptions.BadActionException;
import com.motomutterers.boardgames.user.UserRepository;
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
public class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private AdminService adminService;

    @Test
    void sameUserGuard_sameIds_throwsBadActionException() {
        UUID id = UUID.randomUUID();
        assertThrows(BadActionException.class, () -> adminService.sameUserGuard(id, id));
    }

    @Test
    void banUser_targetIsAdmin_throwsUnauthorizedException() {
        UUID adminId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        User target = mock(User.class);
        when(userService.getUserById(targetId)).thenReturn(target);
        when(target.isAdmin()).thenReturn(true);

        assertThrows(UserUnauthorizedException.class, () -> adminService.banUser(adminId, targetId));
    }

    @Test
    void banUser_userAlreadyBanned_throwsBadActionException() {
        UUID adminId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        User target = mock(User.class);
        when(userService.getUserById(targetId)).thenReturn(target);
        when(target.isAdmin()).thenReturn(false);
        when(target.isDeleted()).thenReturn(false);
        when(target.isBanned()).thenReturn(true);

        assertThrows(BadActionException.class, () -> adminService.banUser(adminId, targetId));
    }

    @Test
    void banUser_validRequest_setsUserBanned() {
        UUID adminId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        User target = new User("test@test.com", "testuser", "hash");
        when(userService.getUserById(targetId)).thenReturn(target);

        adminService.banUser(adminId, targetId);

        assertEquals(UserStatus.BANNED, target.getStatus());
        verify(userRepository).save(target);
    }

    @Test
    void deleteUser_targetIsAdmin_throwsBadActionException() {
        UUID adminId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        User target = mock(User.class);
        when(userService.getUserById(targetId)).thenReturn(target);
        when(target.isDeleted()).thenReturn(false);
        when(target.isAdmin()).thenReturn(true);

        assertThrows(BadActionException.class, () -> adminService.deleteUser(adminId, targetId));
    }

    @Test
    void temporarilyBanUser_alreadyBanned_throwsBadActionException() {
        UUID adminId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        User target = mock(User.class);
        when(userService.getUserById(targetId)).thenReturn(target);
        when(target.isAdmin()).thenReturn(false);
        when(target.isTemporarilyBanned()).thenReturn(true);

        assertThrows(BadActionException.class, () -> adminService.temporarilyBanUser(adminId, targetId));
    }
}