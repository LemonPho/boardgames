package com.motomutterers.boardgames.games.model;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.boot.jackson.autoconfigure.JacksonProperties.Json;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "games")
public class Game {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private Json gameConfig;

    @Column(nullable = false)
    private Json scoringConfig;

    @Column(nullable = false)
    private int minPlayers;

    @Column(nullable = false)
    private int maxPlayers;

    private String description;

    @Column(nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Game(){}
}
