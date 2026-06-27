package com.motomutterers.boardgames.games.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.motomutterers.boardgames.games.dto.GameResponse;
import com.motomutterers.boardgames.games.dto.SimpleGameResponse;
import com.motomutterers.boardgames.games.services.GameService;

@RestController
@RequestMapping("/api/games")
public class GameController {
    private final GameService gameService;

    public GameController(GameService gameService){
        this.gameService = gameService;
    }

    @GetMapping("/all")
    public ResponseEntity<List<SimpleGameResponse>> getGames(){
        List<SimpleGameResponse> response = gameService.getAllGames();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{name}")
    public ResponseEntity<GameResponse> getGame(@PathVariable String name){
        GameResponse response = gameService.getGame(name);
        return ResponseEntity.ok(response);
    }
}
