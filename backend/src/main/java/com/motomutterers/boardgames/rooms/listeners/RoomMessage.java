package com.motomutterers.boardgames.rooms.listeners;

public record RoomMessage<T>(String type, T payload) {}
