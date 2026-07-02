package com.motomutterers.boardgames.rooms.dto;

import com.motomutterers.boardgames.games.dto.SimpleGameResponse;
import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.rooms.model.Room.RoomStatus;

public class SimpleRoomResponse {
    private SimpleGameResponse game;
    private String name;
    private RoomStatus status;

    public SimpleRoomResponse(){}

    public SimpleRoomResponse(Room room){
        this.game = new SimpleGameResponse(room.getGame());
        this.name = room.getName();
        this.status = room.getStatus();
    }

    public SimpleGameResponse getGame(){return this.game;}
    public String getName(){return this.name;}
    public RoomStatus getStatus(){return this.status;}

    public void setGame(SimpleGameResponse game){this.game = game;}
    public void setName(String name){this.name = name;}
    public void setStatus(RoomStatus status){this.status = status;}
}
