package com.motomutterers.boardgames.teams.exceptions;

public class TeamNotFoundException extends RuntimeException {
    public TeamNotFoundException(String message){
        super(message);
    }
}
