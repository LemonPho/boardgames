package com.motomutterers.boardgames.user.services;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.motomutterers.boardgames.sessions.models.session.Session;
import com.motomutterers.boardgames.sessions.models.session.SessionStatus;
import com.motomutterers.boardgames.teams.models.Team;
import com.motomutterers.boardgames.teams.repositories.TeamRepository;
import com.motomutterers.boardgames.user.dto.MatchHistoryResponse;
import com.motomutterers.boardgames.user.model.User;

@Service
public class MatchHistoryService {
    private final TeamRepository teamRepository;
    private final UserService userService;

    public MatchHistoryService(
        TeamRepository teamRepository,
        UserService userService
    ) {
        this.teamRepository = teamRepository;
        this.userService = userService;
    }

    /**
     * A user's completed matches, newest first. Each row places the user's team
     * within its session by final score (highest = 1st) and marks a win as 1st
     * place — derived from scores rather than the (currently unset) winner flag.
     */
    @Transactional(readOnly = true)
    public List<MatchHistoryResponse> getMatchHistory(String username){
        User user = userService.getUserByUsername(username);

        return teamRepository.findCompletedTeamsByUser(user, SessionStatus.COMPLETED).stream()
            .map(this::toMatchRow)
            .toList();
    }

    private MatchHistoryResponse toMatchRow(Team team){
        Session session = team.getSession();
        List<Team> sessionTeams = teamRepository.findBySession(session);

        int placement = placementOf(team, sessionTeams);
        int players = sessionTeams.size();

        return new MatchHistoryResponse(
            session.getId(),
            session.getRoom().getGame().getName(),
            session.getRoom().getName(),
            placement,
            players,
            team.getFinalScore(),
            placement == 1,
            session.getEndedAt() != null ? session.getEndedAt() : session.getCreatedAt()
        );
    }

    // 1-based rank of the team by final score, highest first. Ties share the
    // better rank (standard competition ranking).
    private int placementOf(Team team, List<Team> sessionTeams){
        List<Team> ranked = sessionTeams.stream()
            .sorted(Comparator.comparingLong(Team::getFinalScore).reversed())
            .toList();

        long higherScores = ranked.stream()
            .filter(t -> t.getFinalScore() > team.getFinalScore())
            .count();

        return (int) higherScores + 1;
    }
}
