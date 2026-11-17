package com.motomutterers.boardgames.skullking.services;

import com.motomutterers.boardgames.exceptions.BadActionException;
import com.motomutterers.boardgames.sessions.models.session.Session;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEvent;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEventPayload;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEventType;
import com.motomutterers.boardgames.skullking.events.SkullKingStateChangedEvent;
import com.motomutterers.boardgames.teams.models.Team;
import com.motomutterers.boardgames.user.model.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * resetRound discards the CURRENT round's phase events (and their team rows) and restarts
 * the round at BIDS with the same round number, card count and leader. Previous rounds are
 * never touched.
 */
public class SkullKingResetRoundTest extends SkullKingTestSupport {

    private Session adminSession(User admin, Authentication authentication, Team... teams) {
        Session session = session(false, teams);
        when(userService.getAuthenticatedUser(authentication)).thenReturn(admin);
        when(roomsUtilityService.getRoomByName("room")).thenReturn(session.getRoom());
        when(sessionUtilitysService.getOrThrowSessionByRoom(session.getRoom())).thenReturn(session);
        return session;
    }

    @Test
    @SuppressWarnings("unchecked")
    void resetRound_fromTrickResults_deletesRoundEventsAndRecreatesBids() {
        User admin = user();
        Authentication authentication = auth();
        Team a = team(0);
        Team b = team(1);
        Session session = adminSession(admin, authentication, a, b);

        SessionEvent bids = bidsEvent(3, 3, id(b));
        SessionEvent inProgress = inProgressEvent(3, 3, id(b));
        SessionEvent tricks = tricksEvent(3, 3, id(b), true);
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(tricks);
        stubAllEvents(session, bids, inProgress, tricks);

        service.resetRound("room", authentication);

        ArgumentCaptor<List<SessionEvent>> deleted = ArgumentCaptor.forClass(List.class);
        verify(sessionEventService).deleteEvents(deleted.capture());
        assertEquals(List.of(bids, inProgress, tricks), deleted.getValue());

        ArgumentCaptor<SessionEventPayload> captor = ArgumentCaptor.forClass(SessionEventPayload.class);
        verify(sessionEventService).createSessionEvent(eq(session), eq(SessionEventType.BIDS), captor.capture());
        assertEquals(new SessionEventPayload.Bids(3, 3, id(b)), captor.getValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    void resetRound_fromBonusPoints_deletesOnlyTheCurrentRound() {
        User admin = user();
        Authentication authentication = auth();
        Team a = team(0);
        Team b = team(1);
        Session session = adminSession(admin, authentication, a, b);

        // Round 1 is finished; round 2 is in its bonus phase.
        SessionEvent bids1 = bidsEvent(1, 1, id(a));
        SessionEvent inProgress1 = inProgressEvent(1, 1, id(a));
        SessionEvent tricks1 = tricksEvent(1, 1, id(a), false);
        SessionEvent bonus1 = bonusEvent(1, 1, id(a));
        SessionEvent bids2 = bidsEvent(2, 2, id(b));
        SessionEvent inProgress2 = inProgressEvent(2, 2, id(b));
        SessionEvent tricks2 = tricksEvent(2, 2, id(b), false);
        SessionEvent bonus2 = bonusEvent(2, 2, id(b));
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(bonus2);
        stubAllEvents(session, bids1, inProgress1, tricks1, bonus1, bids2, inProgress2, tricks2, bonus2);

        service.resetRound("room", authentication);

        ArgumentCaptor<List<SessionEvent>> deleted = ArgumentCaptor.forClass(List.class);
        verify(sessionEventService).deleteEvents(deleted.capture());
        assertEquals(List.of(bids2, inProgress2, tricks2, bonus2), deleted.getValue());

        ArgumentCaptor<SessionEventPayload> captor = ArgumentCaptor.forClass(SessionEventPayload.class);
        verify(sessionEventService).createSessionEvent(eq(session), eq(SessionEventType.BIDS), captor.capture());
        assertEquals(new SessionEventPayload.Bids(2, 2, id(b)), captor.getValue());
    }

    @Test
    void resetRound_nonAdmin_throws() {
        User user = user();
        Authentication authentication = auth();
        Session session = session(false, team(0));
        when(userService.getAuthenticatedUser(authentication)).thenReturn(user);
        when(roomsUtilityService.getRoomByName("room")).thenReturn(session.getRoom());
        doThrow(new BadActionException("not admin"))
            .when(roomsUtilityService).throwIfUserIsNotRoomAdmin(session.getRoom(), user);

        assertThrows(BadActionException.class, () -> service.resetRound("room", authentication));
        verify(sessionEventService, never()).deleteEvents(any());
        verify(sessionEventService, never()).createSessionEvent(any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void resetRound_publishesStateChanged() {
        User admin = user();
        Authentication authentication = auth();
        Team a = team(0);
        Session session = adminSession(admin, authentication, a);

        SessionEvent bids = bidsEvent(1, 1, id(a));
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(bids);
        stubAllEvents(session, bids);

        service.resetRound("room", authentication);

        // Resetting a fresh BIDS phase is allowed: it just clears the entered bids.
        verify(sessionEventService).deleteEvents(List.of(bids));
        verify(eventPublisher).publishEvent(any(SkullKingStateChangedEvent.class));
    }

    private User user() {
        User u = new User("admin@test.com", "admin", "hash");
        ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
        return u;
    }
}
