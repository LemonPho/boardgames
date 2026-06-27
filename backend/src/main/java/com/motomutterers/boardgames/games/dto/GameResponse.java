package com.motomutterers.boardgames.games.dto;

import com.motomutterers.boardgames.games.model.Game;

public class GameResponse {
    private String name;
    private String type;
    private String gameConfig;
    private String scoringConfig;
    private int minPlayers;
    private int maxPlayers;
    private String description;

    public GameResponse(){}

    public GameResponse(Game game){
        this.name = game.getName();
        this.type = game.getType();
        this.gameConfig = game.getGameConfig();
        this.scoringConfig = game.getScoringConfig();
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

    public String getGameConfig(){
        return gameConfig;
    }

    public String getScoringConfig(){
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

    public void setGameConfig(String gameConfig){
        this.gameConfig = gameConfig;
    }

    public void setScoringConfig(String scoringConfig){
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
