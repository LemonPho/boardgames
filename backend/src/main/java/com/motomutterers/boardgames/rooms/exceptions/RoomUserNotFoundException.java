package com.motomutterers.boardgames.rooms.exceptions;

public class RoomUserNotFoundException extends RuntimeException{
    public RoomUserNotFoundException(String message){
        super(message);
    }
}
