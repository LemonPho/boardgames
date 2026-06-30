package com.motomutterers.boardgames.rooms.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.motomutterers.boardgames.games.dto.SimpleGameResponse;
import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.rooms.model.Room.RoomStatus;
import com.motomutterers.boardgames.rooms.model.Room.TrackingMode;

public class RoomResponse {
    private String name;
    private SimpleGameResponse game;
    private RoomStatus status;
    private TrackingMode trackingMode;
    private List<RoomUserResponse> players = new ArrayList<RoomUserResponse>();
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime createdAt;

    public RoomResponse(){}

    public RoomResponse(Room room){
        this.name = room.getName();
        this.game = new SimpleGameResponse(room.getGame());
        this.status = room.getStatus();
        this.trackingMode = room.getTrackingMode();
        this.players = room.getPlayers().stream()
            .map(RoomUserResponse::new)
            .collect(Collectors.toList());
        this.startedAt = room.getStartedAt();
        this.endedAt = room.getEndedAt();
        this.createdAt = room.getCreatedAt();
    }

    public String getName(){
        return name;
    }

    public SimpleGameResponse getGame(){
        return game;
    }

    public RoomStatus getStatus(){
        return status;
    }

    public TrackingMode getTrackingMode(){
        return trackingMode;
    }

    public List<RoomUserResponse> getPlayers(){
        return players;
    }

    public LocalDateTime getStartedAt(){
        return startedAt;
    }

    public LocalDateTime getEndedAt(){
        return endedAt;
    }

    public LocalDateTime getCreatedAt(){
        return createdAt;
    }

    public void setName(String name){
        this.name = name;
    }

    public void setGame(SimpleGameResponse game){
        this.game = game;
    }

    public void setStatus(RoomStatus status){
        this.status = status;
    }

    public void setTrackingMode(TrackingMode trackingMode){
        this.trackingMode = trackingMode;
    }

    public void setStartedAt(LocalDateTime startedAt){
        this.startedAt = startedAt;
    }

    public void setEndedAt(LocalDateTime endedAt){
        this.endedAt = endedAt;
    }
    
}
