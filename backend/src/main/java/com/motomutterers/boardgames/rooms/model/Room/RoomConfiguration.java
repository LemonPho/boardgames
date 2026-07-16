package com.motomutterers.boardgames.rooms.model.Room;

/**
 * Per-room game configuration, stored as JSONB so new settings can be added
 * without a schema change. Fields are shared across games where sensible
 * (trackingMode) and game-specific where needed (advancedCards for Skull King).
 * Unknown/missing keys deserialize to defaults, so old rooms stay valid.
 */
public class RoomConfiguration {
    private TrackingMode trackingMode = TrackingMode.ADMIN;
    private boolean advancedCards = false;

    public RoomConfiguration() {}

    public RoomConfiguration(TrackingMode trackingMode, boolean advancedCards) {
        if (trackingMode != null) this.trackingMode = trackingMode;
        this.advancedCards = advancedCards;
    }

    public TrackingMode getTrackingMode() { return this.trackingMode; }
    public boolean getAdvancedCards() { return this.advancedCards; }

    public void setTrackingMode(TrackingMode trackingMode) { this.trackingMode = trackingMode; }
    public void setAdvancedCards(boolean advancedCards) { this.advancedCards = advancedCards; }
}
