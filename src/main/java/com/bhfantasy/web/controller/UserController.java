package com.bhfantasy.web.controller;

import com.bhfantasy.web.model.User;
import com.bhfantasy.web.repository.UserRepository;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    @Secured("ROLE_USER")
    public User user(@AuthenticationPrincipal User user) {
        return userRepository.findByPrincipalId(user.getPrincipalId());
    }
}
