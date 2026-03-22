package com.smartcourier.admin.feign;

import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Feign Client: Admin Service → Delivery Service
 * Uses Eureka service name "delivery-service" for discovery.
 */
@FeignClient(name = "delivery-service")
public interface DeliveryServiceClient {

    @GetMapping("/api/deliveries/admin/{id}")
    DeliveryResponse getDeliveryById(
            @PathVariable Long id,
            @RequestHeader("X-User-Name") String username);

    @GetMapping("/api/deliveries/admin/all")
    List<DeliveryResponse> getAllDeliveries(
            @RequestParam(required = false) String status,
            @RequestHeader("X-User-Name") String username);

    @PutMapping("/api/deliveries/{id}/status")
    DeliveryResponse updateDeliveryStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String notes,
            @RequestHeader("X-User-Name") String username,
            @RequestHeader("X-User-Role") String role);

    @Data
    class DeliveryResponse {
        private Long id;
        private String trackingNumber;
        private Long userId;
        private String serviceType;
        private String status;
        private BigDecimal totalAmount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
