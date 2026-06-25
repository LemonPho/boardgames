package com.motomutterers.boardgames.games.services;

import org.springframework.stereotype.Service;

import com.motomutterers.boardgames.games.exceptions.GameNotFoundException;
import com.motomutterers.boardgames.games.model.Game;
import com.motomutterers.boardgames.games.repository.GameRepository;

@Service
public class GameService {
    private final GameRepository gameRepository;

    public GameService(
        GameRepository gameRepository
    ){
        this.gameRepository = gameRepository;
    }

    public Game getGameByName(String name){
        return gameRepository.findByName(name)
            .orElseThrow(() -> new GameNotFoundException("Game was not found"));
    }
}
