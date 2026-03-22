package com.smartcourier.admin.feign;

import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Feign Client: Admin Service → Tracking Service
 *
 * Uses Eureka service name "tracking-service" for discovery.
 * No hardcoded URL — resolved dynamically via Eureka registry.
 */
@FeignClient(name = "tracking-service")
public interface TrackingServiceClient {

    @GetMapping("/api/tracking/{deliveryId}")
    TrackingHistoryResponse getTracking(@PathVariable Long deliveryId);

    @PostMapping("/api/tracking/events")
    TrackingEventResponse createEvent(
            @RequestHeader("X-User-Name") String username,
            @RequestHeader("X-User-Role") String role,
            @RequestBody CreateEventRequest request);

    @Data
    class TrackingHistoryResponse {
        private Long deliveryId;
        private String currentStatus;
        private List<TrackingEventResponse> events;
    }

    @Data
    class TrackingEventResponse {
        private Long id;
        private Long deliveryId;
        private String status;
        private String location;
        private String notes;
        private LocalDateTime createdAt;
    }

    @Data
    class CreateEventRequest {
        private Long deliveryId;
        private String status;
        private String location;
        private String notes;
    }
}
