package com.motomutterers.boardgames.rooms.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.motomutterers.boardgames.rooms.dto.CreateRoomRequest;
import com.motomutterers.boardgames.rooms.dto.RoomInvitationRequest;
import com.motomutterers.boardgames.rooms.dto.RoomResponse;
import com.motomutterers.boardgames.rooms.services.RoomService;
import com.motomutterers.boardgames.user.dto.UserAvailabilityResponse;

import jakarta.websocket.server.PathParam;

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

    @GetMapping("/search-users")
    public ResponseEntity<List<UserAvailabilityResponse>> searchUsersAvailability(
        @RequestParam String username,
        @RequestParam String roomName
    ) {
        return ResponseEntity.ok(roomService.searchUsersAvailability(username, roomName));
    }

    @PostMapping("/invite")
    public ResponseEntity<String> invitePlayer(
        @RequestBody RoomInvitationRequest request
    ) {
        return ResponseEntity.ok(roomService.invitePlayer(request));
    }

    @PutMapping("/accept")
    public ResponseEntity<String> acceptInvite(
        @RequestParam String token,
        Authentication authentication
    ) {
        return ResponseEntity.ok(roomService.acceptInvite(token, authentication));
    }

    @GetMapping("/{name}")
    public ResponseEntity<RoomResponse> getRoom(
        @PathVariable String name
    ) {
        RoomResponse roomResponse = roomService.getRoom(name);
        return ResponseEntity.ok(roomResponse);
    }   
}
