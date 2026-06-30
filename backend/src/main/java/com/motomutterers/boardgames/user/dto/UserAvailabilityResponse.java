package com.motomutterers.boardgames.user.dto;

import com.motomutterers.boardgames.user.model.User;

public class UserAvailabilityResponse extends UserResponse {
    private boolean inGame;
    private boolean invited;

    public UserAvailabilityResponse(User user, boolean inGame, boolean invited){
        this.setEmail(user.getEmail());
        this.setUsername(user.getUsername());
        this.inGame = inGame;
        this.invited = invited;
    }

    public boolean getInGame(){
        return this.inGame;
    }

    public boolean getInvited(){
        return this.invited;
    }

    public void setInGame(boolean inGame){
        this.inGame = inGame;
    }

    public void setInvited(boolean invited){
        this.invited = invited;
    }
}
