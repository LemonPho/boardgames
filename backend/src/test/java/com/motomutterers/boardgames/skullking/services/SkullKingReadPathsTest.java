package com.motomutterers.boardgames.skullking.services;

import com.motomutterers.boardgames.exceptions.BadActionException;
import com.motomutterers.boardgames.rooms.model.Room.RoomUser;
import com.motomutterers.boardgames.rooms.model.Room.RoomUserRoles;
import com.motomutterers.boardgames.sessions.models.session.Session;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEvent;
import com.motomutterers.boardgames.skullking.dto.RoundHistoryResponse;
import com.motomutterers.boardgames.skullking.dto.RoundHistoryTeamResponse;
import com.motomutterers.boardgames.skullking.dto.SkullKingStateResponse;
import com.motomutterers.boardgames.teams.models.Team;
import com.motomutterers.boardgames.user.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Chunk 5 (service) — read paths. getRoundHistory (row assembly, completed /
 * kraken flags, not-started error) and getState (lookups, keep-alive, delegates
 * to the state builder).
 */
public class SkullKingReadPathsTest extends SkullKingTestSupport {

    private User user() {
        User u = new User("p@test.com", "player", "hash");
        ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
        return u;
    }

    private Session readSession(User user, Authentication authentication, RoomUser roomUser, Team... teams) {
        Session session = session(false, teams);
        when(userService.getAuthenticatedUser(authentication)).thenReturn(user);
        when(roomsUtilityService.getRoomByName("room")).thenReturn(session.getRoom());
        when(roomsUtilityService.getOrThrowRoomUserByUserAndRoom(user, session.getRoom())).thenReturn(roomUser);
        when(sessionUtilitysService.getOrThrowSessionByRoom(session.getRoom())).thenReturn(session);
        return session;
    }

    // --- getRoundHistory ---

    @Test
    void getRoundHistory_completedRound_assemblesRowsWithScores() {
        User user = user();
        Authentication authentication = auth();
        Team a = team(LocalDateTime.now().minusMinutes(2));
        Team b = team(LocalDateTime.now().minusMinutes(1));
        RoomUser roomUser = new RoomUser(user, null, RoomUserRoles.ADMIN);
        Session session = readSession(user, authentication, roomUser, a, b);

        SessionEvent bids = bidsEvent(1, 5, id(a));
        SessionEvent tricks = tricksEvent(1, 5, id(a), false);
        SessionEvent bonus = bonusEvent(1, 5, id(a));
        stubAllEvents(session, bids, tricks, bonus);

        // a: bid 2, won 2, +1 standard 14 -> 20*2 + 10 = 50.
        stubTeamEvents(bids, teamBid(a, 2), teamBid(b, 3));
        stubTeamEvents(tricks, teamTricks(a, 2), teamTricks(b, 3));
        stubTeamEvents(bonus, teamBonus(a, bonus(1, false, 0, 0, false, 0)), teamBonus(b, bonus(0, false, 0, 0, false, 0)));

        RoundHistoryResponse response = service.getRoundHistory("room", 1, authentication);

        assertEquals(1, response.getRound());
        assertEquals(5, response.getCardCount());
        assertTrue(response.getCompleted());       // bonus event present
        assertFalse(response.getKrakenPlayed());
        assertEquals(2, response.getTeams().size());

        RoundHistoryTeamResponse rowA = response.getTeams().stream()
            .filter(t -> t.getTeamId().equals(a.getId())).findFirst().orElseThrow();
        assertEquals(2, rowA.getBid());
        assertEquals(2, rowA.getTricksWon());
        assertEquals(50, rowA.getRoundScore());
    }

    @Test
    void getRoundHistory_roundNotStarted_throws() {
        User user = user();
        Authentication authentication = auth();
        Team a = team();
        RoomUser roomUser = new RoomUser(user, null, RoomUserRoles.ADMIN);
        Session session = readSession(user, authentication, roomUser, a);

        // Only round 1 exists.
        stubAllEvents(session, bidsEvent(1, 5, id(a)));

        assertThrows(BadActionException.class, () -> service.getRoundHistory("room", 2, authentication));
    }

    @Test
    void getRoundHistory_tricksNotEntered_rowsHaveNullTricksAndZeroScore() {
        User user = user();
        Authentication authentication = auth();
        Team a = team();
        RoomUser roomUser = new RoomUser(user, null, RoomUserRoles.ADMIN);
        Session session = readSession(user, authentication, roomUser, a);

        // Bids only: round in progress, not scorable.
        SessionEvent bids = bidsEvent(1, 5, id(a));
        stubAllEvents(session, bids);
        stubTeamEvents(bids, teamBid(a, 2));

        RoundHistoryResponse response = service.getRoundHistory("room", 1, authentication);

        assertFalse(response.getCompleted());
        RoundHistoryTeamResponse row = response.getTeams().get(0);
        assertEquals(2, row.getBid());
        assertNull(row.getTricksWon());
        assertEquals(0, row.getRoundScore());
    }

    @Test
    void getRoundHistory_krakenRound_flagsKraken() {
        User user = user();
        Authentication authentication = auth();
        Team a = team();
        RoomUser roomUser = new RoomUser(user, null, RoomUserRoles.ADMIN);
        Session session = readSession(user, authentication, roomUser, a);

        SessionEvent bids = bidsEvent(1, 5, id(a));
        SessionEvent tricks = tricksEvent(1, 5, id(a), true);  // kraken
        stubAllEvents(session, bids, tricks);
        stubTeamEvents(bids, teamBid(a, 2));
        stubTeamEvents(tricks, teamTricks(a, 2));

        RoundHistoryResponse response = service.getRoundHistory("room", 1, authentication);

        assertTrue(response.getKrakenPlayed());
        assertFalse(response.getCompleted());  // no bonus event yet
    }

    // --- getState ---

    @Test
    void getState_delegatesToStateBuilderAndKeepsRoomAlive() {
        User user = user();
        Authentication authentication = auth();
        Team a = team();
        RoomUser roomUser = new RoomUser(user, null, RoomUserRoles.ADMIN);
        Session session = readSession(user, authentication, roomUser, a);

        SkullKingStateResponse expected = mock(SkullKingStateResponse.class);
        when(stateBuilder.buildState(eq(session), eq(roomUser), eq(true))).thenReturn(expected);

        SkullKingStateResponse result = service.getState("room", authentication);

        assertSame(expected, result);
        // Loading/reconnecting keeps the in-person game's room from expiring.
        verify(roomsUtilityService).updateRoomLastUpdated(session.getRoom());
    }

    @Test
    void getState_passesIsAdminFalseForPlayer() {
        User user = user();
        Authentication authentication = auth();
        Team a = team();
        RoomUser roomUser = new RoomUser(user, null, RoomUserRoles.PLAYER);
        Session session = readSession(user, authentication, roomUser, a);

        service.getState("room", authentication);

        verify(stateBuilder).buildState(session, roomUser, false);
    }
}
