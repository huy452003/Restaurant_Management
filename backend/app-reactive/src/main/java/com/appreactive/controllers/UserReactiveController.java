package com.appreactive.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appreactive.services.UserReactiveService;
import com.common.models.user.UserModel;

import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/reactive/users")
public class UserReactiveController {
    @Autowired
    private UserReactiveService userReactiveService;

    @GetMapping("/flux-test")
    public Flux<UserModel> getFluxTest() {
        return userReactiveService.getFluxTest();
    }
}
