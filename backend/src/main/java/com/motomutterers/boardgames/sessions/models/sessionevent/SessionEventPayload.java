package com.motomutterers.boardgames.sessions.models.sessionevent;

import java.util.List;

public sealed interface SessionEventPayload {

    record Bids(int round, int cardCount) implements SessionEventPayload {}

    record InProgress() implements SessionEventPayload {}

    record TrickResults() implements SessionEventPayload {}

    record BonusPoints() implements SessionEventPayload {}
}
