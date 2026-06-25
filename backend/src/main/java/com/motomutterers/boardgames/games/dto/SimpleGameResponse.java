package com.motomutterers.boardgames.games.dto;

import com.motomutterers.boardgames.games.model.Game;

public class SimpleGameResponse {
    private String name;
    private String type;
    private int minPlayers;
    private int maxPlayers;
    private String description;

    public SimpleGameResponse(){}

    public SimpleGameResponse(Game game){
        this.name = game.getName();
        this.type = game.getType();
        this.minPlayers = game.getMinPlayers();
        this.maxPlayers = game.getMaxPlayers();
        this.description = game.getDescription();
    }

    public String getName(){
        return name;
    }

    public String getType(){
        return type;
    }

    public int getMinPlayers(){
        return minPlayers;
    }

    public int getMaxPlayers(){
        return maxPlayers;
    }

    public String getDescription(){
        return description;
    }

    public void setName(String name){
        if(name.isBlank()) return;
        this.name = name;
    }

    public void setType(String type){
        if(type.isBlank()) return;
        this.type = type;
    }

    public void setMinPlayers(int minPlayers){
        if(minPlayers < 1) return;
        this.minPlayers = minPlayers;
    }

    public void setMaxPlayers(int maxPlayers){
        if(maxPlayers < 1) return;
        this.maxPlayers = maxPlayers;
    }

    public void setDescription(String description){
        this.description = description;
    }
}
