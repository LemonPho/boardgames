package com.motomutterers.boardgames.rooms.dto;

import java.time.LocalDateTime;

import com.motomutterers.boardgames.rooms.model.RoomUser;
import com.motomutterers.boardgames.rooms.model.RoomUserRoles;
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
}
