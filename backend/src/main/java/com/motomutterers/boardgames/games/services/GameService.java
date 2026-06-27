package com.motomutterers.boardgames.games.services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.motomutterers.boardgames.games.dto.GameResponse;
import com.motomutterers.boardgames.games.dto.SimpleGameResponse;
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

    public List<SimpleGameResponse> getAllGames(){
        List<Game> games = gameRepository.findAll();
        List<SimpleGameResponse> gamesResponse = 
            games
                .stream()
                .map(SimpleGameResponse::new)
                .collect(Collectors.toList()); 

        return gamesResponse;
    }

    public GameResponse getGame(String name){
        Game game = getGameByName(name);
        GameResponse response = new GameResponse(game);

        return response;
    }
}
