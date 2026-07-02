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

import com.motomutterers.boardgames.rooms.dto.CreateAnonymousPlayerRequest;
import com.motomutterers.boardgames.rooms.dto.CreateRoomRequest;
import com.motomutterers.boardgames.rooms.dto.RemovePlayerRequest;
import com.motomutterers.boardgames.rooms.dto.RoomInvitationRequest;
import com.motomutterers.boardgames.rooms.dto.RoomInvitationResponse;
import com.motomutterers.boardgames.rooms.dto.RoomResponse;
import com.motomutterers.boardgames.rooms.dto.RoomUserResponse;
import com.motomutterers.boardgames.rooms.model.Room.RoomUser;
import com.motomutterers.boardgames.rooms.services.RoomService;
import com.motomutterers.boardgames.user.dto.UserAvailabilityResponse;

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

    @PutMapping("/{roomName}/cancel")
    public ResponseEntity<String> cancelRoom(
        @PathVariable String roomName,
        Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(roomService.cancelRoom(roomName, userId));
    }

    @GetMapping("/{roomName}/search-users")
    public ResponseEntity<List<UserAvailabilityResponse>> searchUsersAvailability(
        @RequestParam String username,
        @PathVariable String roomName
    ) {
        return ResponseEntity.ok(roomService.searchUsersAvailability(username, roomName));
    }

    @PostMapping("/{roomName}/invite")
    public ResponseEntity<RoomInvitationResponse> invitePlayer(
        @RequestBody RoomInvitationRequest request,
        @PathVariable String roomName,
        Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        request.setRoomName(roomName);
        request.setAdminId(userId);
        return ResponseEntity.ok(roomService.invitePlayer(request));
    }

    @PostMapping("{roomName}/revoke-invite")
    public ResponseEntity<List<RoomInvitationResponse>> revokeInvite(
        @RequestBody RoomInvitationRequest request,
        @PathVariable String roomName,
        Authentication authentication
    ) {
        UUID adminId = UUID.fromString(authentication.getName());
        request.setRoomName(roomName);
        request.setAdminId(adminId);
        return ResponseEntity.ok(roomService.revokeInvite(request));
    }

    @PostMapping("/{roomName}/create-anonymous")
    public ResponseEntity<RoomUserResponse> createAnonymousPlayer(
        @RequestBody CreateAnonymousPlayerRequest request,
        @PathVariable String roomName,
        Authentication authentication
    ) {
        UUID adminId = UUID.fromString(authentication.getName());
        request.setAdminId(adminId);
        request.setRoomName(roomName);

        return ResponseEntity.ok(roomService.createAnonymousPlayer(request));
    }

    @PostMapping("/{roomName}/remove-player")
    public ResponseEntity<List<RoomUserResponse>> removeAnonymousPlayer(
        @RequestBody RemovePlayerRequest request,
        @PathVariable String roomName,
        Authentication authentication
    ) {
        UUID adminId = UUID.fromString(authentication.getName());
        request.setAdminId(adminId);
        request.setRoomName(roomName);

        return ResponseEntity.ok(roomService.removePlayer(request));
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
