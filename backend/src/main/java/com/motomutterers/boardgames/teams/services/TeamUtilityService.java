package com.motomutterers.boardgames.teams.services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.motomutterers.boardgames.rooms.model.Room.RoomUser;
import com.motomutterers.boardgames.sessions.models.session.Session;
import com.motomutterers.boardgames.teams.models.Team;
import com.motomutterers.boardgames.teams.models.TeamUser;
import com.motomutterers.boardgames.teams.repositories.TeamRepository;
import com.motomutterers.boardgames.teams.repositories.TeamUserRepository;

import jakarta.transaction.Transactional;

@Service
public class TeamUtilityService {
    private final TeamRepository teamRepository;
    private final TeamUserRepository teamUserRepository;

    public TeamUtilityService(
            TeamRepository teamRepository,
            TeamUserRepository teamUserRepository) {
        this.teamRepository = teamRepository;
        this.teamUserRepository = teamUserRepository;
    }

    @Transactional
    public Team createTeam(List<RoomUser> users, Session session, String name) {
        Team team = new Team(session, name);
        teamRepository.save(team);

        List<TeamUser> teamUsers = users.stream()
                .map(user -> new TeamUser(team, user))
                .collect(Collectors.toList());
        teamUserRepository.saveAll(teamUsers);

        team.setTeamUsers(teamUsers);

        return team;
    }
}
