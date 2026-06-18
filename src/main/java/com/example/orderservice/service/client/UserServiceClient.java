package com.example.orderservice.service.client;

import com.example.orderservice.dto.response.UserInfoResponse;
import com.example.orderservice.service.client.fallback.UserServiceFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "user-service",
        url = "${user.service.url:http://localhost:8081}",
        path = "/api/v1/users",
        fallback = UserServiceFallback.class
)
public interface UserServiceClient {

    @GetMapping("/{userId}")
    UserInfoResponse getUserById(@PathVariable("userId") UUID userId);
}
