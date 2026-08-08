package com.motomutterers.boardgames.skullking.dto;

/**
 * What kind of player a scoreboard row represents, so the frontend knows whether
 * the name links to a profile:
 *   ACTIVE    : a live account — links to /profile/{username}
 *   DELETED   : the account was deleted (soft-deleted, so the name still resolves
 *               here) — render the name but don't link, there's no profile to visit
 *   ANONYMOUS : an admin-added placeholder with no account at all
 */
public enum ScoreboardPlayerType {
    ACTIVE,
    DELETED,
    ANONYMOUS
}
