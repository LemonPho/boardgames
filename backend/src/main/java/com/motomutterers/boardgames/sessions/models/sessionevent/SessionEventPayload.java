package com.motomutterers.boardgames.sessions.models.sessionevent;

public sealed interface SessionEventPayload {

    record Bids(int round, int cardCount) implements SessionEventPayload {}

    record InProgress(int round, int cardCount) implements SessionEventPayload {}

    record TrickResults(int round, int cardCount) implements SessionEventPayload {}

    record BonusPoints(int round, int cardCount) implements SessionEventPayload {}
}
