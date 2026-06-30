package com.motomutterers.boardgames.exceptions;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.motomutterers.boardgames.auth.exceptions.PasswordIncorrectException;
import com.motomutterers.boardgames.auth.exceptions.RefreshTokenExpiredException;
import com.motomutterers.boardgames.auth.exceptions.RefreshTokenNotFoundException;
import com.motomutterers.boardgames.auth.exceptions.UserUnauthorizedException;
import com.motomutterers.boardgames.auth.exceptions.VerificationTokenExpiredException;
import com.motomutterers.boardgames.auth.exceptions.VerificationTokenNotFoundException;
import com.motomutterers.boardgames.games.exceptions.GameNotFoundException;
import com.motomutterers.boardgames.rooms.exceptions.RoomNotFoundException;
import com.motomutterers.boardgames.user.exceptions.UserInActiveRoomException;
import com.motomutterers.boardgames.user.exceptions.UserNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, String>> handleValidation(ValidationException e) {
        return ResponseEntity.status(400).body(e.getErrors());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.status(400).body(errors);
    }

    @ExceptionHandler(PasswordIncorrectException.class)
    public ResponseEntity<String> handleIncorrectPassword(PasswordIncorrectException e){
        return ResponseEntity.status(400).body(e.getMessage());
    }

    @ExceptionHandler(UserUnauthorizedException.class)
    public ResponseEntity<String> handleUserUnauthorized(UserUnauthorizedException e){
        return ResponseEntity.status(403).body(e.getMessage());
    }

    @ExceptionHandler(BadActionException.class)
    public ResponseEntity<String> handleBadAction(BadActionException e){
        return ResponseEntity.status(400).body(e.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<String> handleUserNotFound(UserNotFoundException e) {
        return ResponseEntity.status(404).body(e.getMessage());
    }

    @ExceptionHandler(RefreshTokenExpiredException.class)
    public ResponseEntity<String> handleRefreshTokenExpired(RefreshTokenExpiredException e){
        return ResponseEntity.status(403).body(e.getMessage());
    }

    @ExceptionHandler(RefreshTokenNotFoundException.class)
    public ResponseEntity<String> handleRefreshTokenNotFound(RefreshTokenNotFoundException e){
        return ResponseEntity.status(404).body(e.getMessage());
    }

    @ExceptionHandler(VerificationTokenNotFoundException.class)
    public ResponseEntity<String> handleVerificationTokenNotFound(VerificationTokenNotFoundException e){
        return ResponseEntity.status(404).body(e.getMessage());
    }

    @ExceptionHandler(VerificationTokenExpiredException.class)
    public ResponseEntity<String> handleVerificationTokenExpiredException(VerificationTokenExpiredException e){
        return ResponseEntity.status(404).body(e.getMessage());
    }

    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<String> handleGameNotFoundException(GameNotFoundException e){
        return ResponseEntity.status(404).body(e.getMessage());
    }

    @ExceptionHandler(RoomNotFoundException.class)
    public ResponseEntity<String> handleRoomNotFoundException(RoomNotFoundException e){
        return ResponseEntity.status(404).body(e.getMessage());
    }

    @ExceptionHandler(UserInActiveRoomException.class)
    public ResponseEntity<String> handleUserInActiveRoomException(UserInActiveRoomException e){
        return ResponseEntity.status(400).body(e.getMessage());
    }
}
