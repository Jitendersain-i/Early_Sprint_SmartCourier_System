package com.smartcourier.admin.feign;

import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Feign Client: Admin Service → Auth Service (user management)
 * Uses Eureka service name "auth-service" for discovery.
 */
@FeignClient(name = "auth-service", contextId = "userServiceClient")
public interface UserServiceClient {

    @GetMapping("/api/auth/admin/users")
    List<UserResponse> getAllUsers(@RequestHeader("X-User-Name") String username);

    @PutMapping("/api/auth/admin/users/{id}/toggle")
    UserResponse toggleUserStatus(
            @PathVariable Long id,
            @RequestHeader("X-User-Name") String username);

    @Data
    class UserResponse {
        private Long id;
        private String username;
        private String email;
        private String fullName;
        private String role;
        private Boolean isActive;
        private LocalDateTime createdAt;
    }
}
