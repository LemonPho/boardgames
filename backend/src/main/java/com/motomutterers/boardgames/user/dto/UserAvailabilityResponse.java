package com.motomutterers.boardgames.user.dto;

import com.motomutterers.boardgames.user.model.User;

public class UserAvailabilityResponse extends UserResponse {
    private boolean inGame;

    public UserAvailabilityResponse(User user, boolean inGame){
        this.setEmail(user.getEmail());
        this.setUsername(user.getUsername());
        this.inGame = inGame;
    }

    public boolean getInGame(){
        return this.inGame;
    }

    public void setInGame(boolean inGame){
        this.inGame = inGame;
    }
}
