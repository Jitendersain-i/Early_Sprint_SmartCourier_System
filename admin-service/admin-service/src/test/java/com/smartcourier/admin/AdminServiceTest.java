package com.smartcourier.admin;

import com.smartcourier.admin.dto.AdminDTOs.*;
import com.smartcourier.admin.entity.Hub;
import com.smartcourier.admin.feign.*;
import com.smartcourier.admin.repository.HubRepository;
import com.smartcourier.admin.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private HubRepository hubRepository;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private DeliveryServiceClient deliveryServiceClient;
    @Mock private TrackingServiceClient trackingServiceClient;
    @Mock private UserServiceClient userServiceClient;

    @InjectMocks private AdminService adminService;

    private AuthServiceClient.UserProfile adminProfile;
    private AuthServiceClient.UserProfile nonAdminProfile;
    private DeliveryServiceClient.DeliveryResponse sampleDelivery;

    @BeforeEach
    void setUp() {
        adminProfile = new AuthServiceClient.UserProfile();
        adminProfile.setId(1L); adminProfile.setUsername("admin");
        adminProfile.setRole("ROLE_ADMIN");

        nonAdminProfile = new AuthServiceClient.UserProfile();
        nonAdminProfile.setId(2L); nonAdminProfile.setUsername("user");
        nonAdminProfile.setRole("ROLE_USER");

        sampleDelivery = new DeliveryServiceClient.DeliveryResponse();
        sampleDelivery.setId(10L); sampleDelivery.setTrackingNumber("SC123");
        sampleDelivery.setStatus("BOOKED"); sampleDelivery.setTotalAmount(BigDecimal.valueOf(15.00));
    }

    // ─── Role Guard ───────────────────────────────────────────────────────────

    @Test
    void getAllDeliveries_NonAdmin_ThrowsAccessDenied() {
        when(authServiceClient.getUserProfile("user")).thenReturn(nonAdminProfile);

        assertThatThrownBy(() -> adminService.getAllDeliveries(null, "user"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Access denied");
    }

    // ─── Dashboard ────────────────────────────────────────────────────────────

    @Test
    void getDashboard_ReturnsLiveStats() {
        when(authServiceClient.getUserProfile("admin")).thenReturn(adminProfile);
        when(hubRepository.count()).thenReturn(3L);

        DeliveryServiceClient.DeliveryResponse delivered = new DeliveryServiceClient.DeliveryResponse();
        delivered.setStatus("DELIVERED"); delivered.setTotalAmount(BigDecimal.valueOf(20.00));
        when(deliveryServiceClient.getAllDeliveries(null, "admin"))
                .thenReturn(List.of(sampleDelivery, delivered));

        DashboardResponse dash = adminService.getDashboard("admin");

        assertThat(dash.getTotalHubs()).isEqualTo(3L);
        assertThat(dash.getTotalDeliveries()).isEqualTo(2L);
        assertThat(dash.getTotalRevenue()).isEqualByComparingTo(BigDecimal.valueOf(35.00));
    }

    // ─── Deliveries ───────────────────────────────────────────────────────────

    @Test
    void getAllDeliveries_Admin_ReturnsList() {
        when(authServiceClient.getUserProfile("admin")).thenReturn(adminProfile);
        when(deliveryServiceClient.getAllDeliveries(null, "admin"))
                .thenReturn(List.of(sampleDelivery));

        List<DeliveryServiceClient.DeliveryResponse> result =
                adminService.getAllDeliveries(null, "admin");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTrackingNumber()).isEqualTo("SC123");
    }

    @Test
    void getDelivery_Admin_ReturnsDelivery() {
        when(authServiceClient.getUserProfile("admin")).thenReturn(adminProfile);
        when(deliveryServiceClient.getDeliveryById(10L, "admin")).thenReturn(sampleDelivery);

        DeliveryServiceClient.DeliveryResponse result = adminService.getDelivery(10L, "admin");

        assertThat(result.getId()).isEqualTo(10L);
    }

    // ─── Exception Resolution ─────────────────────────────────────────────────

    @Test
    void resolveDeliveryException_Admin_CallsDeliveryFeign() {
        ResolveExceptionRequest req = new ResolveExceptionRequest();
        req.setResolutionStatus("IN_TRANSIT");
        req.setLocation("Hub B"); req.setNotes("Package found after delay");

        DeliveryServiceClient.DeliveryResponse resolved = new DeliveryServiceClient.DeliveryResponse();
        resolved.setId(10L); resolved.setStatus("IN_TRANSIT");

        when(authServiceClient.getUserProfile("admin")).thenReturn(adminProfile);
        when(deliveryServiceClient.updateDeliveryStatus(
                eq(10L), eq("IN_TRANSIT"), eq("Hub B"), eq("Package found after delay"),
                eq("admin"), eq("ROLE_ADMIN"))).thenReturn(resolved);

        DeliveryServiceClient.DeliveryResponse result =
                adminService.resolveDeliveryException(10L, req, "admin");

        assertThat(result.getStatus()).isEqualTo("IN_TRANSIT");
        verify(deliveryServiceClient).updateDeliveryStatus(
                10L, "IN_TRANSIT", "Hub B", "Package found after delay", "admin", "ROLE_ADMIN");
    }

    // ─── Hub Management ───────────────────────────────────────────────────────

    @Test
    void createHub_Success() {
        CreateHubRequest req = new CreateHubRequest();
        req.setName("Mumbai Hub"); req.setAddress("Andheri East");
        req.setCity("Mumbai"); req.setCountry("India");

        Hub savedHub = Hub.builder().id(1L).name("Mumbai Hub")
                .address("Andheri East").city("Mumbai").country("India").isActive(true).build();

        when(authServiceClient.getUserProfile("admin")).thenReturn(adminProfile);
        when(hubRepository.existsByName("Mumbai Hub")).thenReturn(false);
        when(hubRepository.save(any(Hub.class))).thenReturn(savedHub);

        HubResponse response = adminService.createHub(req, "admin");

        assertThat(response.getName()).isEqualTo("Mumbai Hub");
        assertThat(response.getIsActive()).isTrue();
    }

    @Test
    void createHub_DuplicateName_ThrowsException() {
        CreateHubRequest req = new CreateHubRequest();
        req.setName("Mumbai Hub"); req.setAddress("X"); req.setCity("Y"); req.setCountry("Z");

        when(authServiceClient.getUserProfile("admin")).thenReturn(adminProfile);
        when(hubRepository.existsByName("Mumbai Hub")).thenReturn(true);

        assertThatThrownBy(() -> adminService.createHub(req, "admin"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Hub with name already exists");
    }

    // ─── User Management ──────────────────────────────────────────────────────

    @Test
    void getAllUsers_Admin_ReturnsList() {
        UserServiceClient.UserResponse user = new UserServiceClient.UserResponse();
        user.setId(5L); user.setUsername("testuser"); user.setRole("ROLE_USER");

        when(authServiceClient.getUserProfile("admin")).thenReturn(adminProfile);
        when(userServiceClient.getAllUsers("admin")).thenReturn(List.of(user));

        List<UserServiceClient.UserResponse> result = adminService.getAllUsers("admin");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsername()).isEqualTo("testuser");
    }

    // ─── Reports ─────────────────────────────────────────────────────────────

    @Test
    void getReports_ReturnsComputedStats() {
        DeliveryServiceClient.DeliveryResponse d1 = new DeliveryServiceClient.DeliveryResponse();
        d1.setStatus("DELIVERED"); d1.setTotalAmount(BigDecimal.valueOf(10.00));
        DeliveryServiceClient.DeliveryResponse d2 = new DeliveryServiceClient.DeliveryResponse();
        d2.setStatus("FAILED"); d2.setTotalAmount(BigDecimal.valueOf(20.00));

        when(authServiceClient.getUserProfile("admin")).thenReturn(adminProfile);
        when(deliveryServiceClient.getAllDeliveries(null, "admin")).thenReturn(List.of(d1, d2));

        ReportsResponse report = adminService.getReports("monthly", "admin");

        assertThat(report.getTotalDeliveries()).isEqualTo(2L);
        assertThat(report.getDeliveredCount()).isEqualTo(1L);
        assertThat(report.getFailedCount()).isEqualTo(1L);
        assertThat(report.getTotalRevenue()).isEqualByComparingTo(BigDecimal.valueOf(30.00));
    }
}
