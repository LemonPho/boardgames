package com.motomutterers.boardgames.games.services;

import com.motomutterers.boardgames.games.dto.GameResponse;
import com.motomutterers.boardgames.games.dto.SimpleGameResponse;
import com.motomutterers.boardgames.games.exceptions.GameNotFoundException;
import com.motomutterers.boardgames.games.model.Game;
import com.motomutterers.boardgames.games.repository.GameRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GameServiceTest {

    @Mock private GameRepository gameRepository;

    @InjectMocks
    private GameService gameService;

    // helpers
    private Game game(String name) {
        Game game = new Game();
        game.setName(name);
        game.setType("rounds");
        game.setMinPlayers(2);
        game.setMaxPlayers(8);
        game.setDescription("desc");
        return game;
    }

    // --- getGameByName ---

    @Test
    void getGameByName_exists_returns() {
        Game game = game("Skull King");
        when(gameRepository.findByName("Skull King")).thenReturn(Optional.of(game));

        assertEquals(game, gameService.getGameByName("Skull King"));
    }

    @Test
    void getGameByName_missing_throws() {
        when(gameRepository.findByName("Nope")).thenReturn(Optional.empty());

        assertThrows(GameNotFoundException.class, () -> gameService.getGameByName("Nope"));
    }

    // --- getAllGames ---

    @Test
    void getAllGames_mapsToSimpleResponses() {
        when(gameRepository.findAll()).thenReturn(List.of(game("Skull King"), game("Wingspan")));

        List<SimpleGameResponse> result = gameService.getAllGames();

        assertEquals(2, result.size());
        assertEquals("Skull King", result.get(0).getName());
        assertEquals("Wingspan", result.get(1).getName());
    }

    @Test
    void getAllGames_empty_returnsEmptyList() {
        when(gameRepository.findAll()).thenReturn(List.of());

        assertTrue(gameService.getAllGames().isEmpty());
    }

    // --- getGame ---

    @Test
    void getGame_exists_returnsResponse() {
        when(gameRepository.findByName("Skull King")).thenReturn(Optional.of(game("Skull King")));

        GameResponse response = gameService.getGame("Skull King");

        assertEquals("Skull King", response.getName());
        assertEquals(8, response.getMaxPlayers());
    }

    @Test
    void getGame_missing_throws() {
        when(gameRepository.findByName("Nope")).thenReturn(Optional.empty());

        assertThrows(GameNotFoundException.class, () -> gameService.getGame("Nope"));
    }
}
