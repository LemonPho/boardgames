package com.motomutterers.boardgames.teams.models;

import java.time.LocalDateTime;
import java.util.UUID;

import com.motomutterers.boardgames.sessions.models.Session;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "teams")
public class Team {
    @GeneratedValue
    @Id
    private UUID id;
    
    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    private String name;
    private long finalScore;
    private boolean winner;
    private LocalDateTime createdAt;

    public Team(){}
    public Team(
        Session session,
        String name
    ) {
        this.session = session;
        this.name = name;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId(){return this.id;}
    public Session getSession(){return this.session;}
    public String getName(){return this.name;}
    public long getFinalScore(){return this.finalScore;}
    public boolean getWinner(){return this.winner;}
    public LocalDateTime getCreatedAt(){return this.createdAt;}

    public void setSession(Session session){this.session = session;}
    public void setName(String name){this.name = name;}
    public void setFinalScore(long finalScore){this.finalScore = finalScore;}
    public void setWinner(boolean winner){this.winner = winner;}
    public void setCreatedAt(LocalDateTime createdAt){this.createdAt = createdAt;}
}
