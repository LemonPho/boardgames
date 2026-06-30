package com.motomutterers.boardgames.rooms.exceptions;

public class RoomExpiredException extends RuntimeException {
    public RoomExpiredException(String message){
        super(message);
    }
}
