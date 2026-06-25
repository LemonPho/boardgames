package com.motomutterers.boardgames.games.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.motomutterers.boardgames.games.model.Game;

public interface GameRepository extends JpaRepository<Game, UUID> {
    Optional<Game> findByName(String name);
}
