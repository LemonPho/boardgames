package com.motomutterers.boardgames.sessions.models.sessionevent;

public sealed interface SessionEventPayload {

    record Bids(int round, int cardCount) implements SessionEventPayload {}

    record InProgress(int round, int cardCount) implements SessionEventPayload {}

    // krakenPlayed: a Kraken destroyed a trick this round. That trick is won by
    // nobody, so the teams' tricks must sum to cardCount − 1 rather than cardCount.
    record TrickResults(int round, int cardCount, boolean krakenPlayed) implements SessionEventPayload {}

    record BonusPoints(int round, int cardCount) implements SessionEventPayload {}
}
