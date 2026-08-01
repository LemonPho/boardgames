package com.motomutterers.boardgames.rooms.dto;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.motomutterers.boardgames.games.dto.SimpleGameResponse;
import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.rooms.model.Room.RoomConfiguration;
import com.motomutterers.boardgames.rooms.model.Room.RoomStatus;
import com.motomutterers.boardgames.rooms.model.Room.TrackingMode;

public class RoomResponse {
    private String name;
    private SimpleGameResponse game;
    private RoomStatus status;
    private RoomConfiguration configuration;
    private TrackingMode trackingMode;
    // Players include pending invites (RoomUsers with status PENDING_INVITE) and
    // declines; each carries its status, so the client renders invite state
    // without a separate list. Ordered by playing position (seat/turn order).
    private List<RoomUserResponse> players = new java.util.ArrayList<RoomUserResponse>();
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime createdAt;

    public RoomResponse(){}

    public RoomResponse(Room room){
        this.name = room.getName();
        this.game = new SimpleGameResponse(room.getGame());
        this.status = room.getStatus();
        this.configuration = room.getConfiguration();
        this.trackingMode = room.getTrackingMode();
        // Declined invites are kept in the DB (to block re-invite spam) but are not
        // room members, so they're excluded here. Active players and pending invites
        // are shown, ordered by playing position.
        this.players = room.getPlayers().stream()
            .filter(p -> p.getStatus() != com.motomutterers.boardgames.rooms.model.Room.RoomUserStatus.DECLINED)
            .sorted(Comparator.comparingInt(com.motomutterers.boardgames.rooms.model.Room.RoomUser::getPlayingPosition))
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

    public RoomConfiguration getConfiguration(){
        return configuration;
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

    public void setConfiguration(RoomConfiguration configuration){
        this.configuration = configuration;
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
