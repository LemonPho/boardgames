package com.motomutterers.boardgames.sessions.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.motomutterers.boardgames.sessions.dto.CreateSessionRequest;
import com.motomutterers.boardgames.sessions.dto.SessionResponse;
import com.motomutterers.boardgames.sessions.dto.SessionStateResponse;
import com.motomutterers.boardgames.sessions.services.SessionService;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {
    private final SessionService sessionService;

    public SessionController(
        SessionService sessionService
    ) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public ResponseEntity<SessionResponse> createSession(
        @RequestBody CreateSessionRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(sessionService.createSession(request, authentication));
    }

    @GetMapping("/{roomName}")
    public ResponseEntity<SessionStateResponse> getSessionState(
        @PathVariable String roomName
    ) {
        return ResponseEntity.ok(sessionService.getSessionState(roomName));
    }
}
