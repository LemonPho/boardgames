package com.motomutterers.boardgames.teams.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.motomutterers.boardgames.sessions.models.session.SessionStatus;
import com.motomutterers.boardgames.teams.models.Team;
import com.motomutterers.boardgames.user.model.User;

public interface TeamRepository extends JpaRepository<Team, UUID> {

    // A user's teams in completed sessions — the basis for match history. Newest
    // first. Joins through the team's room-user to the user and up to the
    // session/room/game so building each row doesn't trigger lazy loads.
    @Query("SELECT t FROM Team t "
        + "JOIN t.roomUser ru "
        + "JOIN t.session s "
        + "WHERE ru.user = :user AND s.status = :status "
        + "ORDER BY s.endedAt DESC, s.createdAt DESC")
    List<Team> findCompletedTeamsByUser(@Param("user") User user, @Param("status") SessionStatus status);

    // All teams in a session, used to rank placement within that match.
    List<Team> findBySession(com.motomutterers.boardgames.sessions.models.session.Session session);
}
