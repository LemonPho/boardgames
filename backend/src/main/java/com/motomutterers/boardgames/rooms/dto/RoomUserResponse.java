package com.motomutterers.boardgames.rooms.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.motomutterers.boardgames.rooms.model.Room.RoomUser;
import com.motomutterers.boardgames.rooms.model.Room.RoomUserRoles;
import com.motomutterers.boardgames.rooms.model.Room.RoomUserStatus;
import com.motomutterers.boardgames.teams.dto.TeamResponse;
import com.motomutterers.boardgames.user.dto.UserResponse;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoomUserResponse {
    private UUID id;
    private UserResponse user;
    private String displayName;
    private RoomUserRoles role;
    private RoomUserStatus status;
    private int playingPosition;
    private TeamResponse team;
    private LocalDateTime joinedAt;

    public RoomUserResponse(){}

    public RoomUserResponse(RoomUser roomUser){
        this.id = roomUser.getId();
        if(roomUser.getUser() != null) this.user = new UserResponse(roomUser.getUser());
        this.displayName = roomUser.getDisplayName();
        this.role = roomUser.getRole();
        this.status = roomUser.getStatus();
        this.playingPosition = roomUser.getPlayingPosition();
        if(roomUser.getTeam() != null) this.team = new TeamResponse(roomUser.getTeam(), false);
        this.joinedAt = roomUser.getJoinedAt();
    }

    public RoomUserResponse(RoomUser roomUser, boolean includeTeam){
        this.id = roomUser.getId();
        if(roomUser.getUser() != null) this.user = new UserResponse(roomUser.getUser());
        this.displayName = roomUser.getDisplayName();
        this.role = roomUser.getRole();
        this.status = roomUser.getStatus();
        this.playingPosition = roomUser.getPlayingPosition();
        if(includeTeam && roomUser.getTeam() != null) this.team = new TeamResponse(roomUser.getTeam(), false);
        this.joinedAt = roomUser.getJoinedAt();
    }

    public UUID getId() {return this.id;}
    public UserResponse getUser() {return this.user;}
    public String getDisplayName() {return this.displayName;}
    public RoomUserRoles getRole() {return this.role;}
    public RoomUserStatus getStatus() {return this.status;}
    public int getPlayingPosition() {return this.playingPosition;}
    public TeamResponse getTeam() {return this.team;}
    public LocalDateTime getJoinedAt() {return this.joinedAt;}
}
