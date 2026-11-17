package com.motomutterers.boardgames.skullking.services;

import com.motomutterers.boardgames.exceptions.BadActionException;
import com.motomutterers.boardgames.sessions.models.session.Session;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEvent;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEventPayload;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEventType;
import com.motomutterers.boardgames.sessions.models.teamsessionevent.TeamSessionEventType;
import com.motomutterers.boardgames.skullking.dto.CorrectBidsRequest;
import com.motomutterers.boardgames.skullking.dto.CorrectBonusRequest;
import com.motomutterers.boardgames.skullking.dto.CorrectTricksRequest;
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
 * Chunk 4 — past-round corrections (admin only) and the Kraken toggle.
 * correctBids / correctTricks / correctBonus / setKraken: admin guard, per-set
 * validation (ranges, trick totals, loot, bonus card limits), upsert of every
 * team, and score recompute.
 */
public class SkullKingCorrectionsTest extends SkullKingTestSupport {

    private User user() {
        User u = new User("admin@test.com", "admin", "hash");
        ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
        return u;
    }

    /** Wires the admin auth/room/session lookups shared by every correction path. */
    private Session correctionSession(User admin, Authentication authentication, Team... teams) {
        Session session = session(false, teams);
        when(userService.getAuthenticatedUser(authentication)).thenReturn(admin);
        when(roomsUtilityService.getRoomByName("room")).thenReturn(session.getRoom());
        when(sessionUtilitysService.getOrThrowSessionByRoom(session.getRoom())).thenReturn(session);
        return session;
    }

    private CorrectBidsRequest.TeamValue bidVal(Team team, int value) {
        var tv = new CorrectBidsRequest.TeamValue();
        tv.setTeamId(id(team));
        tv.setValue(value);
        return tv;
    }

    private CorrectBidsRequest bidsReq(CorrectBidsRequest.TeamValue... vals) {
        var r = new CorrectBidsRequest();
        r.setTeams(List.of(vals));
        return r;
    }

    private CorrectTricksRequest.TeamValue trickVal(Team team, int value) {
        var tv = new CorrectTricksRequest.TeamValue();
        tv.setTeamId(id(team));
        tv.setValue(value);
        return tv;
    }

    private CorrectTricksRequest tricksReq(CorrectTricksRequest.TeamValue... vals) {
        var r = new CorrectTricksRequest();
        r.setTeams(List.of(vals));
        return r;
    }

    private CorrectBonusRequest.TeamBonusValue bonusVal(
            Team team, int std, boolean black, int mermaid, int pirate, boolean sk, int loot) {
        var tv = new CorrectBonusRequest.TeamBonusValue();
        tv.setTeamId(id(team));
        tv.setStandardFourteens(std);
        tv.setBlackFourteen(black);
        tv.setMermaidsByPirate(mermaid);
        tv.setPiratesBySkullKing(pirate);
        tv.setSkullKingByMermaid(sk);
        tv.setLoot(loot);
        return tv;
    }

    private CorrectBonusRequest bonusReq(CorrectBonusRequest.TeamBonusValue... vals) {
        var r = new CorrectBonusRequest();
        r.setTeams(List.of(vals));
        return r;
    }

    // --- correctBids ---

    @Test
    void correctBids_valid_upsertsEachTeamAndRecomputes() {
        User admin = user();
        Authentication authentication = auth();
        Team a = team();
        Team b = team();
        Session session = correctionSession(admin, authentication, a, b);

        SessionEvent bids = bidsEvent(1, 5, id(a));
        stubAllEvents(session, bids);
        when(teamUtilityService.getOrThrowTeamById(id(a))).thenReturn(a);
        when(teamUtilityService.getOrThrowTeamById(id(b))).thenReturn(b);

        service.correctBids("room", 1, bidsReq(bidVal(a, 2), bidVal(b, 3)), authentication);

        verify(teamSessionEventService).upsertTeamSessionEvent(eq(session), eq(bids), eq(a), eq(TeamSessionEventType.BIDS), any());
        verify(teamSessionEventService).upsertTeamSessionEvent(eq(session), eq(bids), eq(b), eq(TeamSessionEventType.BIDS), any());
        // recompute runs afterwards (reads all events again).
        verify(sessionEventService, atLeastOnce()).findAllEvents(session);
    }

    @Test
    void correctBids_notAdmin_throwsAndSavesNothing() {
        User user = user();
        Authentication authentication = auth();
        Team a = team();
        Session session = session(false, a);
        when(userService.getAuthenticatedUser(authentication)).thenReturn(user);
        when(roomsUtilityService.getRoomByName("room")).thenReturn(session.getRoom());
        doThrow(new BadActionException("not admin"))
            .when(roomsUtilityService).throwIfUserIsNotRoomAdmin(session.getRoom(), user);

        assertThrows(BadActionException.class,
            () -> service.correctBids("room", 1, bidsReq(bidVal(a, 2)), authentication));
        verify(teamSessionEventService, never()).upsertTeamSessionEvent(any(), any(), any(), any(), any());
    }

    @Test
    void correctBids_bidAboveCardCount_throwsBeforeSaving() {
        User admin = user();
        Authentication authentication = auth();
        Team a = team();
        Session session = correctionSession(admin, authentication, a);

        stubAllEvents(session, bidsEvent(1, 5, id(a)));

        // Validation runs over the whole set before any upsert.
        assertThrows(BadActionException.class,
            () -> service.correctBids("room", 1, bidsReq(bidVal(a, 6)), authentication));
        verify(teamSessionEventService, never()).upsertTeamSessionEvent(any(), any(), any(), any(), any());
    }

    @Test
    void correctBids_roundHasNoBidsPhase_throws() {
        User admin = user();
        Authentication authentication = auth();
        Team a = team();
        Session session = correctionSession(admin, authentication, a);

        // No events for round 2.
        stubAllEvents(session, bidsEvent(1, 5, id(a)));

        assertThrows(BadActionException.class,
            () -> service.correctBids("room", 2, bidsReq(bidVal(a, 1)), authentication));
    }

    @Test
    void correctBids_duringUnfinishedRound_scoresFinishedRoundsOnly() {
        User admin = user();
        Authentication authentication = auth();
        Team a = team();
        Team b = team();
        Session session = correctionSession(admin, authentication, a, b);

        // Round 1 finished (bonus followed by round 2 BIDS); round 2 is in TRICK_RESULTS
        // with values already entered.
        SessionEvent bids1 = bidsEvent(1, 1, id(a));
        SessionEvent tricks1 = tricksEvent(1, 1, id(a), false);
        SessionEvent bonus1 = bonusEvent(1, 1, id(a));
        SessionEvent bids2 = bidsEvent(2, 2, id(b));
        SessionEvent inProgress2 = inProgressEvent(2, 2, id(b));
        SessionEvent tricks2 = tricksEvent(2, 2, id(b), false);
        stubAllEvents(session, bids1, tricks1, bonus1, bids2, inProgress2, tricks2);
        when(teamUtilityService.getOrThrowTeamById(id(a))).thenReturn(a);
        when(teamUtilityService.getOrThrowTeamById(id(b))).thenReturn(b);

        // The mocked upsert doesn't persist, so stub the corrected round-1 values directly.
        stubTeamEvents(bids1, teamBid(a, 1), teamBid(b, 0));
        stubTeamEvents(tricks1, teamTricks(a, 1), teamTricks(b, 0));
        stubTeamEvents(bonus1, teamBonus(a, bonus(0, false, 0, 0, false, 0)), teamBonus(b, bonus(0, false, 0, 0, false, 0)));
        stubTeamEvents(bids2, teamBid(a, 2), teamBid(b, 0));
        stubTeamEvents(tricks2, teamTricks(a, 2), teamTricks(b, 0));

        service.correctBids("room", 1, bidsReq(bidVal(a, 1), bidVal(b, 0)), authentication);

        // Round 1 only: a bid 1 won 1 -> +20; b bid 0 won 0 over 1 card -> +10.
        // Round 2 (a +40, b +20 if it counted) is excluded.
        verify(teamUtilityService).setScore(a, 20L);
        verify(teamUtilityService).setScore(b, 10L);
    }

    // --- corrections on the live (current) round: admin review from the session screen ---

    /**
     * Round 1 finished (a bid 1 won 1 -> +20; b bid 0 won 0 over 1 card -> +10); round 2 is
     * the live round, currently in {@code livePhase}. Returns round 2's BIDS and TRICK_RESULTS events.
     */
    private SessionEvent[] stubFinishedRound1AndLiveRound2(Session session, Team a, Team b, SessionEventType livePhase) {
        SessionEvent bids1 = bidsEvent(1, 1, id(a));
        SessionEvent tricks1 = tricksEvent(1, 1, id(a), false);
        SessionEvent bonus1 = bonusEvent(1, 1, id(a));
        SessionEvent bids2 = bidsEvent(2, 2, id(b));
        SessionEvent inProgress2 = inProgressEvent(2, 2, id(b));
        SessionEvent tricks2 = tricksEvent(2, 2, id(b), false);
        SessionEvent bonus2 = bonusEvent(2, 2, id(b));
        if (livePhase == SessionEventType.TRICK_RESULTS) {
            stubAllEvents(session, bids1, tricks1, bonus1, bids2, inProgress2, tricks2);
        } else {
            stubAllEvents(session, bids1, tricks1, bonus1, bids2, inProgress2, tricks2, bonus2);
            stubTeamEvents(bonus2, teamBonus(a, bonus(1, false, 0, 0, false, 0)), teamBonus(b, bonus(0, false, 0, 0, false, 0)));
        }
        when(teamUtilityService.getOrThrowTeamById(id(a))).thenReturn(a);
        when(teamUtilityService.getOrThrowTeamById(id(b))).thenReturn(b);

        stubTeamEvents(bids1, teamBid(a, 1), teamBid(b, 0));
        stubTeamEvents(tricks1, teamTricks(a, 1), teamTricks(b, 0));
        stubTeamEvents(bonus1, teamBonus(a, bonus(0, false, 0, 0, false, 0)), teamBonus(b, bonus(0, false, 0, 0, false, 0)));
        // Round 2 as corrected (the mocked upsert doesn't persist): a bid 2 won 2, b bid 0 won 0.
        // If round 2 were scored, a would gain +40 (+10 bonus) and b +20.
        stubTeamEvents(bids2, teamBid(a, 2), teamBid(b, 0));
        stubTeamEvents(tricks2, teamTricks(a, 2), teamTricks(b, 0));
        return new SessionEvent[]{bids2, tricks2};
    }

    @Test
    void correctBids_currentRoundInTrickResults_upsertsAndDoesNotScoreIt() {
        User admin = user();
        Authentication authentication = auth();
        Team a = team();
        Team b = team();
        Session session = correctionSession(admin, authentication, a, b);
        SessionEvent bids2 = stubFinishedRound1AndLiveRound2(session, a, b, SessionEventType.TRICK_RESULTS)[0];

        service.correctBids("room", 2, bidsReq(bidVal(a, 2), bidVal(b, 0)), authentication);

        verify(teamSessionEventService).upsertTeamSessionEvent(eq(session), eq(bids2), eq(a), eq(TeamSessionEventType.BIDS), any());
        verify(teamSessionEventService).upsertTeamSessionEvent(eq(session), eq(bids2), eq(b), eq(TeamSessionEventType.BIDS), any());
        // Scores stay at round 1's totals: the live round isn't finished.
        verify(teamUtilityService).setScore(a, 20L);
        verify(teamUtilityService).setScore(b, 10L);
    }

    @Test
    void correctBids_currentRoundInBonusPoints_upsertsAndDoesNotScoreIt() {
        User admin = user();
        Authentication authentication = auth();
        Team a = team();
        Team b = team();
        Session session = correctionSession(admin, authentication, a, b);
        SessionEvent bids2 = stubFinishedRound1AndLiveRound2(session, a, b, SessionEventType.BONUS_POINTS)[0];

        service.correctBids("room", 2, bidsReq(bidVal(a, 2), bidVal(b, 0)), authentication);

        verify(teamSessionEventService).upsertTeamSessionEvent(eq(session), eq(bids2), eq(a), eq(TeamSessionEventType.BIDS), any());
        verify(teamSessionEventService).upsertTeamSessionEvent(eq(session), eq(bids2), eq(b), eq(TeamSessionEventType.BIDS), any());
        // The open bonus phase isn't closed by a correction, so round 2 stays unscored.
        verify(teamUtilityService).setScore(a, 20L);
        verify(teamUtilityService).setScore(b, 10L);
    }

    @Test
    void correctTricks_currentRoundInBonusPoints_upsertsAndDoesNotScoreIt() {
        User admin = user();
        Authentication authentication = auth();
        Team a = team();
        Team b = team();
        Session session = correctionSession(admin, authentication, a, b);
        SessionEvent tricks2 = stubFinishedRound1AndLiveRound2(session, a, b, SessionEventType.BONUS_POINTS)[1];

        // 2 + 0 == 2 cards.
        service.correctTricks("room", 2, tricksReq(trickVal(a, 2), trickVal(b, 0)), authentication);

        verify(teamSessionEventService).upsertTeamSessionEvent(eq(session), eq(tricks2), eq(a), eq(TeamSessionEventType.TRICK_RESULTS), any());
        verify(teamSessionEventService).upsertTeamSessionEvent(eq(session), eq(tricks2), eq(b), eq(TeamSessionEventType.TRICK_RESULTS), any());
        verify(teamUtilityService).setScore(a, 20L);
        verify(teamUtilityService).setScore(b, 10L);
    }

    @Test
    void correctTricks_currentRoundInTrickResults_upsertsAndDoesNotScoreIt() {
        User admin = user();
        Authentication authentication = auth();
        Team a = team();
        Team b = team();
        Session session = correctionSession(admin, authentication, a, b);
        SessionEvent tricks2 = stubFinishedRound1AndLiveRound2(session, a, b, SessionEventType.TRICK_RESULTS)[1];

        service.correctTricks("room", 2, tricksReq(trickVal(a, 2), trickVal(b, 0)), authentication);

        verify(teamSessionEventService).upsertTeamSessionEvent(eq(session), eq(tricks2), eq(a), eq(TeamSessionEventType.TRICK_RESULTS), any());
        verify(teamUtilityService).setScore(a, 20L);
        verify(teamUtilityService).setScore(b, 10L);
    }

    // --- correctTricks ---

    @Test
    void correctTricks_totalMatchesCards_upsertsAndRecomputes() {
        User admin = user();
        Authentication authentication = auth();
        Team a = team();
        Team b = team();
        Session session = correctionSession(admin, authentication, a, b);

        SessionEvent bids = bidsEvent(1, 5, id(a));
        SessionEvent tricks = tricksEvent(1, 5, id(a), false);
        stubAllEvents(session, bids, tricks);
        when(teamUtilityService.getOrThrowTeamById(id(a))).thenReturn(a);
        when(teamUtilityService.getOrThrowTeamById(id(b))).thenReturn(b);

        // 2 + 3 == 5 cards.
        service.correctTricks("room", 1, tricksReq(trickVal(a, 2), trickVal(b, 3)), authentication);

        verify(teamSessionEventService).upsertTeamSessionEvent(eq(session), eq(tricks), eq(a), eq(TeamSessionEventType.TRICK_RESULTS), any());
        verify(teamSessionEventService).upsertTeamSessionEvent(eq(session), eq(tricks), eq(b), eq(TeamSessionEventType.TRICK_RESULTS), any());
    }

    @Test
    void correctTricks_totalWrong_throws() {
        User admin = user();
        Authentication authentication = auth();
        Team a = team();
        Team b = team();
        Session session = correctionSession(admin, authentication, a, b);

        stubAllEvents(session, bidsEvent(1, 5, id(a)), tricksEvent(1, 5, id(a), false));

        // 2 + 2 == 4 != 5 cards.
        assertThrows(BadActionException.class,
            () -> service.correctTricks("room", 1, tricksReq(trickVal(a, 2), trickVal(b, 2)), authentication));
        verify(teamSessionEventService, never()).upsertTeamSessionEvent(any(), any(), any(), any(), any());
    }

    @Test
    void correctTricks_krakenReducesExpectedTotal() {
        User admin = user();
        Authentication authentication = auth();
        Team a = team();
        Team b = team();
        Session session = correctionSession(admin, authentication, a, b);

        SessionEvent bids = bidsEvent(1, 5, id(a));
        SessionEvent tricks = tricksEvent(1, 5, id(a), true);  // kraken played
        stubAllEvents(session, bids, tricks);
        when(teamUtilityService.getOrThrowTeamById(id(a))).thenReturn(a);
        when(teamUtilityService.getOrThrowTeamById(id(b))).thenReturn(b);

        // Kraken destroyed a trick: total must be 5 - 1 = 4.
        service.correctTricks("room", 1, tricksReq(trickVal(a, 2), trickVal(b, 2)), authentication);

        verify(teamSessionEventService).upsertTeamSessionEvent(eq(session), eq(tricks), eq(a), eq(TeamSessionEventType.TRICK_RESULTS), any());
    }

    @Test
    void correctTricks_valueAboveCardCount_throws() {
        User admin = user();
        Authentication authentication = auth();
        Team a = team();
        Session session = correctionSession(admin, authentication, a);

        stubAllEvents(session, bidsEvent(1, 5, id(a)), tricksEvent(1, 5, id(a), false));

        assertThrows(BadActionException.class,
            () -> service.correctTricks("room", 1, tricksReq(trickVal(a, 6)), authentication));
    }

    // --- setKraken ---

    @Test
    void setKraken_updatesTrickResultsPayload() {
        User admin = user();
        Authentication authentication = auth();
        Team a = team();
        Session session = correctionSession(admin, authentication, a);

        SessionEvent tricks = tricksEvent(1, 5, id(a), false);
        stubAllEvents(session, bidsEvent(1, 5, id(a)), tricks);

        service.setKraken("room", 1, true, authentication);

        ArgumentCaptor<SessionEventPayload> captor = ArgumentCaptor.forClass(SessionEventPayload.class);
        verify(sessionEventService).updatePayload(eq(tricks), captor.capture());
        SessionEventPayload.TrickResults updated = (SessionEventPayload.TrickResults) captor.getValue();
        assertTrue(updated.krakenPlayed());
        assertEquals(5, updated.cardCount());  // other fields preserved
    }

    @Test
    void setKraken_notAdmin_throws() {
        User user = user();
        Authentication authentication = auth();
        Session session = session(false, team());
        when(userService.getAuthenticatedUser(authentication)).thenReturn(user);
        when(roomsUtilityService.getRoomByName("room")).thenReturn(session.getRoom());
        doThrow(new BadActionException("not admin"))
            .when(roomsUtilityService).throwIfUserIsNotRoomAdmin(session.getRoom(), user);

        assertThrows(BadActionException.class, () -> service.setKraken("room", 1, true, authentication));
        verify(sessionEventService, never()).updatePayload(any(), any());
    }

    // --- correctBonus ---

    @Test
    void correctBonus_eligibleTeam_upsertsAndRecomputes() {
        User admin = user();
        Authentication authentication = auth();
        Team a = team();
        Session session = correctionSession(admin, authentication, a);

        SessionEvent bids = bidsEvent(1, 5, id(a));
        SessionEvent tricks = tricksEvent(1, 5, id(a), false);
        SessionEvent bonus = bonusEvent(1, 5, id(a));
        stubAllEvents(session, bids, tricks, bonus);
        when(teamUtilityService.getOrThrowTeamById(id(a))).thenReturn(a);
        // Eligibility read per team from the round's bids + tricks events.
        stubTeamEvents(bids, teamBid(a, 2));
        stubTeamEvents(tricks, teamTricks(a, 2));

        service.correctBonus("room", 1, bonusReq(bonusVal(a, 1, false, 0, 0, false, 0)), authentication);

        verify(teamSessionEventService).upsertTeamSessionEvent(eq(session), eq(bonus), eq(a), eq(TeamSessionEventType.BONUS_POINTS), any());
    }

    @Test
    void correctBonus_ineligibleTeamWithBonus_throws() {
        User admin = user();
        Authentication authentication = auth();
        Team a = team();
        Session session = correctionSession(admin, authentication, a);

        SessionEvent bids = bidsEvent(1, 5, id(a));
        SessionEvent tricks = tricksEvent(1, 5, id(a), false);
        SessionEvent bonus = bonusEvent(1, 5, id(a));
        stubAllEvents(session, bids, tricks, bonus);
        // Missed bid (3 != 1): ineligible for bonus.
        stubTeamEvents(bids, teamBid(a, 3));
        stubTeamEvents(tricks, teamTricks(a, 1));

        assertThrows(BadActionException.class,
            () -> service.correctBonus("room", 1, bonusReq(bonusVal(a, 1, false, 0, 0, false, 0)), authentication));
        verify(teamSessionEventService, never()).upsertTeamSessionEvent(any(), any(), any(), any(), any());
    }

    @Test
    void correctBonus_madeZeroBidWithLoot_upserts() {
        User admin = user();
        Authentication authentication = auth();
        Team a = team();
        Team b = team();
        Session session = correctionSession(admin, authentication, a, b);

        SessionEvent bids = bidsEvent(1, 5, id(a));
        SessionEvent tricks = tricksEvent(1, 5, id(a), false);
        SessionEvent bonus = bonusEvent(1, 5, id(a));
        stubAllEvents(session, bids, tricks, bonus);
        when(teamUtilityService.getOrThrowTeamById(id(a))).thenReturn(a);
        when(teamUtilityService.getOrThrowTeamById(id(b))).thenReturn(b);
        // a made a zero bid (LOOT_ONLY) and allied with b, who made 5. Loot pairs up (1 + 1).
        stubTeamEvents(bids, teamBid(a, 0), teamBid(b, 5));
        stubTeamEvents(tricks, teamTricks(a, 0), teamTricks(b, 5));

        service.correctBonus("room", 1,
            bonusReq(bonusVal(a, 0, false, 0, 0, false, 1), bonusVal(b, 0, false, 0, 0, false, 1)),
            authentication);

        verify(teamSessionEventService).upsertTeamSessionEvent(eq(session), eq(bonus), eq(a), eq(TeamSessionEventType.BONUS_POINTS), any());
    }

    @Test
    void correctBonus_madeZeroBidWithNonLootBonus_throws() {
        User admin = user();
        Authentication authentication = auth();
        Team a = team();
        Session session = correctionSession(admin, authentication, a);

        SessionEvent bids = bidsEvent(1, 5, id(a));
        SessionEvent tricks = tricksEvent(1, 5, id(a), false);
        SessionEvent bonus = bonusEvent(1, 5, id(a));
        stubAllEvents(session, bids, tricks, bonus);
        stubTeamEvents(bids, teamBid(a, 0));
        stubTeamEvents(tricks, teamTricks(a, 0));

        // A zero bid captured nothing, so the black 14 is impossible -> reject.
        BadActionException ex = assertThrows(BadActionException.class,
            () -> service.correctBonus("room", 1, bonusReq(bonusVal(a, 0, true, 0, 0, false, 0)), authentication));
        assertEquals("A zero bid can only earn Loot bonus points", ex.getMessage());
        verify(teamSessionEventService, never()).upsertTeamSessionEvent(any(), any(), any(), any(), any());
    }

    @Test
    void correctBonus_missedBidWithLoot_throws() {
        User admin = user();
        Authentication authentication = auth();
        Team a = team();
        Session session = correctionSession(admin, authentication, a);

        SessionEvent bids = bidsEvent(1, 5, id(a));
        SessionEvent tricks = tricksEvent(1, 5, id(a), false);
        SessionEvent bonus = bonusEvent(1, 5, id(a));
        stubAllEvents(session, bids, tricks, bonus);
        // Missed zero bid (0 != 2): NONE, so even Loot is rejected.
        stubTeamEvents(bids, teamBid(a, 0));
        stubTeamEvents(tricks, teamTricks(a, 2));

        BadActionException ex = assertThrows(BadActionException.class,
            () -> service.correctBonus("room", 1, bonusReq(bonusVal(a, 0, false, 0, 0, false, 1)), authentication));
        assertEquals("A team can only earn bonus points if it made a bid of one or more", ex.getMessage());
        verify(teamSessionEventService, never()).upsertTeamSessionEvent(any(), any(), any(), any(), any());
    }

    @Test
    void correctBonus_oddLootTotal_throws() {
        User admin = user();
        Authentication authentication = auth();
        Team a = team();
        Team b = team();
        Session session = correctionSession(admin, authentication, a, b);

        SessionEvent bids = bidsEvent(1, 5, id(a));
        SessionEvent tricks = tricksEvent(1, 5, id(a), false);
        SessionEvent bonus = bonusEvent(1, 5, id(a));
        stubAllEvents(session, bids, tricks, bonus);
        stubTeamEvents(bids, teamBid(a, 2), teamBid(b, 2));
        stubTeamEvents(tricks, teamTricks(a, 2), teamTricks(b, 2));

        // Loot total 1 is odd -> alliances always pay two teams.
        assertThrows(BadActionException.class,
            () -> service.correctBonus("room", 1,
                bonusReq(bonusVal(a, 0, false, 0, 0, false, 1), bonusVal(b, 0, false, 0, 0, false, 0)),
                authentication));
    }

    @Test
    void correctBonus_exceedsBonusCardLimits_throws() {
        User admin = user();
        Authentication authentication = auth();
        Team a = team();
        Team b = team();
        Session session = correctionSession(admin, authentication, a, b);

        SessionEvent bids = bidsEvent(1, 10, id(a));
        SessionEvent tricks = tricksEvent(1, 10, id(a), false);
        SessionEvent bonus = bonusEvent(1, 10, id(a));
        stubAllEvents(session, bids, tricks, bonus);
        stubTeamEvents(bids, teamBid(a, 2), teamBid(b, 2));
        stubTeamEvents(tricks, teamTricks(a, 2), teamTricks(b, 2));

        // Both teams claim the single black 14 -> only one exists.
        assertThrows(BadActionException.class,
            () -> service.correctBonus("room", 1,
                bonusReq(bonusVal(a, 0, true, 0, 0, false, 0), bonusVal(b, 0, true, 0, 0, false, 0)),
                authentication));
        verify(teamSessionEventService, never()).upsertTeamSessionEvent(any(), any(), any(), any(), any());
    }

    @Test
    void correctBonus_whileLaterBonusPhaseOpen_doesNotScoreOpenRound() {
        User admin = user();
        Authentication authentication = auth();
        Team a = team();
        Session session = correctionSession(admin, authentication, a);

        // Round 1 finished; round 2's BONUS_POINTS is the latest (current) event.
        SessionEvent bids1 = bidsEvent(1, 1, id(a));
        SessionEvent tricks1 = tricksEvent(1, 1, id(a), false);
        SessionEvent bonus1 = bonusEvent(1, 1, id(a));
        SessionEvent bids2 = bidsEvent(2, 2, id(a));
        SessionEvent tricks2 = tricksEvent(2, 2, id(a), false);
        SessionEvent bonus2 = bonusEvent(2, 2, id(a));
        stubAllEvents(session, bids1, tricks1, bonus1, bids2, tricks2, bonus2);
        when(teamUtilityService.getOrThrowTeamById(id(a))).thenReturn(a);

        stubTeamEvents(bids1, teamBid(a, 1));
        stubTeamEvents(tricks1, teamTricks(a, 1));
        // Mocked upsert doesn't persist; stub round 1's corrected bonus (one standard 14).
        stubTeamEvents(bonus1, teamBonus(a, bonus(1, false, 0, 0, false, 0)));
        stubTeamEvents(bids2, teamBid(a, 2));
        stubTeamEvents(tricks2, teamTricks(a, 2));
        stubTeamEvents(bonus2, teamBonus(a, bonus(0, false, 0, 0, false, 0)));

        service.correctBonus("room", 1, bonusReq(bonusVal(a, 1, false, 0, 0, false, 0)), authentication);

        // Round 1 only: 20 * 1 + 10 = 30. Round 2 (+40) isn't finished yet.
        verify(teamUtilityService).setScore(a, 30L);
    }
}
