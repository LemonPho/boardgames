package com.motomutterers.boardgames.sessions.models.session;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.teams.models.Team;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "sessions")
public class Session {
    @GeneratedValue
    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @OneToMany(mappedBy = "session", fetch = FetchType.LAZY)
    List<Team> teams = new ArrayList<Team>();

    @Enumerated(EnumType.STRING)
    private SessionStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime endedAt;

    public Session(){}

    public Session(Room room){
        this.room = room;
        this.status = SessionStatus.IN_PROGRESS;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId(){return this.id;}
    public Room getRoom(){return this.room;}
    public List<Team> getTeams(){return this.teams;}
    public SessionStatus getStatus(){return this.status;}
    public LocalDateTime getCreatedAt(){return this.createdAt;}
    public LocalDateTime getEndedAt(){return this.endedAt;}

    public void setRoom(Room room){this.room = room;}
    public void addTeam(Team team){this.teams.add(team);}
    public void removeTeam(int index){this.teams.remove(index);}
    public void setStatus(SessionStatus status){this.status = status;}
    public void setCreatedAt(LocalDateTime createdAt){this.createdAt = createdAt;}
    public void setEndedAt(LocalDateTime endedAt){this.endedAt = endedAt;}
}
