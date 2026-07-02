package com.motomutterers.boardgames.rooms.dto;

import com.motomutterers.boardgames.rooms.model.Invitation.InvitationStatus;
import com.motomutterers.boardgames.rooms.model.Invitation.RoomInvitationToken;

public class RoomInvitationResponse {
    private String username;
    private InvitationStatus status;

    public RoomInvitationResponse(){}

    public RoomInvitationResponse(
        RoomInvitationToken invitation
    ) {
        this.username = invitation.getUser().getUsername();
        this.status = invitation.getStatus();
    }

    public void setUsername(String username){this.username = username;}
    public void setInvitationStatusResponse(InvitationStatus status){this.status = status;}

    public String getUsername(){return this.username;}
    public InvitationStatus getStatus(){return this.status;}
}
