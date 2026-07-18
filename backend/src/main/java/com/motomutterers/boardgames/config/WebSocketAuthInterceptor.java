package com.motomutterers.boardgames.config;

import java.util.UUID;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import com.motomutterers.boardgames.auth.services.JwtService;
import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.rooms.model.Room.RoomUser;
import com.motomutterers.boardgames.rooms.model.Room.RoomUserRoles;
import com.motomutterers.boardgames.rooms.repository.RoomUserRepository;
import com.motomutterers.boardgames.rooms.services.RoomsUtilityService;
import com.motomutterers.boardgames.teams.models.Team;
import com.motomutterers.boardgames.user.UserRepository;
import com.motomutterers.boardgames.user.model.User;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RoomsUtilityService roomsUtilityService;
    private final RoomUserRepository roomUserRepository;

    public WebSocketAuthInterceptor(
        JwtService jwtService,
        UserRepository userRepository,
        RoomsUtilityService roomsUtilityService,
        RoomUserRepository roomUserRepository
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.roomsUtilityService = roomsUtilityService;
        this.roomUserRepository = roomUserRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            handleSubscribe(accessor);
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        if (!jwtService.isTokenValid(token)) {
            throw new IllegalArgumentException("Invalid token");
        }

        String userId = jwtService.extractUserId(token);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userId, null, null);
        accessor.setUser(auth);
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) return;

        // A user may only subscribe to their own personal notification topic,
        // which is keyed by their (unique) username.
        if (destination.startsWith("/topic/notifications/")) {
            UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) accessor.getUser();
            if (auth == null) {
                throw new IllegalArgumentException("Not authenticated");
            }
            UUID userId = UUID.fromString(auth.getName());
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

            String topicUsername = destination.substring("/topic/notifications/".length());
            if (!user.getUsername().equals(topicUsername)) {
                throw new IllegalArgumentException("You can only subscribe to your own notifications");
            }
            return;
        }

        // Public room topics don't need team-level auth
        if (destination.matches("/topic/rooms/.*")) return;

        // Session-level global events don't need team-level auth
        if (destination.matches("/topic/sessions/[^/]+") && !destination.contains("/teams/") && !destination.contains("/admin")) return;

        // Team-specific and admin subscriptions require auth
        if (destination.contains("/topic/sessions/") && (destination.contains("/teams/") || destination.contains("/admin"))) {
            UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) accessor.getUser();
            if (auth == null) {
                throw new IllegalArgumentException("Not authenticated");
            }

            UUID userId = UUID.fromString(auth.getName());
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

            String roomName = extractRoomName(destination);
            Room room = roomsUtilityService.getRoomByName(roomName);

            RoomUser roomUser = roomUserRepository.findByUserAndRoom(user, room)
                .orElseThrow(() -> new IllegalArgumentException("User not in room"));

            if (destination.contains("/admin")) {
                if (!roomUser.getRole().equals(RoomUserRoles.ADMIN)) {
                    throw new IllegalArgumentException("Only the room admin can subscribe to the admin topic");
                }
            } else if (destination.contains("/teams/")) {
                String teamId = extractTeamId(destination);
                Team team = roomUser.getTeam();
                if (team == null || !team.getId().toString().equals(teamId)) {
                    if (!roomUser.getRole().equals(RoomUserRoles.ADMIN)) {
                        throw new IllegalArgumentException("You can only subscribe to your own team's topic");
                    }
                }
            }
        }
    }

    private String extractRoomName(String destination) {
        // /topic/sessions/{roomName}/teams/{teamId} or /topic/sessions/{roomName}/admin
        String[] parts = destination.split("/");
        // parts: ["", "topic", "sessions", "{roomName}", ...]
        return parts[3];
    }

    private String extractTeamId(String destination) {
        // /topic/sessions/{roomName}/teams/{teamId}
        String[] parts = destination.split("/");
        // parts: ["", "topic", "sessions", "{roomName}", "teams", "{teamId}"]
        return parts[5];
    }
}
