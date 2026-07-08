package com.motomutterers.boardgames.sessions.models.sessionevent;

import java.util.List;

public sealed interface SessionEventPayload {

    record RoundStart(int round, int cardCount) implements SessionEventPayload {}

    record Bids(int round) implements SessionEventPayload {}

    record RoundResults(int round) implements SessionEventPayload {}

    //record SessionEnd(List<TeamScore> finalScores) implements SessionEventPayload {}
}
