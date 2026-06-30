package com.motomutterers.boardgames.rooms.dto;

import java.time.LocalDateTime;

import com.motomutterers.boardgames.rooms.model.Room.RoomUser;
import com.motomutterers.boardgames.rooms.model.Room.RoomUserRoles;
import com.motomutterers.boardgames.user.dto.UserResponse;

public class RoomUserResponse {
    private UserResponse user;
    private String displayName;
    private RoomUserRoles role;
    private LocalDateTime joinedAt;

    public RoomUserResponse(){}

    public RoomUserResponse(RoomUser roomUser){
        this.user = new UserResponse(roomUser.getUser());
        this.displayName = roomUser.getDisplayName();
        this.role = roomUser.getRole();
        this.joinedAt = roomUser.getJoinedAt();
    }

    public void setUser(UserResponse user){
        this.user = user;
    }

    public void setDisplayName(String displayName){
        this.displayName = displayName;
    }

    public void setRole(RoomUserRoles role){
        this.role = role;
    }

    public void setJoinedAt(LocalDateTime joinedAt){
        this.joinedAt = joinedAt;
    }

    public UserResponse getUser() {return this.user;}
    public String getDisplayName() {return this.displayName;}
    public RoomUserRoles getRole() {return this.role;}
    public LocalDateTime getJoinedAt() {return this.joinedAt;}
}
