package org.example.transfer.client;

import org.example.transfer.model.dto.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/api/user/getUsersByIds")
    List<User> getUsers(@RequestParam("ids") List<Long> ids);
}