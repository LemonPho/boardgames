package com.motomutterers.boardgames.skullking.services;

import com.motomutterers.boardgames.exceptions.BadActionException;
import com.motomutterers.boardgames.exceptions.UnauthorizedException;
import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.rooms.model.Room.RoomUser;
import com.motomutterers.boardgames.rooms.model.Room.RoomUserRoles;
import com.motomutterers.boardgames.sessions.models.session.Session;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEvent;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEventType;
import com.motomutterers.boardgames.sessions.models.teamsessionevent.TeamSessionEventType;
import com.motomutterers.boardgames.teams.models.Team;
import com.motomutterers.boardgames.user.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Chunk 3 — live per-team submission. submitBid / submitTrickResult /
 * submitBonusPoints and the authority rule (throwIfUserCantSubmitForTeam):
 * phase guards, value ranges, the running trick-total ceiling, and bonus
 * eligibility.
 */
public class SkullKingSubmissionTest extends SkullKingTestSupport {

    private User user() {
        User u = new User("p@test.com", "player", "hash");
        ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
        return u;
    }

    /**
     * Wires the auth/room/roomUser/session/team lookups shared by every submit
     * path, returning the session. The submitting RoomUser has the given role and
     * is linked to `team` (so a non-admin submitting for their own team passes).
     */
    private Session submitSession(User user, Authentication authentication, RoomUserRoles role, Team team, Team... teams) {
        Session session = session(false, teams);
        RoomUser roomUser = new RoomUser(user, session.getRoom(), role);
        roomUser.setTeam(team);

        when(userService.getAuthenticatedUser(authentication)).thenReturn(user);
        when(roomsUtilityService.getRoomByName("room")).thenReturn(session.getRoom());
        when(roomsUtilityService.getOrThrowRoomUserByUserAndRoom(user, session.getRoom())).thenReturn(roomUser);
        when(sessionUtilitysService.getOrThrowSessionByRoom(session.getRoom())).thenReturn(session);
        return session;
    }

    private com.motomutterers.boardgames.skullking.dto.SubmitBidRequest bidReq(Team team, int bid) {
        var r = new com.motomutterers.boardgames.skullking.dto.SubmitBidRequest();
        r.setTeamId(id(team));
        r.setBid(bid);
        return r;
    }

    private com.motomutterers.boardgames.skullking.dto.SubmitTrickResultRequest trickReq(Team team, int tricksWon) {
        var r = new com.motomutterers.boardgames.skullking.dto.SubmitTrickResultRequest();
        r.setTeamId(id(team));
        r.setTricksWon(tricksWon);
        return r;
    }

    private com.motomutterers.boardgames.skullking.dto.SubmitBonusPointsRequest bonusReq(
            Team team, int std, boolean black, int mermaid, int pirate, boolean sk, int loot) {
        var r = new com.motomutterers.boardgames.skullking.dto.SubmitBonusPointsRequest();
        r.setTeamId(id(team));
        r.setStandardFourteens(std);
        r.setBlackFourteen(black);
        r.setMermaidsByPirate(mermaid);
        r.setPiratesBySkullKing(pirate);
        r.setSkullKingByMermaid(sk);
        r.setLoot(loot);
        return r;
    }

    // --- submitBid ---

    @Test
    void submitBid_validDuringBidsPhase_upserts() {
        User user = user();
        Authentication authentication = auth();
        Team team = team();
        Session session = submitSession(user, authentication, RoomUserRoles.ADMIN, team, team);

        SessionEvent bids = bidsEvent(1, 5, id(team));
        when(teamUtilityService.getOrThrowTeamById(id(team))).thenReturn(team);
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(bids);

        service.submitBid("room", bidReq(team, 3), authentication);

        verify(teamSessionEventService).upsertTeamSessionEvent(
            eq(session), eq(bids), eq(team), eq(TeamSessionEventType.BIDS), any());
    }

    @Test
    void submitBid_wrongPhase_throws() {
        User user = user();
        Authentication authentication = auth();
        Team team = team();
        Session session = submitSession(user, authentication, RoomUserRoles.ADMIN, team, team);

        when(teamUtilityService.getOrThrowTeamById(id(team))).thenReturn(team);
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(tricksEvent(1, 5, id(team), false));

        assertThrows(BadActionException.class, () -> service.submitBid("room", bidReq(team, 3), authentication));
        verify(teamSessionEventService, never()).upsertTeamSessionEvent(any(), any(), any(), any(), any());
    }

    @Test
    void submitBid_aboveCardCount_throws() {
        User user = user();
        Authentication authentication = auth();
        Team team = team();
        Session session = submitSession(user, authentication, RoomUserRoles.ADMIN, team, team);

        when(teamUtilityService.getOrThrowTeamById(id(team))).thenReturn(team);
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(bidsEvent(1, 5, id(team)));

        assertThrows(BadActionException.class, () -> service.submitBid("room", bidReq(team, 6), authentication));
    }

    @Test
    void submitBid_negative_throws() {
        User user = user();
        Authentication authentication = auth();
        Team team = team();
        Session session = submitSession(user, authentication, RoomUserRoles.ADMIN, team, team);

        when(teamUtilityService.getOrThrowTeamById(id(team))).thenReturn(team);
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(bidsEvent(1, 5, id(team)));

        assertThrows(BadActionException.class, () -> service.submitBid("room", bidReq(team, -1), authentication));
    }

    // --- throwIfUserCantSubmitForTeam (via submitBid) ---

    @Test
    void submit_nonAdminForOwnTeam_allowed() {
        User user = user();
        Authentication authentication = auth();
        Team ownTeam = team();
        Session session = submitSession(user, authentication, RoomUserRoles.PLAYER, ownTeam, ownTeam);

        when(teamUtilityService.getOrThrowTeamById(id(ownTeam))).thenReturn(ownTeam);
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(bidsEvent(1, 5, id(ownTeam)));

        assertDoesNotThrow(() -> service.submitBid("room", bidReq(ownTeam, 2), authentication));
    }

    @Test
    void submit_nonAdminForOtherTeam_throwsUnauthorized() {
        User user = user();
        Authentication authentication = auth();
        Team ownTeam = team();
        Team otherTeam = team();
        // Player is linked to ownTeam but tries to submit for otherTeam.
        Session session = submitSession(user, authentication, RoomUserRoles.PLAYER, ownTeam, ownTeam, otherTeam);

        when(teamUtilityService.getOrThrowTeamById(id(otherTeam))).thenReturn(otherTeam);

        assertThrows(UnauthorizedException.class,
            () -> service.submitBid("room", bidReq(otherTeam, 2), authentication));
        verify(teamSessionEventService, never()).upsertTeamSessionEvent(any(), any(), any(), any(), any());
    }

    @Test
    void submit_adminForAnyTeam_allowed() {
        User user = user();
        Authentication authentication = auth();
        Team someTeam = team();
        // Admin has no team of their own but may submit for any team.
        Session session = submitSession(user, authentication, RoomUserRoles.ADMIN, null, someTeam);

        when(teamUtilityService.getOrThrowTeamById(id(someTeam))).thenReturn(someTeam);
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(bidsEvent(1, 5, id(someTeam)));

        assertDoesNotThrow(() -> service.submitBid("room", bidReq(someTeam, 2), authentication));
    }

    // --- submitTrickResult ---

    @Test
    void submitTrickResult_validWithinRunningTotal_upserts() {
        User user = user();
        Authentication authentication = auth();
        Team a = team();
        Team b = team();
        Session session = submitSession(user, authentication, RoomUserRoles.ADMIN, a, a, b);

        SessionEvent tricks = tricksEvent(1, 5, id(a), false);
        when(teamUtilityService.getOrThrowTeamById(id(a))).thenReturn(a);
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(tricks);
        // b already has 2; a submitting 3 -> total 5 == cardCount, allowed.
        stubTeamEvents(tricks, teamTricks(b, 2));

        service.submitTrickResult("room", trickReq(a, 3), authentication);

        verify(teamSessionEventService).upsertTeamSessionEvent(
            eq(session), eq(tricks), eq(a), eq(TeamSessionEventType.TRICK_RESULTS), any());
    }

    @Test
    void submitTrickResult_exceedsRunningTotal_throws() {
        User user = user();
        Authentication authentication = auth();
        Team a = team();
        Team b = team();
        Session session = submitSession(user, authentication, RoomUserRoles.ADMIN, a, a, b);

        SessionEvent tricks = tricksEvent(1, 5, id(a), false);
        when(teamUtilityService.getOrThrowTeamById(id(a))).thenReturn(a);
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(tricks);
        // b has 4; a submitting 3 -> 7 > 5 cards dealt.
        stubTeamEvents(tricks, teamTricks(b, 4));

        assertThrows(BadActionException.class, () -> service.submitTrickResult("room", trickReq(a, 3), authentication));
        verify(teamSessionEventService, never()).upsertTeamSessionEvent(any(), any(), any(), any(), any());
    }

    @Test
    void submitTrickResult_correctionReplacesOwnPreviousValue() {
        User user = user();
        Authentication authentication = auth();
        Team a = team();
        Team b = team();
        Session session = submitSession(user, authentication, RoomUserRoles.ADMIN, a, a, b);

        SessionEvent tricks = tricksEvent(1, 5, id(a), false);
        when(teamUtilityService.getOrThrowTeamById(id(a))).thenReturn(a);
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(tricks);
        // a already recorded 5, b recorded 0. a corrects to 4: own old value is
        // excluded, so 0 (others) + 4 = 4 <= 5, allowed rather than 5+4 overflow.
        stubTeamEvents(tricks, teamTricks(a, 5), teamTricks(b, 0));

        assertDoesNotThrow(() -> service.submitTrickResult("room", trickReq(a, 4), authentication));
    }

    @Test
    void submitTrickResult_wrongPhase_throws() {
        User user = user();
        Authentication authentication = auth();
        Team a = team();
        Session session = submitSession(user, authentication, RoomUserRoles.ADMIN, a, a);

        when(teamUtilityService.getOrThrowTeamById(id(a))).thenReturn(a);
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(bidsEvent(1, 5, id(a)));

        assertThrows(BadActionException.class, () -> service.submitTrickResult("room", trickReq(a, 1), authentication));
    }

    @Test
    void submitTrickResult_aboveCardCount_throws() {
        User user = user();
        Authentication authentication = auth();
        Team a = team();
        Session session = submitSession(user, authentication, RoomUserRoles.ADMIN, a, a);

        SessionEvent tricks = tricksEvent(1, 5, id(a), false);
        when(teamUtilityService.getOrThrowTeamById(id(a))).thenReturn(a);
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(tricks);

        assertThrows(BadActionException.class, () -> service.submitTrickResult("room", trickReq(a, 6), authentication));
    }

    // --- submitBonusPoints ---

    @Test
    void submitBonusPoints_eligibleTeam_upserts() {
        User user = user();
        Authentication authentication = auth();
        Team a = team();
        Session session = submitSession(user, authentication, RoomUserRoles.ADMIN, a, a);

        SessionEvent bonus = bonusEvent(1, 5, id(a));
        SessionEvent bids = bidsEvent(1, 5, id(a));
        SessionEvent tricks = tricksEvent(1, 5, id(a), false);
        when(teamUtilityService.getOrThrowTeamById(id(a))).thenReturn(a);
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(bonus);
        // Eligibility: latest BIDS + TRICK_RESULTS, bid > 0 and bid == tricksWon.
        when(sessionEventService.findLatestEventOfType(session, SessionEventType.BIDS)).thenReturn(Optional.of(bids));
        when(sessionEventService.findLatestEventOfType(session, SessionEventType.TRICK_RESULTS)).thenReturn(Optional.of(tricks));
        stubTeamEvents(bids, teamBid(a, 2));
        stubTeamEvents(tricks, teamTricks(a, 2));

        service.submitBonusPoints("room", bonusReq(a, 1, false, 0, 0, false, 0), authentication);

        verify(teamSessionEventService).upsertTeamSessionEvent(
            eq(session), eq(bonus), eq(a), eq(TeamSessionEventType.BONUS_POINTS), any());
    }

    @Test
    void submitBonusPoints_ineligibleTeamWithBonus_throws() {
        User user = user();
        Authentication authentication = auth();
        Team a = team();
        Session session = submitSession(user, authentication, RoomUserRoles.ADMIN, a, a);

        SessionEvent bonus = bonusEvent(1, 5, id(a));
        SessionEvent bids = bidsEvent(1, 5, id(a));
        SessionEvent tricks = tricksEvent(1, 5, id(a), false);
        when(teamUtilityService.getOrThrowTeamById(id(a))).thenReturn(a);
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(bonus);
        when(sessionEventService.findLatestEventOfType(session, SessionEventType.BIDS)).thenReturn(Optional.of(bids));
        when(sessionEventService.findLatestEventOfType(session, SessionEventType.TRICK_RESULTS)).thenReturn(Optional.of(tricks));
        // Missed bid (3 != 1): ineligible, but a bonus is submitted -> reject.
        stubTeamEvents(bids, teamBid(a, 3));
        stubTeamEvents(tricks, teamTricks(a, 1));

        assertThrows(BadActionException.class,
            () -> service.submitBonusPoints("room", bonusReq(a, 1, false, 0, 0, false, 0), authentication));
        verify(teamSessionEventService, never()).upsertTeamSessionEvent(any(), any(), any(), any(), any());
    }

    @Test
    void submitBonusPoints_ineligibleTeamWithNoBonus_allowed() {
        User user = user();
        Authentication authentication = auth();
        Team a = team();
        Session session = submitSession(user, authentication, RoomUserRoles.ADMIN, a, a);

        SessionEvent bonus = bonusEvent(1, 5, id(a));
        SessionEvent bids = bidsEvent(1, 5, id(a));
        SessionEvent tricks = tricksEvent(1, 5, id(a), false);
        when(teamUtilityService.getOrThrowTeamById(id(a))).thenReturn(a);
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(bonus);
        when(sessionEventService.findLatestEventOfType(session, SessionEventType.BIDS)).thenReturn(Optional.of(bids));
        when(sessionEventService.findLatestEventOfType(session, SessionEventType.TRICK_RESULTS)).thenReturn(Optional.of(tricks));
        stubTeamEvents(bids, teamBid(a, 3));
        stubTeamEvents(tricks, teamTricks(a, 1));

        // No bonus submitted, so an ineligible team may still record zeros.
        assertDoesNotThrow(() -> service.submitBonusPoints("room", bonusReq(a, 0, false, 0, 0, false, 0), authentication));
        verify(teamSessionEventService).upsertTeamSessionEvent(
            eq(session), eq(bonus), eq(a), eq(TeamSessionEventType.BONUS_POINTS), any());
    }

    @Test
    void submitBonusPoints_wrongPhase_throws() {
        User user = user();
        Authentication authentication = auth();
        Team a = team();
        Session session = submitSession(user, authentication, RoomUserRoles.ADMIN, a, a);

        when(teamUtilityService.getOrThrowTeamById(id(a))).thenReturn(a);
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(bidsEvent(1, 5, id(a)));

        assertThrows(BadActionException.class,
            () -> service.submitBonusPoints("room", bonusReq(a, 1, false, 0, 0, false, 0), authentication));
    }

    @Test
    void submitBonusPoints_outOfRangeValue_throws() {
        User user = user();
        Authentication authentication = auth();
        Team a = team();
        Session session = submitSession(user, authentication, RoomUserRoles.ADMIN, a, a);

        when(teamUtilityService.getOrThrowTeamById(id(a))).thenReturn(a);
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(bonusEvent(1, 5, id(a)));

        // 4 standard 14s exceeds the per-team max of 3.
        assertThrows(BadActionException.class,
            () -> service.submitBonusPoints("room", bonusReq(a, 4, false, 0, 0, false, 0), authentication));
    }
}
