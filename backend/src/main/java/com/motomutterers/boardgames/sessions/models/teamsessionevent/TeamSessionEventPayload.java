package com.motomutterers.boardgames.sessions.models.teamsessionevent;

public sealed interface TeamSessionEventPayload {
    record Bids(int bid) implements TeamSessionEventPayload {};
    record TrickResults(int tricksWon) implements TeamSessionEventPayload {};
    record BonusPoints(
        int standardFourteens,
        boolean blackFourteen,
        int mermaidsByPirate,
        int piratesBySkullKing,
        boolean skullKingByMermaid,
        int loot
    ) implements TeamSessionEventPayload {};
}
