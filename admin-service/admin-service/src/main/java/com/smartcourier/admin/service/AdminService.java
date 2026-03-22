package com.smartcourier.admin.service;

import com.smartcourier.admin.dto.AdminDTOs.*;
import com.smartcourier.admin.entity.Hub;
import com.smartcourier.admin.feign.*;
import com.smartcourier.admin.repository.HubRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final HubRepository hubRepository;
    private final AuthServiceClient authServiceClient;
    private final DeliveryServiceClient deliveryServiceClient;
    private final TrackingServiceClient trackingServiceClient;
    private final UserServiceClient userServiceClient;

    // ─── Role Guard ───────────────────────────────────────────────────────────

    private void requireAdmin(String username) {
        AuthServiceClient.UserProfile profile = authServiceClient.getUserProfile(username);
        if (!"ROLE_ADMIN".equals(profile.getRole())) {
            throw new RuntimeException("Access denied: ROLE_ADMIN required");
        }
    }

    // ─── Dashboard ────────────────────────────────────────────────────────────

    public DashboardResponse getDashboard(String username) {
        requireAdmin(username);
        long totalHubs = hubRepository.count();

        // Fetch all deliveries via Feign and compute live stats
        List<DeliveryServiceClient.DeliveryResponse> all =
                deliveryServiceClient.getAllDeliveries(null, username);

        long totalDeliveries = all.size();
        long activeDeliveries = all.stream()
                .filter(d -> d.getStatus() != null &&
                        !d.getStatus().equals("DELIVERED") &&
                        !d.getStatus().equals("RETURNED") &&
                        !d.getStatus().equals("FAILED"))
                .count();
        BigDecimal totalRevenue = all.stream()
                .filter(d -> d.getTotalAmount() != null)
                .map(DeliveryServiceClient.DeliveryResponse::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Count by status
        java.util.Map<String, Long> byStatus = all.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        d -> d.getStatus() != null ? d.getStatus() : "UNKNOWN",
                        java.util.stream.Collectors.counting()));

        List<StatusCount> statusCounts = byStatus.entrySet().stream()
                .map(e -> new StatusCount(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        DashboardResponse res = new DashboardResponse();
        res.setTotalHubs(totalHubs);
        res.setTotalDeliveries(totalDeliveries);
        res.setActiveDeliveries(activeDeliveries);
        res.setTotalRevenue(totalRevenue);
        res.setDeliveriesByStatus(statusCounts);
        log.info("Dashboard fetched by admin: {}", username);
        return res;
    }

    // ─── Delivery Monitoring ──────────────────────────────────────────────────

    public List<DeliveryServiceClient.DeliveryResponse> getAllDeliveries(String status, String username) {
        requireAdmin(username);
        return deliveryServiceClient.getAllDeliveries(status, username);
    }

    public DeliveryServiceClient.DeliveryResponse getDelivery(Long id, String username) {
        requireAdmin(username);
        return deliveryServiceClient.getDeliveryById(id, username);
    }

    // ─── Exception Handling / Resolve ─────────────────────────────────────────

    @Transactional
    public DeliveryServiceClient.DeliveryResponse resolveDeliveryException(
            Long deliveryId, ResolveExceptionRequest req, String username) {
        requireAdmin(username);

        // Update the delivery status to the resolved state via Feign → Delivery Service
        DeliveryServiceClient.DeliveryResponse updated = deliveryServiceClient.updateDeliveryStatus(
                deliveryId,
                req.getResolutionStatus(),
                req.getLocation(),
                req.getNotes(),
                username,
                "ROLE_ADMIN"
        );
        log.info("Admin {} resolved exception for delivery {} → {}", username, deliveryId, req.getResolutionStatus());
        return updated;
    }

    // ─── Tracking ─────────────────────────────────────────────────────────────

    public TrackingServiceClient.TrackingHistoryResponse getTracking(Long deliveryId, String username) {
        requireAdmin(username);
        return trackingServiceClient.getTracking(deliveryId);
    }

    @Transactional
    public TrackingServiceClient.TrackingEventResponse updateDeliveryStatus(
            UpdateDeliveryStatusRequest req, String username) {
        requireAdmin(username);

        TrackingServiceClient.CreateEventRequest eventReq = new TrackingServiceClient.CreateEventRequest();
        eventReq.setDeliveryId(req.getDeliveryId());
        eventReq.setStatus(req.getStatus());
        eventReq.setLocation(req.getLocation());
        eventReq.setNotes(req.getNotes());

        TrackingServiceClient.TrackingEventResponse result =
                trackingServiceClient.createEvent(username, "ROLE_ADMIN", eventReq);

        log.info("Admin {} updated delivery {} to status {}", username, req.getDeliveryId(), req.getStatus());
        return result;
    }

    // ─── User Management ──────────────────────────────────────────────────────

    public List<UserServiceClient.UserResponse> getAllUsers(String username) {
        requireAdmin(username);
        return userServiceClient.getAllUsers(username);
    }

    public UserServiceClient.UserResponse toggleUserStatus(Long userId, String username) {
        requireAdmin(username);
        return userServiceClient.toggleUserStatus(userId, username);
    }

    // ─── Hubs ─────────────────────────────────────────────────────────────────

    @Transactional
    public HubResponse createHub(CreateHubRequest req, String username) {
        requireAdmin(username);
        if (hubRepository.existsByName(req.getName())) {
            throw new RuntimeException("Hub with name already exists: " + req.getName());
        }
        Hub hub = Hub.builder()
                .name(req.getName()).address(req.getAddress())
                .city(req.getCity()).state(req.getState())
                .country(req.getCountry()).isActive(true)
                .build();
        Hub saved = hubRepository.save(hub);
        log.info("Hub created: {} by admin: {}", saved.getName(), username);
        return toHubResponse(saved);
    }

    public List<HubResponse> getAllHubs(String username) {
        requireAdmin(username);
        return hubRepository.findByIsActiveTrue()
                .stream().map(this::toHubResponse).collect(Collectors.toList());
    }

    // ─── Reports ─────────────────────────────────────────────────────────────

    public ReportsResponse getReports(String period, String username) {
        requireAdmin(username);

        List<DeliveryServiceClient.DeliveryResponse> all =
                deliveryServiceClient.getAllDeliveries(null, username);

        BigDecimal totalRevenue = all.stream()
                .filter(d -> d.getTotalAmount() != null)
                .map(DeliveryServiceClient.DeliveryResponse::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long delivered = all.stream()
                .filter(d -> "DELIVERED".equals(d.getStatus())).count();
        long failed = all.stream()
                .filter(d -> "FAILED".equals(d.getStatus()) || "RETURNED".equals(d.getStatus())).count();
        long delayed = all.stream()
                .filter(d -> "DELAYED".equals(d.getStatus())).count();

        BigDecimal avg = all.isEmpty() ? BigDecimal.ZERO :
                totalRevenue.divide(BigDecimal.valueOf(all.size()), 2, java.math.RoundingMode.HALF_UP);

        ReportsResponse report = new ReportsResponse();
        report.setPeriod(period);
        report.setTotalRevenue(totalRevenue);
        report.setTotalDeliveries((long) all.size());
        report.setDeliveredCount(delivered);
        report.setFailedCount(failed);
        report.setDelayedCount(delayed);
        report.setAverageOrderValue(avg);
        return report;
    }

    // ─── Mapper ───────────────────────────────────────────────────────────────

    private HubResponse toHubResponse(Hub h) {
        HubResponse r = new HubResponse();
        r.setId(h.getId()); r.setName(h.getName());
        r.setAddress(h.getAddress()); r.setCity(h.getCity());
        r.setState(h.getState()); r.setCountry(h.getCountry());
        r.setIsActive(h.getIsActive()); r.setCreatedAt(h.getCreatedAt());
        return r;
    }
}
