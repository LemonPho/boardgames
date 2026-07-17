package com.motomutterers.boardgames.skullking.services;

import com.motomutterers.boardgames.exceptions.BadActionException;
import com.motomutterers.boardgames.sessions.models.session.Session;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEvent;
import com.motomutterers.boardgames.sessions.models.teamsessionevent.TeamSessionEventPayload;
import com.motomutterers.boardgames.teams.models.Team;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

/**
 * Chunk 1 — scoring. The per-round score math (scoreTeamRound / bonusValue) and
 * the full-replay recompute. This is the highest-value area: a wrong formula
 * produces silently-incorrect scores, never an exception.
 */
public class SkullKingScoringTest extends SkullKingTestSupport {

    // Invoke the private pure scorer directly for crisp table tests.
    private long score(int bid, int tricksWon, int cardCount, TeamSessionEventPayload.BonusPoints bonus) {
        return (long) ReflectionTestUtils.invokeMethod(service, "scoreTeamRound", bid, tricksWon, cardCount, bonus);
    }

    private long bonusValue(TeamSessionEventPayload.BonusPoints bonus) {
        return (long) ReflectionTestUtils.invokeMethod(service, "bonusValue", bonus);
    }

    // --- scoreTeamRound: zero bid ---

    @Test
    void zeroBid_hit_scoresTenPerCard() {
        // Bid 0, won 0, over 7 cards -> +10 * 7.
        assertEquals(70, score(0, 0, 7, null));
    }

    @Test
    void zeroBid_miss_losesTenPerCard() {
        // Bid 0 but won 2, over 7 cards -> -10 * 7.
        assertEquals(-70, score(0, 2, 7, null));
    }

    @Test
    void zeroBid_hit_ignoresBonus() {
        // A zero bid takes no tricks, so no cards are captured -> bonus can't apply.
        // Still just +10 * 5 even with a fat bonus attached.
        assertEquals(50, score(0, 0, 5, bonus(3, true, 2, 5, true, 2)));
    }

    // --- scoreTeamRound: positive bid ---

    @Test
    void positiveBid_hit_scoresTwentyPerTrick() {
        // Bid 3, won 3 -> 20 * 3. No bonus.
        assertEquals(60, score(3, 3, 10, null));
    }

    @Test
    void positiveBid_hit_addsBonus() {
        // Bid 2, won 2 -> 40, plus one standard 14 (+10).
        assertEquals(50, score(2, 2, 10, bonus(1, false, 0, 0, false, 0)));
    }

    @Test
    void positiveBid_miss_losesTenPerTrickOff() {
        // Bid 4, won 1 -> off by 3 -> -30. Bonus is ignored on a miss.
        assertEquals(-30, score(4, 1, 10, bonus(3, true, 0, 0, false, 0)));
    }

    @Test
    void positiveBid_missOver_losesTenPerTrickOff() {
        // Bid 1, won 4 -> off by 3 -> -30.
        assertEquals(-30, score(1, 4, 10, null));
    }

    // --- bonusValue: each type and combined ---

    @Test
    void bonusValue_null_isZero() {
        assertEquals(0, bonusValue(null));
    }

    @Test
    void bonusValue_standardFourteens_tenEach() {
        assertEquals(30, bonusValue(bonus(3, false, 0, 0, false, 0)));
    }

    @Test
    void bonusValue_blackFourteen_twenty() {
        assertEquals(20, bonusValue(bonus(0, true, 0, 0, false, 0)));
    }

    @Test
    void bonusValue_mermaidByPirate_twentyEach() {
        assertEquals(40, bonusValue(bonus(0, false, 2, 0, false, 0)));
    }

    @Test
    void bonusValue_pirateBySkullKing_thirtyEach() {
        assertEquals(90, bonusValue(bonus(0, false, 0, 3, false, 0)));
    }

    @Test
    void bonusValue_skullKingByMermaid_forty() {
        assertEquals(40, bonusValue(bonus(0, false, 0, 0, true, 0)));
    }

    @Test
    void bonusValue_loot_twentyEach() {
        assertEquals(40, bonusValue(bonus(0, false, 0, 0, false, 2)));
    }

    @Test
    void bonusValue_combined_sumsAll() {
        // 1*10 + 20 + 1*20 + 1*30 + 40 + 1*20 = 140.
        assertEquals(140, bonusValue(bonus(1, true, 1, 1, true, 1)));
    }

    // --- validateLoot: even, capped at 4, pairable ---

    private void validateLoot(int total, int max) {
        // BadActionException is a RuntimeException, so ReflectionTestUtils propagates
        // it directly to assertThrows.
        ReflectionTestUtils.invokeMethod(service, "validateLoot", total, max);
    }

    @Test
    void validateLoot_zero_passes() {
        assertDoesNotThrow(() -> validateLoot(0, 0));
    }

    @Test
    void validateLoot_evenPairable_passes() {
        // One alliance: two teams hold 1 coin each -> total 2, max 1.
        assertDoesNotThrow(() -> validateLoot(2, 1));
        // Two alliances: total 4, max 2.
        assertDoesNotThrow(() -> validateLoot(4, 2));
    }

    @Test
    void validateLoot_oddTotal_throws() {
        // Every alliance pays exactly two teams, so the total must be even.
        assertThrows(BadActionException.class, () -> validateLoot(3, 2));
    }

    @Test
    void validateLoot_exceedsFour_throws() {
        // Only two Loot cards exist -> at most 4 coins.
        assertThrows(BadActionException.class, () -> validateLoot(6, 3));
    }

    @Test
    void validateLoot_notPairable_throws() {
        // total 2 (one alliance) but a team holds 2 coins -> can't be paired.
        assertThrows(BadActionException.class, () -> validateLoot(2, 2));
    }

    // --- validateBonusCardLimits: round-wide totals can't exceed the deck ---

    private void validateBonusCardLimits(int standardFourteens, int blackFourteens,
            int mermaidsByPirate, int piratesBySkullKing, int skullKingsByMermaid) {
        ReflectionTestUtils.invokeMethod(service, "validateBonusCardLimits",
            standardFourteens, blackFourteens, mermaidsByPirate, piratesBySkullKing, skullKingsByMermaid);
    }

    @Test
    void bonusCardLimits_atDeckMaximums_passes() {
        // Exactly what the deck holds: 3 coloured 14s, 1 black 14, 2 mermaids,
        // 5 pirates, 1 Skull King.
        assertDoesNotThrow(() -> validateBonusCardLimits(3, 1, 2, 5, 1));
    }

    @Test
    void bonusCardLimits_allZero_passes() {
        assertDoesNotThrow(() -> validateBonusCardLimits(0, 0, 0, 0, 0));
    }

    @Test
    void bonusCardLimits_tooManyStandardFourteens_throws() {
        // e.g. two teams each claiming 2 -> 4, but only 3 exist.
        assertThrows(BadActionException.class, () -> validateBonusCardLimits(4, 0, 0, 0, 0));
    }

    @Test
    void bonusCardLimits_twoBlackFourteens_throws() {
        // Only one black 14 exists, so two teams can't both capture it.
        assertThrows(BadActionException.class, () -> validateBonusCardLimits(0, 2, 0, 0, 0));
    }

    @Test
    void bonusCardLimits_tooManyMermaids_throws() {
        assertThrows(BadActionException.class, () -> validateBonusCardLimits(0, 0, 3, 0, 0));
    }

    @Test
    void bonusCardLimits_tooManyPirates_throws() {
        assertThrows(BadActionException.class, () -> validateBonusCardLimits(0, 0, 0, 6, 0));
    }

    @Test
    void bonusCardLimits_twoSkullKings_throws() {
        assertThrows(BadActionException.class, () -> validateBonusCardLimits(0, 0, 0, 0, 2));
    }

    // --- recomputeScores: full replay through the event graph ---

    @Test
    void recomputeScores_singleRound_positiveBidHitWithBonus() {
        Team team = team();
        Session session = session(false, team);

        SessionEvent bids = bidsEvent(1, 5, id(team));
        SessionEvent tricks = tricksEvent(1, 5, id(team), false);
        SessionEvent bonus = bonusEvent(1, 5, id(team));
        stubAllEvents(session, bids, tricks, bonus);

        stubTeamEvents(bids, teamBid(team, 2));
        stubTeamEvents(tricks, teamTricks(team, 2));
        stubTeamEvents(bonus, teamBonus(team, bonus(1, false, 0, 0, false, 0)));

        ReflectionTestUtils.invokeMethod(service, "recomputeScores", session);

        // 20*2 + 10 (one standard 14) = 50.
        verify(teamUtilityService).setScore(team, 50L);
    }

    @Test
    void recomputeScores_krakenRound_scoresOnBidVsTricksNotDeck() {
        // A Kraken destroyed one trick this round (flag on the TRICK_RESULTS event),
        // so tricks sum to cardCount − 1. Scoring is driven by bid vs tricksWon, not
        // the kraken flag: a positive bid met still scores 20 * bid.
        Team team = team();
        Session session = session(false, team);

        SessionEvent bids = bidsEvent(1, 5, id(team));
        SessionEvent tricks = tricksEvent(1, 5, id(team), true);  // kraken played
        SessionEvent bonus = bonusEvent(1, 5, id(team));
        stubAllEvents(session, bids, tricks, bonus);

        stubTeamEvents(bids, teamBid(team, 2));
        stubTeamEvents(tricks, teamTricks(team, 2));
        stubTeamEvents(bonus, teamBonus(team, bonus(0, false, 0, 0, false, 0)));

        ReflectionTestUtils.invokeMethod(service, "recomputeScores", session);

        // Bid 2, won 2 -> 20 * 2 = 40. The kraken changes the trick-total rule, not the score.
        verify(teamUtilityService).setScore(team, 40L);
    }

    @Test
    void recomputeScores_krakenRound_zeroBidScoresPerCardDealt() {
        // A zero-bid team scores ±10 per card DEALT (cardCount), regardless of the
        // kraken destroying a trick. 5 cards, bid 0, won 0 -> +50.
        Team team = team();
        Session session = session(false, team);

        SessionEvent bids = bidsEvent(1, 5, id(team));
        SessionEvent tricks = tricksEvent(1, 5, id(team), true);
        SessionEvent bonus = bonusEvent(1, 5, id(team));
        stubAllEvents(session, bids, tricks, bonus);

        stubTeamEvents(bids, teamBid(team, 0));
        stubTeamEvents(tricks, teamTricks(team, 0));
        stubTeamEvents(bonus, teamBonus(team, bonus(0, false, 0, 0, false, 0)));

        ReflectionTestUtils.invokeMethod(service, "recomputeScores", session);

        verify(teamUtilityService).setScore(team, 50L);
    }

    @Test
    void recomputeScores_accumulatesAcrossRounds() {
        Team team = team();
        Session session = session(false, team);

        SessionEvent bids1 = bidsEvent(1, 1, id(team));
        SessionEvent tricks1 = tricksEvent(1, 1, id(team), false);
        SessionEvent bonus1 = bonusEvent(1, 1, id(team));
        SessionEvent bids2 = bidsEvent(2, 2, id(team));
        SessionEvent tricks2 = tricksEvent(2, 2, id(team), false);
        SessionEvent bonus2 = bonusEvent(2, 2, id(team));
        stubAllEvents(session, bids1, tricks1, bonus1, bids2, tricks2, bonus2);

        // Round 1: bid 1, won 1 -> +20. Round 2: bid 0, won 0 over 2 cards -> +20.
        stubTeamEvents(bids1, teamBid(team, 1));
        stubTeamEvents(tricks1, teamTricks(team, 1));
        stubTeamEvents(bonus1, teamBonus(team, bonus(0, false, 0, 0, false, 0)));
        stubTeamEvents(bids2, teamBid(team, 0));
        stubTeamEvents(tricks2, teamTricks(team, 0));
        stubTeamEvents(bonus2, teamBonus(team, bonus(0, false, 0, 0, false, 0)));

        ReflectionTestUtils.invokeMethod(service, "recomputeScores", session);

        verify(teamUtilityService).setScore(team, 40L);
    }

    @Test
    void recomputeScores_skipsRoundMissingTricks() {
        Team team = team();
        Session session = session(false, team);

        // Round 1 complete; round 2 has bids only (not scorable yet).
        SessionEvent bids1 = bidsEvent(1, 1, id(team));
        SessionEvent tricks1 = tricksEvent(1, 1, id(team), false);
        SessionEvent bids2 = bidsEvent(2, 2, id(team));
        stubAllEvents(session, bids1, tricks1, bids2);

        stubTeamEvents(bids1, teamBid(team, 1));
        stubTeamEvents(tricks1, teamTricks(team, 1));
        stubTeamEvents(bids2, teamBid(team, 2));

        ReflectionTestUtils.invokeMethod(service, "recomputeScores", session);

        // Only round 1 scores: bid 1, won 1 -> +20. Round 2 skipped (no tricks).
        verify(teamUtilityService).setScore(team, 20L);
    }

    @Test
    void recomputeScores_missedBidIgnoresBonus() {
        Team team = team();
        Session session = session(false, team);

        SessionEvent bids = bidsEvent(1, 10, id(team));
        SessionEvent tricks = tricksEvent(1, 10, id(team), false);
        SessionEvent bonus = bonusEvent(1, 10, id(team));
        stubAllEvents(session, bids, tricks, bonus);

        // Bid 3, won 1 -> off by 2 -> -20. Bonus present but ignored on a miss.
        stubTeamEvents(bids, teamBid(team, 3));
        stubTeamEvents(tricks, teamTricks(team, 1));
        stubTeamEvents(bonus, teamBonus(team, bonus(3, true, 0, 0, false, 0)));

        ReflectionTestUtils.invokeMethod(service, "recomputeScores", session);

        verify(teamUtilityService).setScore(team, -20L);
    }
}
