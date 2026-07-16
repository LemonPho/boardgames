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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserService userService;

    @InjectMocks
    private AdminService adminService;

    // helpers
    private User plainUser() {
        return new User("test@test.com", "testuser", "hash");
    }

    // --- sameUserGuard ---

    @Test
    void sameUserGuard_sameIds_throwsBadAction() {
        UUID id = UUID.randomUUID();
        assertThrows(BadActionException.class, () -> adminService.sameUserGuard(id, id));
    }

    @Test
    void sameUserGuard_differentIds_passes() {
        assertDoesNotThrow(() -> adminService.sameUserGuard(UUID.randomUUID(), UUID.randomUUID()));
    }

    // --- setIsActive ---

    @Test
    void setIsActive_sameUser_throwsBadAction() {
        UUID id = UUID.randomUUID();
        assertThrows(BadActionException.class, () -> adminService.setIsActive(id, id));
        verify(userRepository, never()).save(any());
    }

    @Test
    void setIsActive_valid_activatesAndSaves() {
        UUID adminId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        User target = plainUser();
        when(userService.getUserById(targetId)).thenReturn(target);

        adminService.setIsActive(adminId, targetId);

        assertEquals(UserStatus.ACTIVE, target.getStatus());
        verify(userRepository).save(target);
    }

    // --- banUser ---

    @Test
    void banUser_sameUser_throwsBadAction() {
        UUID id = UUID.randomUUID();
        assertThrows(BadActionException.class, () -> adminService.banUser(id, id));
    }

    @Test
    void banUser_targetIsAdmin_throwsUnauthorized() {
        UUID adminId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        User target = mock(User.class);
        when(userService.getUserById(targetId)).thenReturn(target);
        when(target.isAdmin()).thenReturn(true);

        assertThrows(UserUnauthorizedException.class, () -> adminService.banUser(adminId, targetId));
    }

    @Test
    void banUser_targetIsDeleted_throwsBadAction() {
        UUID adminId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        User target = mock(User.class);
        when(userService.getUserById(targetId)).thenReturn(target);
        when(target.isAdmin()).thenReturn(false);
        when(target.isDeleted()).thenReturn(true);

        assertThrows(BadActionException.class, () -> adminService.banUser(adminId, targetId));
    }

    @Test
    void banUser_alreadyBanned_throwsBadAction() {
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
    void banUser_valid_setsBannedAndSaves() {
        UUID adminId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        User target = plainUser();
        when(userService.getUserById(targetId)).thenReturn(target);

        adminService.banUser(adminId, targetId);

        assertEquals(UserStatus.BANNED, target.getStatus());
        verify(userRepository).save(target);
    }

    // --- temporarilyBanUser ---

    @Test
    void temporarilyBanUser_targetIsAdmin_throwsUnauthorized() {
        UUID adminId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        User target = mock(User.class);
        when(userService.getUserById(targetId)).thenReturn(target);
        when(target.isAdmin()).thenReturn(true);

        assertThrows(UserUnauthorizedException.class, () -> adminService.temporarilyBanUser(adminId, targetId));
    }

    @Test
    void temporarilyBanUser_alreadyBanned_throwsBadAction() {
        UUID adminId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        User target = mock(User.class);
        when(userService.getUserById(targetId)).thenReturn(target);
        when(target.isAdmin()).thenReturn(false);
        when(target.isTemporarilyBanned()).thenReturn(true);

        assertThrows(BadActionException.class, () -> adminService.temporarilyBanUser(adminId, targetId));
    }

    @Test
    void temporarilyBanUser_valid_setsTempBannedAndSaves() {
        UUID adminId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        User target = plainUser();
        when(userService.getUserById(targetId)).thenReturn(target);

        adminService.temporarilyBanUser(adminId, targetId);

        assertEquals(UserStatus.TEMPORARILY_BANNED, target.getStatus());
        verify(userRepository).save(target);
    }

    // --- deleteUser ---

    @Test
    void deleteUser_alreadyDeleted_throwsBadAction() {
        UUID adminId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        User target = mock(User.class);
        when(userService.getUserById(targetId)).thenReturn(target);
        when(target.isDeleted()).thenReturn(true);

        assertThrows(BadActionException.class, () -> adminService.deleteUser(adminId, targetId));
    }

    @Test
    void deleteUser_targetIsAdmin_throwsBadAction() {
        UUID adminId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        User target = mock(User.class);
        when(userService.getUserById(targetId)).thenReturn(target);
        when(target.isDeleted()).thenReturn(false);
        when(target.isAdmin()).thenReturn(true);

        assertThrows(BadActionException.class, () -> adminService.deleteUser(adminId, targetId));
    }

    @Test
    void deleteUser_valid_setsDeletedAndSaves() {
        UUID adminId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        User target = plainUser();
        when(userService.getUserById(targetId)).thenReturn(target);

        adminService.deleteUser(adminId, targetId);

        assertEquals(UserStatus.DELETED, target.getStatus());
        verify(userRepository).save(target);
    }
}
