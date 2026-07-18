package com.motomutterers.boardgames.user.controllers;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.motomutterers.boardgames.auth.exceptions.UserUnauthorizedException;
import com.motomutterers.boardgames.auth.services.AuthService;
import com.motomutterers.boardgames.user.dto.MatchHistoryResponse;
import com.motomutterers.boardgames.user.dto.UpdateEmailRequest;
import com.motomutterers.boardgames.user.dto.UpdatePasswordRequest;
import com.motomutterers.boardgames.user.dto.UpdateUsernameRequest;
import com.motomutterers.boardgames.user.dto.UserResponse;
import com.motomutterers.boardgames.user.model.User;
import com.motomutterers.boardgames.user.services.MatchHistoryService;
import com.motomutterers.boardgames.user.services.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final MatchHistoryService matchHistoryService;
    private final AuthService authService;

    public UserController(
        UserService userService,
        MatchHistoryService matchHistoryService,
        AuthService authService
    ) {
        this.userService = userService;
        this.matchHistoryService = matchHistoryService;
        this.authService = authService;
    }

    @GetMapping("/current")
    public ResponseEntity<UserResponse> getAuthenticatedUserResponse(
        Authentication authentication
    ){
        return ResponseEntity.ok(userService.getAuthenticatedUserResponse(authentication));
    } 

    @GetMapping("/username/match-all")
    public ResponseEntity<List<UserResponse>> matchAllByUsername(
        @RequestParam String username
    ){
        return ResponseEntity.ok(userService.matchAllByUsername(username));
    }

    // --- account settings (only the account owner may mutate their own account) ---

    @PutMapping("/{username}/username")
    public ResponseEntity<Void> updateUsername(
        @PathVariable String username,
        @Valid @RequestBody UpdateUsernameRequest request,
        Authentication authentication
    ){
        UUID userId = requireSelf(username, authentication);
        userService.updateUsername(userId, request.getUsername());
        return ResponseEntity.noContent().build();
    }

    // Doesn't change the email directly — sends a confirmation link to the new
    // address, which applies the change when clicked.
    @PutMapping("/{username}/email")
    public ResponseEntity<String> updateEmail(
        @PathVariable String username,
        @Valid @RequestBody UpdateEmailRequest request,
        Authentication authentication
    ){
        requireSelf(username, authentication);
        User user = userService.getAuthenticatedUser(authentication);
        authService.requestEmailChange(user, request.getEmail(), request.getCurrentPassword());
        return ResponseEntity.ok("Verification sent to your new email");
    }

    @PutMapping("/{username}/password")
    public ResponseEntity<Void> updatePassword(
        @PathVariable String username,
        @Valid @RequestBody UpdatePasswordRequest request,
        Authentication authentication
    ){
        UUID userId = requireSelf(username, authentication);
        userService.updatePassword(userId, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<Void> deleteAccount(
        @PathVariable String username,
        Authentication authentication
    ){
        UUID userId = requireSelf(username, authentication);
        userService.deleteUser(userId, userId);
        return ResponseEntity.noContent().build();
    }

    // Resolves the authenticated user and confirms they own the path account,
    // returning their id. Blocks editing anyone else's account.
    private UUID requireSelf(String username, Authentication authentication){
        User authUser = userService.getAuthenticatedUser(authentication);
        if(!authUser.getUsername().equals(username)){
            throw new UserUnauthorizedException("You can only modify your own account");
        }
        return authUser.getId();
    }

    // Public profile lookup by exact username, backing the /profile/:username page.
    @GetMapping("/{username}")
    public ResponseEntity<UserResponse> getUserByUsername(
        @PathVariable String username
    ){
        return ResponseEntity.ok(userService.getUserResponseByUsername(username));
    }

    // A user's completed matches, newest first, for the profile match history.
    @GetMapping("/{username}/matches")
    public ResponseEntity<List<MatchHistoryResponse>> getMatchHistory(
        @PathVariable String username
    ){
        return ResponseEntity.ok(matchHistoryService.getMatchHistory(username));
    }
}
