package com.motomutterers.boardgames.user.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.motomutterers.boardgames.user.dto.UserAvailabilityResponse;
import com.motomutterers.boardgames.user.dto.UserResponse;
import com.motomutterers.boardgames.user.services.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(
        UserService userService
    ) {
        this.userService = userService;
    }

    @GetMapping("/current")
    public ResponseEntity<UserResponse> getAuthenticatedUser(
        Authentication authentication
    ){
        return ResponseEntity.ok(userService.getAuthenticatedUser(authentication));
    } 

    @GetMapping("/username/match-all")
    public ResponseEntity<List<UserResponse>> matchAllByUsername(
        @RequestParam String username
    ){
        return ResponseEntity.ok(userService.matchAllByUsername(username));
    }

    @GetMapping("/username/match-all-availability")
    public ResponseEntity<List<UserAvailabilityResponse>> matchAllByUsernameAvailability(
        @RequestParam String username
    ) {
        return ResponseEntity.ok(userService.matchAllByUsernameAvailability(username));
    }
}
