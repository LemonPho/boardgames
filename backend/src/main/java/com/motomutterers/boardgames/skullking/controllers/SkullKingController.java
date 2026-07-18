package com.motomutterers.boardgames.skullking.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.motomutterers.boardgames.skullking.dto.CorrectBidsRequest;
import com.motomutterers.boardgames.skullking.dto.CorrectBonusRequest;
import com.motomutterers.boardgames.skullking.dto.CorrectTricksRequest;
import com.motomutterers.boardgames.skullking.dto.RoundHistoryResponse;
import com.motomutterers.boardgames.skullking.dto.ScoreboardResponse;
import com.motomutterers.boardgames.skullking.dto.SetKrakenRequest;
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

    @GetMapping("/scoreboard")
    public ResponseEntity<ScoreboardResponse> getScoreboard(
        @PathVariable String roomName,
        Authentication authentication
    ){
        return ResponseEntity.ok(skullKingService.getScoreboard(roomName, authentication));
    }

    @GetMapping("/rounds/{round}")
    public ResponseEntity<RoundHistoryResponse> getRoundHistory(
        @PathVariable String roomName,
        @PathVariable int round,
        Authentication authentication
    ){
        return ResponseEntity.ok(skullKingService.getRoundHistory(roomName, round, authentication));
    }

    @PostMapping("/rounds/{round}/bids")
    public ResponseEntity<Void> correctBids(
        @PathVariable String roomName,
        @PathVariable int round,
        @RequestBody CorrectBidsRequest request,
        Authentication authentication
    ){
        skullKingService.correctBids(roomName, round, request, authentication);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/rounds/{round}/tricks")
    public ResponseEntity<Void> correctTricks(
        @PathVariable String roomName,
        @PathVariable int round,
        @RequestBody CorrectTricksRequest request,
        Authentication authentication
    ){
        skullKingService.correctTricks(roomName, round, request, authentication);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/rounds/{round}/bonus")
    public ResponseEntity<Void> correctBonus(
        @PathVariable String roomName,
        @PathVariable int round,
        @RequestBody CorrectBonusRequest request,
        Authentication authentication
    ){
        skullKingService.correctBonus(roomName, round, request, authentication);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/rounds/{round}/kraken")
    public ResponseEntity<Void> setKraken(
        @PathVariable String roomName,
        @PathVariable int round,
        @RequestBody SetKrakenRequest request,
        Authentication authentication
    ){
        skullKingService.setKraken(roomName, round, request.getKrakenPlayed(), authentication);
        return ResponseEntity.ok().build();
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
