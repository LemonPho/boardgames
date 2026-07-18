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
import com.motomutterers.boardgames.rooms.dto.RoomResponse;
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
    public ResponseEntity<Void> cancelRoom(
        @PathVariable String roomName,
        Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        roomService.cancelRoom(roomName, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{roomName}/leave")
    public ResponseEntity<Void> leaveRoom(
        @PathVariable String roomName,
        Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        roomService.leaveRoom(roomName, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{roomName}/search-users")
    public ResponseEntity<List<UserAvailabilityResponse>> searchUsersAvailability(
        @RequestParam String username,
        @PathVariable String roomName
    ) {
        return ResponseEntity.ok(roomService.searchUsersAvailability(username, roomName));
    }

    @PostMapping("/{roomName}/invite")
    public ResponseEntity<Void> invitePlayer(
        @RequestBody RoomInvitationRequest request,
        @PathVariable String roomName,
        Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        request.setRoomName(roomName);
        request.setAdminId(userId);
        roomService.invitePlayer(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("{roomName}/revoke-invite")
    public ResponseEntity<Void> revokeInvite(
        @RequestBody RoomInvitationRequest request,
        @PathVariable String roomName,
        Authentication authentication
    ) {
        UUID adminId = UUID.fromString(authentication.getName());
        request.setRoomName(roomName);
        request.setAdminId(adminId);
        roomService.revokeInvite(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomName}/create-anonymous")
    public ResponseEntity<Void> createAnonymousPlayer(
        @RequestBody CreateAnonymousPlayerRequest request,
        @PathVariable String roomName,
        Authentication authentication
    ) {
        UUID adminId = UUID.fromString(authentication.getName());
        request.setAdminId(adminId);
        request.setRoomName(roomName);
        roomService.createAnonymousPlayer(request);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomName}/remove-player")
    public ResponseEntity<Void> removeAnonymousPlayer(
        @RequestBody RemovePlayerRequest request,
        @PathVariable String roomName,
        Authentication authentication
    ) {
        UUID adminId = UUID.fromString(authentication.getName());
        request.setAdminId(adminId);
        request.setRoomName(roomName);
        roomService.removePlayer(request);

        return ResponseEntity.ok().build();
    }

    @PutMapping("/accept")
    public ResponseEntity<RoomResponse> acceptInvite(
        @RequestParam String token,
        Authentication authentication
    ) {
        return ResponseEntity.ok(roomService.acceptInvite(token, authentication));
    }

    @GetMapping("/active")
    public ResponseEntity<RoomResponse> getActiveRoom(
        Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        RoomResponse room = roomService.getActiveRoom(userId);
        // 204 when the user isn't in any room, so the client shows no rooms section.
        return room == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(room);
    }

    @GetMapping("/{name}")
    public ResponseEntity<RoomResponse> getRoom(
        @PathVariable String name
    ) {
        return ResponseEntity.ok(roomService.getRoom(name));
    }
}
