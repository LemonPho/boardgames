package com.motomutterers.boardgames.user.controllers;

import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.motomutterers.boardgames.user.model.User;
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

    /*
    @GetMapping("/{username}")
    public ResponseEntity<User> getUserByUsername(
        @PathVariable String username,
        Authentication authentication
    ){

    } */
}
