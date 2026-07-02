package com.motomutterers.boardgames.rooms.events;

public class RoomUpdatedEvent {
    private final String roomName;

    public RoomUpdatedEvent(String roomName) {
        this.roomName = roomName;
    }

    public String getRoomName() {
        return roomName;
    }
}
