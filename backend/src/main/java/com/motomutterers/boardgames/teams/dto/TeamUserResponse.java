package com.motomutterers.boardgames.teams.dto;

import com.motomutterers.boardgames.rooms.dto.RoomUserResponse;
import com.motomutterers.boardgames.teams.models.TeamUser;

public class TeamUserResponse {
    private RoomUserResponse user;
    
    public TeamUserResponse(
        TeamUser teamUser
    ) {
        this.user = new RoomUserResponse(teamUser.getRoomUser());
    }

    public RoomUserResponse getUser(){return this.user;}
}
