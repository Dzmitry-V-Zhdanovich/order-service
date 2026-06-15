package com.example.orderservice.service.client.fallback;

import com.example.orderservice.dto.response.UserInfoResponse;
import com.example.orderservice.service.client.UserServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class UserServiceFallback implements UserServiceClient {

    @Override
    public UserInfoResponse getUserById(UUID userId) {
        log.warn("Circuit breaker is OPEN. Returning fallback response for user id: {}", userId);

        return UserInfoResponse.builder()
                .id(userId)
                .name("User")
                .surname("Information Unavailable")
                .email("unavailable@user.service")
                .active(false)
                .build();
    }
}
