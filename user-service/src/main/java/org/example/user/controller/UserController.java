package org.example.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.user.model.User;
import org.example.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "User Service", description = "user business")
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Operation(summary = "get user list by id list", description = "input user id list, return user list")
    @GetMapping("/getUsersByIds")
    public List<User> getUsersByIds(@Parameter(description = "user id list, e.g. 1,2,3")
                                        @RequestParam List<Long> ids) {
        return userRepository.findByIdIn(ids);
    }
}
