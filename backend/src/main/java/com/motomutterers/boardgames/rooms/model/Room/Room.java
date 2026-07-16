package com.motomutterers.boardgames.rooms.model.Room;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.motomutterers.boardgames.games.model.Game;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "rooms")
public class Room {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)  
    @Column(columnDefinition = "room_status")
    private RoomStatus status;

    @OneToMany(mappedBy = "room", fetch = FetchType.LAZY)
    private List<RoomUser> players = new ArrayList<RoomUser>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private RoomConfiguration configuration = new RoomConfiguration();

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private LocalDateTime lastUpdated;

    @Column(insertable = false, updatable = false, nullable = false)
    private LocalDateTime createdAt;

    public Room(){}

    public Room(
        Game game,
        String name,
        RoomConfiguration configuration
    ){
        this.game = game;
        this.name = name;
        this.configuration = configuration;
        this.status = RoomStatus.WAITING;
        this.lastUpdated = LocalDateTime.now();
    }

    public UUID getId(){
        return id;
    }

    public Game getGame(){
        return game;
    }

    public String getName(){
        return name;
    }

    public RoomStatus getStatus(){
        return status;
    }

    public RoomConfiguration getConfiguration(){
        return configuration;
    }

    // Convenience accessor: tracking mode is part of the room configuration.
    public TrackingMode getTrackingMode(){
        return configuration != null ? configuration.getTrackingMode() : null;
    }

    public List<RoomUser> getPlayers(){
        return players;
    }

    public LocalDateTime getStartedAt(){
        return startedAt;
    }

    public LocalDateTime getEndedAt(){
        return endedAt;
    }

    public LocalDateTime getLastUpdated(){
        return lastUpdated;
    }

    public LocalDateTime getCreatedAt(){
        return createdAt;
    }

    public void setGame(Game game){
        this.game = game;
    }

    public void setName(String name){
        this.name = name;
    }

    public void setStatus(RoomStatus status){
        this.status = status;
    }

    public void setConfiguration(RoomConfiguration configuration){
        this.configuration = configuration;
    }

    public void addPlayer(RoomUser player){
        this.players.add(player);
    }

    public void setStartedAt(LocalDateTime startedAt){
        this.startedAt = startedAt;
    }

    public void setEndedAt(LocalDateTime endedAt){
        this.endedAt = endedAt;
    }

    public void setLastUpdated(LocalDateTime lastUpdated){
        this.lastUpdated = lastUpdated;
    }
}
