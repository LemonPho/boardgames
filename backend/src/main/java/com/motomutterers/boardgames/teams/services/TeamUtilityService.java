package com.motomutterers.boardgames.teams.services;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.motomutterers.boardgames.rooms.model.Room.RoomUser;
import com.motomutterers.boardgames.rooms.repository.RoomUserRepository;
import com.motomutterers.boardgames.sessions.models.session.Session;
import com.motomutterers.boardgames.teams.exceptions.TeamNotFoundException;
import com.motomutterers.boardgames.teams.models.Team;
import com.motomutterers.boardgames.teams.repositories.TeamRepository;

import jakarta.transaction.Transactional;

@Service
public class TeamUtilityService {
    private final TeamRepository teamRepository;
    private final RoomUserRepository roomUserRepository;

    public TeamUtilityService(
        TeamRepository teamRepository,
        RoomUserRepository roomUserRepository
    ) {
        this.teamRepository = teamRepository;
        this.roomUserRepository = roomUserRepository;
    }

    @Transactional
    public Team createTeam(RoomUser roomUser, Session session) {
        Team team = new Team(session);
        teamRepository.save(team);

        roomUser.setTeam(team);
        roomUserRepository.save(roomUser);

        return team;
    }

    public Team getOrThrowTeamById(String teamId){
        return teamRepository.findById(UUID.fromString(teamId))
            .orElseThrow(() -> new TeamNotFoundException("Team with id: " + teamId + " not found."));
    }

    /**
     * A session's teams in stable display/turn order: by each player's seat
     * position (set at room setup, rewritten when the admin reorders players).
     * This is the single ordering used for the first-round leader, round rotation,
     * and the per-user game state, so display and gameplay can't diverge.
     */
    public List<Team> orderedTeams(Session session){
        return session.getTeams().stream()
            .sorted(Comparator.comparingInt(
                t -> t.getRoomUser() != null ? t.getRoomUser().getPlayingPosition() : Integer.MAX_VALUE))
            .toList();
    }

    @Transactional
    public void setScore(Team team, long score){
        team.setFinalScore(score);
        teamRepository.save(team);
    }
}
