package com.smartcourier.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class AdminDTOs {

    // ─── Dashboard ────────────────────────────────────────────────────────────
    @Data
    public static class DashboardResponse {
        private long totalDeliveries;
        private long activeDeliveries;
        private long totalHubs;
        private BigDecimal totalRevenue;
        private List<StatusCount> deliveriesByStatus;
    }

    @Data
    public static class StatusCount {
        private String status;
        private long count;
        public StatusCount(String status, long count) {
            this.status = status; this.count = count;
        }
    }

    // ─── Hub ─────────────────────────────────────────────────────────────────
    @Data
    public static class CreateHubRequest {
        @NotBlank private String name;
        @NotBlank private String address;
        @NotBlank private String city;
        private String state;
        @NotBlank private String country;
    }

    @Data
    public static class HubResponse {
        private Long id;
        private String name;
        private String address;
        private String city;
        private String state;
        private String country;
        private Boolean isActive;
        private LocalDateTime createdAt;
    }

    // ─── Update Delivery Status ───────────────────────────────────────────────
    @Data
    public static class UpdateDeliveryStatusRequest {
        @NotNull  private Long deliveryId;
        @NotBlank private String status;
        private String location;
        private String notes;
    }

    // ─── Exception Resolution ─────────────────────────────────────────────────
    @Data
    public static class ResolveExceptionRequest {
        @NotBlank private String resolutionStatus;  // e.g. IN_TRANSIT, RETURNED, FAILED
        private String location;
        @NotBlank private String notes;             // mandatory: reason for resolution
    }

    // ─── Reports ─────────────────────────────────────────────────────────────
    @Data
    public static class ReportsResponse {
        private String period;
        private BigDecimal totalRevenue;
        private long totalDeliveries;
        private long deliveredCount;
        private long failedCount;
        private long delayedCount;
        private BigDecimal averageOrderValue;
    }

    // ─── (kept for backward compat) ───────────────────────────────────────────
    @Data
    public static class RevenueReportResponse {
        private String period;
        private BigDecimal totalRevenue;
        private long totalDeliveries;
        private BigDecimal averageOrderValue;
    }

    @Data
    public static class MessageResponse {
        private String message;
        public MessageResponse(String message) { this.message = message; }
    }
}
