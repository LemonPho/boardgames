package com.motomutterers.boardgames.skullking.services;

import com.motomutterers.boardgames.exceptions.BadActionException;
import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.rooms.model.Room.RoomStatus;
import com.motomutterers.boardgames.sessions.models.session.Session;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEvent;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEventPayload;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEventType;
import com.motomutterers.boardgames.teams.models.Team;
import com.motomutterers.boardgames.user.model.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Chunk 2 — round setup and phase progression. Covers the card-count formula,
 * starting-team selection/rotation, and the BIDS → IN_PROGRESS → TRICK_RESULTS
 * → BONUS_POINTS → (next round | complete) transition guards.
 */
public class SkullKingProgressionTest extends SkullKingTestSupport {

    // helper: wire the common auth/room/session lookups an admin action performs.
    private Session adminSession(Room room, User admin, Authentication authentication, Team... teams) {
        Session session = session(false, teams);
        // room is created inside session(); align the lookups to it.
        when(userService.getAuthenticatedUser(authentication)).thenReturn(admin);
        when(roomsUtilityService.getRoomByName("room")).thenReturn(session.getRoom());
        when(sessionUtilitysService.getOrThrowSessionByRoom(session.getRoom())).thenReturn(session);
        return session;
    }

    private int cardCountForRound(Session session, int round) {
        return (int) ReflectionTestUtils.invokeMethod(service, "cardCountForRound", session, round);
    }

    private String nextStarter(Session session, String current) {
        return (String) ReflectionTestUtils.invokeMethod(service, "nextRoundStartingTeamId", session, current);
    }

    // --- createInitialRound ---

    @Test
    void createInitialRound_createsBidsEventForRoundOneWithAStartingTeam() {
        Team a = team();
        Team b = team();
        Session session = session(false, a, b);

        service.createInitialRound(session);

        ArgumentCaptor<SessionEventPayload> payloadCaptor = ArgumentCaptor.forClass(SessionEventPayload.class);
        verify(sessionEventService).createSessionEvent(eq(session), eq(SessionEventType.BIDS), payloadCaptor.capture());

        SessionEventPayload.Bids bids = (SessionEventPayload.Bids) payloadCaptor.getValue();
        assertEquals(1, bids.round());
        assertEquals(1, bids.cardCount());  // round 1 -> 1 card
        // The leader is one of the session's teams, chosen at random.
        Set<String> teamIds = Set.of(a.getId().toString(), b.getId().toString());
        assertTrue(teamIds.contains(bids.startingTeamId()));
    }

    // --- cardCountForRound ---

    @Test
    void cardCountForRound_earlyRounds_equalRoundNumber() {
        Session session = session(false, team(), team(), team());  // 3 players
        assertEquals(1, cardCountForRound(session, 1));
        assertEquals(5, cardCountForRound(session, 5));
    }

    @Test
    void cardCountForRound_cappedByDeckWhenTableIsLarge() {
        // 8 players, base deck 70 -> 70/8 = 8 cards max. Round 10 is capped to 8.
        Team[] teams = new Team[8];
        for (int i = 0; i < 8; i++) teams[i] = team();
        Session session = session(false, teams);

        assertEquals(8, cardCountForRound(session, 10));
        assertEquals(7, cardCountForRound(session, 7));  // below the cap, so unchanged
    }

    @Test
    void cardCountForRound_advancedDeckIsLarger() {
        // 8 players, advanced deck 74 -> 74/8 = 9 cards max, so round 10 caps at 9.
        Team[] teams = new Team[8];
        for (int i = 0; i < 8; i++) teams[i] = team();
        Session session = session(true, teams);

        assertEquals(9, cardCountForRound(session, 10));
    }

    // --- nextRoundStartingTeamId ---

    @Test
    void nextStartingTeam_rotatesToNextSeatByJoinOrder() {
        Team first = team(LocalDateTime.now().minusMinutes(3));
        Team second = team(LocalDateTime.now().minusMinutes(2));
        Team third = team(LocalDateTime.now().minusMinutes(1));
        Session session = session(false, first, second, third);

        assertEquals(second.getId().toString(), nextStarter(session, first.getId().toString()));
        assertEquals(third.getId().toString(), nextStarter(session, second.getId().toString()));
    }

    @Test
    void nextStartingTeam_wrapsAroundToFirstSeat() {
        Team first = team(LocalDateTime.now().minusMinutes(2));
        Team second = team(LocalDateTime.now().minusMinutes(1));
        Session session = session(false, first, second);

        assertEquals(first.getId().toString(), nextStarter(session, second.getId().toString()));
    }

    // --- startRound (BIDS -> IN_PROGRESS) ---

    @Test
    void startRound_allBidsIn_createsInProgressEvent() {
        User admin = user();
        Authentication authentication = auth();
        Team a = team();
        Team b = team();
        Session session = adminSession(null, admin, authentication, a, b);

        SessionEvent bids = bidsEvent(1, 1, id(a));
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(bids);
        when(teamSessionEventRepository.countBySessionEvent(bids)).thenReturn(2);

        service.startRound("room", authentication);

        verify(sessionEventService).createSessionEvent(eq(session), eq(SessionEventType.IN_PROGRESS), any());
    }

    @Test
    void startRound_notAllBidsIn_throws() {
        User admin = user();
        Authentication authentication = auth();
        Team a = team();
        Team b = team();
        Session session = adminSession(null, admin, authentication, a, b);

        SessionEvent bids = bidsEvent(1, 1, id(a));
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(bids);
        when(teamSessionEventRepository.countBySessionEvent(bids)).thenReturn(1);  // only 1 of 2

        assertThrows(BadActionException.class, () -> service.startRound("room", authentication));
        verify(sessionEventService, never()).createSessionEvent(any(), eq(SessionEventType.IN_PROGRESS), any());
    }

    @Test
    void startRound_wrongPhase_throws() {
        User admin = user();
        Authentication authentication = auth();
        Session session = adminSession(null, admin, authentication, team());

        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(inProgressEvent(1, 1, "x"));

        assertThrows(BadActionException.class, () -> service.startRound("room", authentication));
    }

    // --- startTrickResults (IN_PROGRESS -> TRICK_RESULTS) ---

    @Test
    void startTrickResults_fromInProgress_createsTrickResultsEvent() {
        User admin = user();
        Authentication authentication = auth();
        Session session = adminSession(null, admin, authentication, team());

        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(inProgressEvent(1, 1, "x"));

        service.startTrickResults("room", authentication);

        verify(sessionEventService).createSessionEvent(eq(session), eq(SessionEventType.TRICK_RESULTS), any());
    }

    @Test
    void startTrickResults_wrongPhase_throws() {
        User admin = user();
        Authentication authentication = auth();
        Session session = adminSession(null, admin, authentication, team());

        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(bidsEvent(1, 1, "x"));

        assertThrows(BadActionException.class, () -> service.startTrickResults("room", authentication));
    }

    // --- startBonusPoints (TRICK_RESULTS -> BONUS_POINTS) ---

    @Test
    void startBonusPoints_tricksSumCorrect_createsBonusEvent() {
        User admin = user();
        Authentication authentication = auth();
        Team a = team();
        Team b = team();
        Session session = adminSession(null, admin, authentication, a, b);

        SessionEvent tricks = tricksEvent(1, 3, id(a), false);
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(tricks);
        when(teamSessionEventRepository.countBySessionEvent(tricks)).thenReturn(2);
        // Tricks sum to the 3 cards dealt.
        stubTeamEvents(tricks, teamTricks(a, 1), teamTricks(b, 2));

        service.startBonusPoints("room", authentication);

        verify(sessionEventService).createSessionEvent(eq(session), eq(SessionEventType.BONUS_POINTS), any());
    }

    @Test
    void startBonusPoints_tricksSumWrong_throws() {
        User admin = user();
        Authentication authentication = auth();
        Team a = team();
        Team b = team();
        Session session = adminSession(null, admin, authentication, a, b);

        SessionEvent tricks = tricksEvent(1, 3, id(a), false);
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(tricks);
        when(teamSessionEventRepository.countBySessionEvent(tricks)).thenReturn(2);
        // Only 2 tricks recorded but 3 cards dealt.
        stubTeamEvents(tricks, teamTricks(a, 1), teamTricks(b, 1));

        assertThrows(BadActionException.class, () -> service.startBonusPoints("room", authentication));
        verify(sessionEventService, never()).createSessionEvent(any(), eq(SessionEventType.BONUS_POINTS), any());
    }

    @Test
    void startBonusPoints_krakenReducesExpectedTotal() {
        User admin = user();
        Authentication authentication = auth();
        Team a = team();
        Team b = team();
        Session session = adminSession(null, admin, authentication, a, b);

        // Kraken destroyed a trick: 3 cards -> tricks must sum to 2.
        SessionEvent tricks = tricksEvent(1, 3, id(a), true);
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(tricks);
        when(teamSessionEventRepository.countBySessionEvent(tricks)).thenReturn(2);
        stubTeamEvents(tricks, teamTricks(a, 1), teamTricks(b, 1));

        service.startBonusPoints("room", authentication);

        verify(sessionEventService).createSessionEvent(eq(session), eq(SessionEventType.BONUS_POINTS), any());
    }

    // --- finishRound (BONUS_POINTS -> next round | complete) ---

    @Test
    void finishRound_midGame_createsNextRoundBids() {
        User admin = user();
        Authentication authentication = auth();
        Team a = team(LocalDateTime.now().minusMinutes(2));
        Team b = team(LocalDateTime.now().minusMinutes(1));
        Session session = adminSession(null, admin, authentication, a, b);

        SessionEvent bonus = bonusEvent(1, 1, id(a));
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(bonus);
        when(teamSessionEventRepository.countBySessionEvent(bonus)).thenReturn(2);
        stubTeamEvents(bonus, teamBonus(a, bonus(0, false, 0, 0, false, 0)), teamBonus(b, bonus(0, false, 0, 0, false, 0)));
        // recomputeScores reads all events; give it just this round's bids+tricks.
        stubAllEvents(session, bidsEvent(1, 1, id(a)), tricksEvent(1, 1, id(a), false));

        service.finishRound("room", authentication);

        ArgumentCaptor<SessionEventPayload> captor = ArgumentCaptor.forClass(SessionEventPayload.class);
        verify(sessionEventService).createSessionEvent(eq(session), eq(SessionEventType.BIDS), captor.capture());
        SessionEventPayload.Bids next = (SessionEventPayload.Bids) captor.getValue();
        assertEquals(2, next.round());
        // Leader rotates to the next seat.
        assertEquals(b.getId().toString(), next.startingTeamId());
        verify(sessionUtilitysService, never()).completeSession(any());
    }

    @Test
    void finishRound_finalRound_completesSession() {
        User admin = user();
        Authentication authentication = auth();
        Team a = team();
        Session session = adminSession(null, admin, authentication, a);

        SessionEvent bonus = bonusEvent(10, 8, id(a));  // round 10 = MAX_ROUNDS
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(bonus);
        when(teamSessionEventRepository.countBySessionEvent(bonus)).thenReturn(1);
        stubTeamEvents(bonus, teamBonus(a, bonus(0, false, 0, 0, false, 0)));
        stubAllEvents(session, bidsEvent(10, 8, id(a)), tricksEvent(10, 8, id(a), false));

        service.finishRound("room", authentication);

        verify(sessionUtilitysService).completeSession(session);
        verify(roomsUtilityService).changeRoomStatus(session.getRoom(), RoomStatus.COMPLETED);
        verify(sessionEventService, never()).createSessionEvent(any(), eq(SessionEventType.BIDS), any());
    }

    @Test
    void finishRound_notAllBonusesIn_throws() {
        User admin = user();
        Authentication authentication = auth();
        Team a = team();
        Team b = team();
        Session session = adminSession(null, admin, authentication, a, b);

        SessionEvent bonus = bonusEvent(1, 1, id(a));
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(bonus);
        when(teamSessionEventRepository.countBySessionEvent(bonus)).thenReturn(1);  // 1 of 2

        assertThrows(BadActionException.class, () -> service.finishRound("room", authentication));
        verify(sessionUtilitysService, never()).completeSession(any());
    }

    @Test
    void finishRound_wrongPhase_throws() {
        User admin = user();
        Authentication authentication = auth();
        Session session = adminSession(null, admin, authentication, team());

        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(bidsEvent(1, 1, "x"));

        assertThrows(BadActionException.class, () -> service.finishRound("room", authentication));
    }

    // helper
    private User user() {
        User u = new User("admin@test.com", "admin", "hash");
        ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
        return u;
    }
}
