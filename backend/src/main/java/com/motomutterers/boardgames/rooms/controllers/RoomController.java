package com.motomutterers.boardgames.rooms.controllers;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.motomutterers.boardgames.rooms.dto.CreateRoomRequest;
import com.motomutterers.boardgames.rooms.dto.RoomInvitationRequest;
import com.motomutterers.boardgames.rooms.dto.RoomResponse;
import com.motomutterers.boardgames.rooms.services.RoomService;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {
    private final RoomService roomService;

    public RoomController(
        RoomService roomService
    ) {
        this.roomService = roomService;
    }

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(
        @RequestBody CreateRoomRequest request,
        Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        RoomResponse roomResponse = roomService.createRoom(request, userId);
        return ResponseEntity.ok(roomResponse);
    }

    @PostMapping("/invite")
    public ResponseEntity<String> invitePlayer(
        @RequestBody RoomInvitationRequest request
    ) {
        return ResponseEntity.ok(roomService.invitePlayer(request));
    }

    @GetMapping("/{name}")
    public ResponseEntity<RoomResponse> getRoom(
        @PathVariable String name
    ) {
        RoomResponse roomResponse = roomService.getRoom(name);
        return ResponseEntity.ok(roomResponse);
    }   
}
