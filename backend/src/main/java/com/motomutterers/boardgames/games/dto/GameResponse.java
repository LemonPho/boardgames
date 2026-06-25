package com.motomutterers.boardgames.games.dto;

import org.springframework.boot.jackson.autoconfigure.JacksonProperties.Json;

public class GameResponse {
    private String name;
    private String type;
    private Json gameConfig;
    private Json scoringConfig;
    private int minPlayers;
    private int maxPlayers;
    private String description;

    public String getName(){
        return name;
    }

    public String getType(){
        return type;
    }

    public Json getGameConfig(){
        return gameConfig;
    }

    public Json getScoringConfig(){
        return scoringConfig;
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

    public void setGameConfig(Json gameConfig){
        this.gameConfig = gameConfig;
    }

    public void setScoringConfig(Json scoringConfig){
        this.scoringConfig = scoringConfig;
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
