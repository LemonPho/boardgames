package com.motomutterers.boardgames.user.dto;

import com.motomutterers.boardgames.user.model.User;

public class UserAvailabilityResponse extends UserResponse {
    private boolean inGame;
    private boolean invited;
    private boolean declined;

    public UserAvailabilityResponse(User user, boolean inGame, boolean invited, boolean declined){
        this.setEmail(user.getEmail());
        this.setUsername(user.getUsername());
        this.inGame = inGame;
        this.invited = invited;
        this.declined = declined;
    }

    public boolean getInGame(){
        return this.inGame;
    }

    public boolean getInvited(){
        return this.invited;
    }

    public boolean getDeclined(){
        return this.declined;
    }

    public void setInGame(boolean inGame){
        this.inGame = inGame;
    }

    public void setInvited(boolean invited){
        this.invited = invited;
    }

    public void setDeclined(boolean declined){
        this.declined = declined;
    }
}
