package com.motomutterers.boardgames.skullking.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.motomutterers.boardgames.skullking.dto.SkullKingStateResponse;
import com.motomutterers.boardgames.skullking.dto.SubmitBidRequest;
import com.motomutterers.boardgames.skullking.dto.SubmitBonusPointsRequest;
import com.motomutterers.boardgames.skullking.dto.SubmitTrickResultRequest;
import com.motomutterers.boardgames.skullking.services.SkullKingService;

@RestController
@RequestMapping("/api/skull-king/{roomName}")
public class SkullKingController {
    private final SkullKingService skullKingService;

    public SkullKingController(
        SkullKingService skullKingService
    ){
        this.skullKingService = skullKingService;
    }

    @GetMapping("/state")
    public ResponseEntity<SkullKingStateResponse> getState(
        @PathVariable String roomName,
        Authentication authentication
    ){
        return ResponseEntity.ok(skullKingService.getState(roomName, authentication));
    }

    @PostMapping("/bids")
    public ResponseEntity<Void> submitBid(
        @PathVariable String roomName,
        @RequestBody SubmitBidRequest request,
        Authentication authentication
    ){
        skullKingService.submitBid(roomName, request, authentication);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/trick-results")
    public ResponseEntity<Void> submitTrickResult(
        @PathVariable String roomName,
        @RequestBody SubmitTrickResultRequest request,
        Authentication authentication
    ){
        skullKingService.submitTrickResult(roomName, request, authentication);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/start-round")
    public ResponseEntity<Void> startRound(
        @PathVariable String roomName,
        Authentication authentication
    ){
        skullKingService.startRound(roomName, authentication);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/start-trick-results")
    public ResponseEntity<Void> startTrickResults(
        @PathVariable String roomName,
        Authentication authentication
    ){
        skullKingService.startTrickResults(roomName, authentication);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/start-bonus-points")
    public ResponseEntity<Void> startBonusPoints(
        @PathVariable String roomName,
        Authentication authentication
    ){
        skullKingService.startBonusPoints(roomName, authentication);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/bonus-points")
    public ResponseEntity<Void> submitBonusPoints(
        @PathVariable String roomName,
        @RequestBody SubmitBonusPointsRequest request,
        Authentication authentication
    ){
        skullKingService.submitBonusPoints(roomName, request, authentication);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/finish-round")
    public ResponseEntity<Void> finishRound(
        @PathVariable String roomName,
        Authentication authentication
    ){
        skullKingService.finishRound(roomName, authentication);
        return ResponseEntity.ok().build();
    }
}
