package com.smartcourier.admin.controller;

import com.smartcourier.admin.dto.AdminDTOs.*;
import com.smartcourier.admin.feign.DeliveryServiceClient;
import com.smartcourier.admin.feign.TrackingServiceClient;
import com.smartcourier.admin.feign.UserServiceClient;
import com.smartcourier.admin.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin monitoring, reports, user/hub management")
public class AdminController {

    private final AdminService adminService;

    // ─── Dashboard ────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    @Operation(summary = "Admin dashboard: live stats — deliveries, revenue, hubs")
    public ResponseEntity<DashboardResponse> getDashboard(
            @RequestHeader("X-User-Name") String username) {
        return ResponseEntity.ok(adminService.getDashboard(username));
    }

    // ─── Delivery Monitoring ──────────────────────────────────────────────────

    /** GET /api/admin/deliveries?status=  — list all deliveries (optional filter) */
    @GetMapping("/deliveries")
    @Operation(summary = "List all deliveries across the platform (optional status filter)")
    public ResponseEntity<List<DeliveryServiceClient.DeliveryResponse>> getAllDeliveries(
            @RequestParam(required = false) String status,
            @RequestHeader("X-User-Name") String username) {
        return ResponseEntity.ok(adminService.getAllDeliveries(status, username));
    }

    /** GET /api/admin/deliveries/{id} — get single delivery */
    @GetMapping("/deliveries/{id}")
    @Operation(summary = "Get any delivery by ID")
    public ResponseEntity<DeliveryServiceClient.DeliveryResponse> getDelivery(
            @PathVariable Long id,
            @RequestHeader("X-User-Name") String username) {
        return ResponseEntity.ok(adminService.getDelivery(id, username));
    }

    /** GET /api/admin/tracking/{deliveryId} — tracking history */
    @GetMapping("/tracking/{deliveryId}")
    @Operation(summary = "Get tracking history for any delivery")
    public ResponseEntity<TrackingServiceClient.TrackingHistoryResponse> getTracking(
            @PathVariable Long deliveryId,
            @RequestHeader("X-User-Name") String username) {
        return ResponseEntity.ok(adminService.getTracking(deliveryId, username));
    }

    /** POST /api/admin/deliveries/status — push status update */
    @PostMapping("/deliveries/status")
    @Operation(summary = "Push a status update event for a delivery")
    public ResponseEntity<TrackingServiceClient.TrackingEventResponse> updateStatus(
            @Valid @RequestBody UpdateDeliveryStatusRequest request,
            @RequestHeader("X-User-Name") String username) {
        return ResponseEntity.ok(adminService.updateDeliveryStatus(request, username));
    }

    // ─── Exception Handling ───────────────────────────────────────────────────

    /** PUT /api/admin/deliveries/{id}/resolve — resolve a delayed/failed delivery */
    @PutMapping("/deliveries/{id}/resolve")
    @Operation(summary = "Resolve a delivery exception (DELAYED/FAILED → new status)")
    public ResponseEntity<DeliveryServiceClient.DeliveryResponse> resolveException(
            @PathVariable Long id,
            @Valid @RequestBody ResolveExceptionRequest request,
            @RequestHeader("X-User-Name") String username) {
        return ResponseEntity.ok(adminService.resolveDeliveryException(id, request, username));
    }

    // ─── User Management ──────────────────────────────────────────────────────

    /** GET /api/admin/users — list all registered users */
    @GetMapping("/users")
    @Operation(summary = "List all users on the platform")
    public ResponseEntity<List<UserServiceClient.UserResponse>> getAllUsers(
            @RequestHeader("X-User-Name") String username) {
        return ResponseEntity.ok(adminService.getAllUsers(username));
    }

    /** PUT /api/admin/users/{id}/toggle — activate / deactivate user */
    @PutMapping("/users/{id}/toggle")
    @Operation(summary = "Toggle a user's active/inactive status")
    public ResponseEntity<UserServiceClient.UserResponse> toggleUser(
            @PathVariable Long id,
            @RequestHeader("X-User-Name") String username) {
        return ResponseEntity.ok(adminService.toggleUserStatus(id, username));
    }

    // ─── Hub Management ───────────────────────────────────────────────────────

    @PostMapping("/hubs")
    @Operation(summary = "Create a new dispatch hub")
    public ResponseEntity<HubResponse> createHub(
            @Valid @RequestBody CreateHubRequest request,
            @RequestHeader("X-User-Name") String username) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.createHub(request, username));
    }

    @GetMapping("/hubs")
    @Operation(summary = "List all active hubs")
    public ResponseEntity<List<HubResponse>> getAllHubs(
            @RequestHeader("X-User-Name") String username) {
        return ResponseEntity.ok(adminService.getAllHubs(username));
    }

    // ─── Reports ─────────────────────────────────────────────────────────────

    /** GET /api/admin/reports — unified operational report */
    @GetMapping("/reports")
    @Operation(summary = "Get unified operational report (revenue + delivery stats)")
    public ResponseEntity<ReportsResponse> getReports(
            @RequestParam(defaultValue = "monthly") String period,
            @RequestHeader("X-User-Name") String username) {
        return ResponseEntity.ok(adminService.getReports(period, username));
    }
}
