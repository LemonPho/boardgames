package com.motomutterers.boardgames.teams.services;

import com.motomutterers.boardgames.games.model.Game;
import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.rooms.model.Room.RoomConfiguration;
import com.motomutterers.boardgames.rooms.model.Room.RoomUser;
import com.motomutterers.boardgames.rooms.model.Room.RoomUserRoles;
import com.motomutterers.boardgames.rooms.model.Room.TrackingMode;
import com.motomutterers.boardgames.rooms.repository.RoomUserRepository;
import com.motomutterers.boardgames.sessions.models.session.Session;
import com.motomutterers.boardgames.teams.exceptions.TeamNotFoundException;
import com.motomutterers.boardgames.teams.models.Team;
import com.motomutterers.boardgames.teams.repositories.TeamRepository;
import com.motomutterers.boardgames.user.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TeamUtilityServiceTest {

    @Mock private TeamRepository teamRepository;
    @Mock private RoomUserRepository roomUserRepository;

    @InjectMocks
    private TeamUtilityService teamUtilityService;

    // helpers
    private Session session() {
        Game game = new Game();
        game.setName("Skull King");
        Room room = new Room(game, "room", new RoomConfiguration(TrackingMode.ADMIN, false));
        return new Session(room);
    }

    private RoomUser roomUser(Room room) {
        User user = new User("test@test.com", "testuser", "hash");
        return new RoomUser(user, room, RoomUserRoles.PLAYER);
    }

    // --- createTeam ---

    @Test
    void createTeam_savesTeamAndLinksToRoomUser() {
        Session session = session();
        RoomUser player = roomUser(session.getRoom());

        Team result = teamUtilityService.createTeam(player, session);

        assertNotNull(result);
        assertEquals(session, result.getSession());
        // The team is persisted and the player is linked to it, then re-saved.
        verify(teamRepository).save(result);
        assertEquals(result, player.getTeam());
        verify(roomUserRepository).save(player);
    }

    // --- getOrThrowTeamById ---

    @Test
    void getOrThrowTeamById_exists_returns() {
        UUID id = UUID.randomUUID();
        Team team = new Team(session());
        when(teamRepository.findById(id)).thenReturn(Optional.of(team));

        assertEquals(team, teamUtilityService.getOrThrowTeamById(id.toString()));
    }

    @Test
    void getOrThrowTeamById_missing_throws() {
        UUID id = UUID.randomUUID();
        when(teamRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(TeamNotFoundException.class,
            () -> teamUtilityService.getOrThrowTeamById(id.toString()));
    }

    // --- setScore ---

    @Test
    void setScore_setsFinalScoreAndSaves() {
        Team team = new Team(session());

        teamUtilityService.setScore(team, 130);

        assertEquals(130, team.getFinalScore());
        verify(teamRepository).save(team);
    }

    @Test
    void setScore_overwritesPreviousScore() {
        Team team = new Team(session());
        team.setFinalScore(80);

        teamUtilityService.setScore(team, -20);

        // recomputeScores replays from scratch, so setScore must overwrite (not add).
        assertEquals(-20, team.getFinalScore());
        verify(teamRepository).save(team);
    }
}
