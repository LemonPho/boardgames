package com.motomutterers.boardgames.notifications.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.motomutterers.boardgames.exceptions.BadActionException;
import com.motomutterers.boardgames.notifications.dto.CreateNotificationRequest;
import com.motomutterers.boardgames.notifications.dto.CreateRoomInvitationNotificationRequest;
import com.motomutterers.boardgames.notifications.dto.NotificationResponse;
import com.motomutterers.boardgames.notifications.model.Notification;
import com.motomutterers.boardgames.notifications.model.NotificationType;
import com.motomutterers.boardgames.notifications.repositories.NotificationRepository;
import com.motomutterers.boardgames.rooms.dto.SimpleRoomResponse;
import com.motomutterers.boardgames.user.dto.UserResponse;
import com.motomutterers.boardgames.user.model.User;
import com.motomutterers.boardgames.user.services.UserService;

import tools.jackson.databind.ObjectMapper;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserService userService;

    public NotificationService(
        NotificationRepository notificationRepository,
        UserService userService
    ) {
        this.notificationRepository = notificationRepository;
        this.userService = userService;
    }

    public void readNotification(){

    }

    public List<NotificationResponse> getNotifications(Authentication authentication){
        User user = userService.getAuthenticatedUser(authentication);
        List<NotificationResponse> notifications = notificationRepository.findNotificationsByUser(user)
            .stream()
            .map(NotificationResponse::new)
            .collect(Collectors.toList());
        return notifications;
    }

    public NotificationResponse createRoomInvitationNotification(CreateRoomInvitationNotificationRequest request){
        if(request.getUser().getId() == request.getRoomAdmin().getId()){
            throw new BadActionException("You cannot send a notification to yourself");
        }
        
        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("room", new SimpleRoomResponse(request.getRoom()));
        payloadMap.put("roomAdmin", new UserResponse(request.getRoomAdmin()));
        ObjectMapper mapper = new ObjectMapper();
        String payload = mapper.writeValueAsString(payloadMap);

        CreateNotificationRequest createNotificationRequest = new CreateNotificationRequest(
            request.getUser(),
            NotificationType.ROOM_INVITATION, 
            payload);

        return createNotification(createNotificationRequest);
    }

    public NotificationResponse createNotification(CreateNotificationRequest request){
        Notification notification = new Notification(request.getUser(), request.getType(), request.getPayload());
        notificationRepository.save(notification);
        NotificationResponse response = new NotificationResponse(notification);

        return response;
    }
}
