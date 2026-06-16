package com.example.orderservice.wiremock;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

public class UserServiceStubs {

    public static void stubGetUserByIdSuccess(WireMockRuntimeInfo wmRuntimeInfo, UUID userId) {
        String response = String.format("""
                {
                    "id": "%s",
                    "email": "john.doe@example.com",
                    "name": "John",
                    "surname": "Doe",
                    "active": true,
                    "createdAt": "2024-01-15T10:00:00",
                    "updatedAt": "2024-01-15T10:00:00"
                }
                """, userId.toString());

        stubFor(get(urlPathEqualTo("/api/v1/users/" + userId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(response)));
    }

    public static void stubGetUserByIdNotFound(UUID userId) {
        stubFor(get(urlPathEqualTo("/api/v1/users/" + userId))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody("User not found")));
    }

    public static void stubGetUserByIdTimeout(UUID userId) {
        stubFor(get(urlPathEqualTo("/api/v1/users/" + userId))
                .willReturn(aResponse()
                        .withStatus(504)
                        .withFixedDelay(10000)
                        .withBody("Gateway Timeout")));
    }

    public static void stubGetUserByIdInternalError(UUID userId) {
        stubFor(get(urlPathEqualTo("/api/v1/users/" + userId))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));
    }

    public static void resetStubs() {
        WireMock.reset();
    }
}
