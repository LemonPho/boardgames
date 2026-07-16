package com.motomutterers.boardgames.sessions.models.sessionevent;

public sealed interface SessionEventPayload {

    // startingTeamId: the team that leads (goes first) this round. Chosen randomly
    // for round 1 and rotated by one seat (in join order) each round. Carried on
    // every phase payload so the current event always knows the round's leader.
    // Nullable so events stored before this field existed deserialize cleanly.

    record Bids(int round, int cardCount, String startingTeamId) implements SessionEventPayload {}

    record InProgress(int round, int cardCount, String startingTeamId) implements SessionEventPayload {}

    // krakenPlayed: a Kraken destroyed a trick this round. That trick is won by
    // nobody, so the teams' tricks must sum to cardCount − 1 rather than cardCount.
    record TrickResults(int round, int cardCount, String startingTeamId, boolean krakenPlayed) implements SessionEventPayload {}

    record BonusPoints(int round, int cardCount, String startingTeamId) implements SessionEventPayload {}
}
