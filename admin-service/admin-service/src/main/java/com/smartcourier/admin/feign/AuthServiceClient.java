package com.smartcourier.admin.feign;

import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Feign Client: Admin Service → Auth Service
 *
 * Uses Eureka service name "auth-service" for discovery.
 * No hardcoded URL — resolved dynamically via Eureka registry.
 */
@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @GetMapping("/api/auth/profile")
    UserProfile getUserProfile(@RequestHeader("X-User-Name") String username);

    @Data
    class UserProfile {
        private Long id;
        private String username;
        private String email;
        private String fullName;
        private String role;
        private Boolean isActive;
    }
}
