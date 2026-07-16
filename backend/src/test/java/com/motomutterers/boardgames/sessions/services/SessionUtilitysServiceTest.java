package com.motomutterers.boardgames.sessions.services;

import com.motomutterers.boardgames.exceptions.UnauthorizedException;
import com.motomutterers.boardgames.games.model.Game;
import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.rooms.model.Room.RoomConfiguration;
import com.motomutterers.boardgames.rooms.model.Room.RoomUser;
import com.motomutterers.boardgames.rooms.model.Room.RoomUserRoles;
import com.motomutterers.boardgames.rooms.model.Room.TrackingMode;
import com.motomutterers.boardgames.sessions.exceptions.SessionExistsException;
import com.motomutterers.boardgames.sessions.exceptions.SessionNotFoundException;
import com.motomutterers.boardgames.sessions.models.session.Session;
import com.motomutterers.boardgames.sessions.models.session.SessionStatus;
import com.motomutterers.boardgames.sessions.repositories.SessionRepository;
import com.motomutterers.boardgames.user.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SessionUtilitysServiceTest {

    @Mock private SessionRepository sessionRepository;

    @InjectMocks
    private SessionUtilitysService sessionUtilitysService;

    // helpers
    private Room room() {
        Game game = new Game();
        game.setName("Skull King");
        return new Room(game, "room", new RoomConfiguration(TrackingMode.ADMIN, false));
    }

    private RoomUser roomUser(RoomUserRoles role) {
        User user = new User("test@test.com", "testuser", "hash");
        return new RoomUser(user, room(), role);
    }

    // --- throwIfSessionExistsInRoom ---

    @Test
    void throwIfSessionExistsInRoom_exists_throws() {
        Room room = room();
        when(sessionRepository.findByRoomAndStatus(room, SessionStatus.IN_PROGRESS))
            .thenReturn(Optional.of(new Session(room)));

        assertThrows(SessionExistsException.class,
            () -> sessionUtilitysService.throwIfSessionExistsInRoom(room));
    }

    @Test
    void throwIfSessionExistsInRoom_none_passes() {
        Room room = room();
        when(sessionRepository.findByRoomAndStatus(room, SessionStatus.IN_PROGRESS))
            .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> sessionUtilitysService.throwIfSessionExistsInRoom(room));
    }

    // --- getOrThrowSessionById ---

    @Test
    void getOrThrowSessionById_exists_returns() {
        UUID id = UUID.randomUUID();
        Session session = new Session(room());
        when(sessionRepository.findById(id)).thenReturn(Optional.of(session));

        assertEquals(session, sessionUtilitysService.getOrThrowSessionById(id.toString()));
    }

    @Test
    void getOrThrowSessionById_missing_throws() {
        UUID id = UUID.randomUUID();
        when(sessionRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(SessionNotFoundException.class,
            () -> sessionUtilitysService.getOrThrowSessionById(id.toString()));
    }

    // --- cancelSession ---

    @Test
    void cancelSession_setsCancelledAndSaves() {
        Session session = new Session(room());

        sessionUtilitysService.cancelSession(session);

        assertEquals(SessionStatus.CANCELLED, session.getStatus());
        verify(sessionRepository).save(session);
    }

    // --- completeSession ---

    @Test
    void completeSession_setsCompletedAndSaves() {
        Session session = new Session(room());

        sessionUtilitysService.completeSession(session);

        assertEquals(SessionStatus.COMPLETED, session.getStatus());
        verify(sessionRepository).save(session);
    }

    // --- cancelSessionIfExists ---

    @Test
    void cancelSessionIfExists_present_cancels() {
        Room room = room();
        Session session = new Session(room);
        when(sessionRepository.findByRoomAndStatus(room, SessionStatus.IN_PROGRESS))
            .thenReturn(Optional.of(session));

        sessionUtilitysService.cancelSessionIfExists(room);

        assertEquals(SessionStatus.CANCELLED, session.getStatus());
        verify(sessionRepository).save(session);
    }

    @Test
    void cancelSessionIfExists_absent_doesNothing() {
        Room room = room();
        when(sessionRepository.findByRoomAndStatus(room, SessionStatus.IN_PROGRESS))
            .thenReturn(Optional.empty());

        sessionUtilitysService.cancelSessionIfExists(room);

        verify(sessionRepository, never()).save(any());
    }

    // --- getOrThrowSessionByRoom ---

    @Test
    void getOrThrowSessionByRoom_exists_returns() {
        Room room = room();
        Session session = new Session(room);
        when(sessionRepository.findByRoomAndStatus(room, SessionStatus.IN_PROGRESS))
            .thenReturn(Optional.of(session));

        assertEquals(session, sessionUtilitysService.getOrThrowSessionByRoom(room));
    }

    @Test
    void getOrThrowSessionByRoom_missing_throws() {
        Room room = room();
        when(sessionRepository.findByRoomAndStatus(room, SessionStatus.IN_PROGRESS))
            .thenReturn(Optional.empty());

        assertThrows(SessionNotFoundException.class,
            () -> sessionUtilitysService.getOrThrowSessionByRoom(room));
    }

    // --- throwIfUserIsntRoomAdmin ---

    @Test
    void throwIfUserIsntRoomAdmin_admin_passes() {
        assertDoesNotThrow(() -> sessionUtilitysService.throwIfUserIsntRoomAdmin(roomUser(RoomUserRoles.ADMIN)));
    }

    @Test
    void throwIfUserIsntRoomAdmin_nonAdmin_throws() {
        assertThrows(UnauthorizedException.class,
            () -> sessionUtilitysService.throwIfUserIsntRoomAdmin(roomUser(RoomUserRoles.PLAYER)));
    }
}
