package com.motomutterers.boardgames.sessions.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.motomutterers.boardgames.sessions.models.teamsessionevent.TeamSessionEvent;
import com.motomutterers.boardgames.sessions.models.session.Session;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEvent;
import com.motomutterers.boardgames.teams.models.Team;

public interface TeamSessionEventRepository extends JpaRepository<TeamSessionEvent, UUID>{
    int countBySession(Session session);
    int countBySessionEvent(SessionEvent sessionEvent);
    boolean existsBySessionEventAndTeam(SessionEvent sessionEvent, Team team);
    List<TeamSessionEvent> findBySessionEventOrderBySequenceAsc(SessionEvent sessionEvent);
}
