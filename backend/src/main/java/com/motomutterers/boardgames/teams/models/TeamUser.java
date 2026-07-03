package com.motomutterers.boardgames.teams.models;

import java.time.LocalDateTime;
import java.util.UUID;

import com.motomutterers.boardgames.rooms.model.Room.RoomUser;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "teams_users")
public class TeamUser {
    @GeneratedValue
    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    @ManyToOne
    @JoinColumn(name = "room_user_id")
    private RoomUser roomUser;

    private LocalDateTime createdAt;

    public TeamUser(
        Team team,
        RoomUser roomUser
    ) {
        this.team = team;
        this.roomUser = roomUser;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId(){return this.id;}
    public Team getTeam(){return this.team;}
    public RoomUser getRoomUser(){return this.roomUser;}
    public LocalDateTime getCreatedAt(){return this.createdAt;}

    public void setTeam(Team team){this.team = team;}
    public void setRoomUser(RoomUser roomUser){this.roomUser = roomUser;}
}
