package com.motomutterers.boardgames.exceptions;

import java.util.UUID;

import com.motomutterers.boardgames.sessions.models.teamsessionevent.TeamSessionEventPayload;
import com.motomutterers.boardgames.sessions.models.teamsessionevent.TeamSessionEventType;

/**
 * A live save was made against a value that has changed since the client last saw it.
 * Not an error: the handler replies 409 with the current value and version so the
 * out-of-date client can silently adopt them. Thrown before the write, so the
 * transaction rolls back with nothing saved and nothing published.
 */
public class VersionConflictException extends RuntimeException {
    private final UUID teamId;
    private final TeamSessionEventType type;
    private final int currentVersion;
    // The stored value, or null when there's no row yet (current version 0).
    private final TeamSessionEventPayload currentPayload;

    public VersionConflictException(
        UUID teamId,
        TeamSessionEventType type,
        int currentVersion,
        TeamSessionEventPayload currentPayload
    ){
        super("Version conflict on " + type + " for team " + teamId + ": current version " + currentVersion);
        this.teamId = teamId;
        this.type = type;
        this.currentVersion = currentVersion;
        this.currentPayload = currentPayload;
    }

    public UUID getTeamId(){return this.teamId;}
    public TeamSessionEventType getType(){return this.type;}
    public int getCurrentVersion(){return this.currentVersion;}
    public TeamSessionEventPayload getCurrentPayload(){return this.currentPayload;}
}
