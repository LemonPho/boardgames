package com.motomutterers.boardgames.sessions.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.motomutterers.boardgames.sessions.dto.teamsessionevent.CreateTeamSessionEventRequest;
import com.motomutterers.boardgames.sessions.services.TeamSessionEventService;

@RestController
@RequestMapping("/api/team-session-event")
public class TeamSessionEventController {
    private final TeamSessionEventService teamSessionEventService;

    public TeamSessionEventController(
        TeamSessionEventService teamSessionEventService
    ){
        this.teamSessionEventService = teamSessionEventService;
    }

    @PostMapping("/{sessionId}")
    public ResponseEntity<Void> createTeamSessionEvent(
        @PathVariable String sessionId,
        CreateTeamSessionEventRequest request,
        Authentication authentication
    ){
        teamSessionEventService.createTeamSessionEvent(sessionId, request, authentication);
        return ResponseEntity.ok().build();
    }
}
