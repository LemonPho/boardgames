package com.motomutterers.boardgames.sessions.models.teamsessionevent;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.motomutterers.boardgames.sessions.models.session.Session;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEvent;
import com.motomutterers.boardgames.teams.models.Team;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "team_session_events")
public class TeamSessionEvent {
    @GeneratedValue
    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @ManyToOne
    @JoinColumn(name = "session_event_id", nullable = false)
    private SessionEvent sessionEvent;

    @ManyToOne
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne
    @JoinColumn(name = "corrects_team_session_event_id")
    private TeamSessionEvent correctsEvent;

    @OneToMany(mappedBy = "correctsEvent")
    private List<TeamSessionEvent> corrections = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private TeamSessionEventType type;

    private int sequence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String payload;

    private LocalDateTime createdAt;

    public TeamSessionEvent(){}

    public TeamSessionEvent(
        Session session,
        SessionEvent sessionEvent,
        Team team,
        TeamSessionEventType type,
        int sequence,
        String payload
    ){
        this.session = session;
        this.sessionEvent = sessionEvent;
        this.team = team;
        this.type = type;
        this.sequence = sequence;
        this.payload = payload;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId(){return this.id;}
    public Session getSession(){return this.session;}
    public SessionEvent getSessionEvent(){return this.sessionEvent;}
    public Team getTeam(){return this.team;}
    public TeamSessionEvent getCorrectsEvent(){return this.correctsEvent;}
    public TeamSessionEventType getType(){return this.type;}
    public int getSequence(){return this.sequence;}
    public String getPayload(){return this.payload;}
    public LocalDateTime getCreatedAt(){return this.createdAt;}

    public void setSession(Session session){this.session = session;}
    public void setSessionEvent(SessionEvent sessionEvent){this.sessionEvent = sessionEvent;}
    public void setTeam(Team team){this.team = team;}
    public void setType(TeamSessionEventType type){this.type = type;}
    public void setSequence(int sequence){this.sequence = sequence;}
    public void setPayload(String payload){this.payload = payload;}
}
