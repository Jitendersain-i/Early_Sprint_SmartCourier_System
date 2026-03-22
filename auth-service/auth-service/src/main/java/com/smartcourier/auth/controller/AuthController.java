package com.smartcourier.auth.controller;

import com.smartcourier.auth.dto.AuthDTOs.*;
import com.smartcourier.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication, user profile, and admin user management")
public class AuthController {

    private final AuthService authService;

    /** GET /api/auth/services  [PUBLIC] — available courier services for landing page */
    @GetMapping("/services")
    @Operation(summary = "Get available courier service types (public — landing page)")
    public ResponseEntity<List<Map<String, Object>>> getServices() {
        List<Map<String, Object>> services = List.of(
            Map.of("type", "STANDARD",  "label", "Standard Delivery",  "description", "3-5 business days",  "basePrice", 5.00),
            Map.of("type", "EXPRESS",   "label", "Express Delivery",   "description", "1-2 business days",  "basePrice", 12.00),
            Map.of("type", "OVERNIGHT", "label", "Overnight Delivery", "description", "Next business day",  "basePrice", 20.00),
            Map.of("type", "SAME_DAY",  "label", "Same Day Delivery",  "description", "Delivered today",    "basePrice", 30.00)
        );
        return ResponseEntity.ok(services);
    }

    /** POST /api/auth/signup */
    @PostMapping("/signup")
    @Operation(summary = "Register a new user")
    public ResponseEntity<MessageResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(authService.signup(request));
    }

    /** POST /api/auth/login */
    @PostMapping("/login")
    @Operation(summary = "Login and receive JWT token")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /** GET /api/auth/profile */
    @GetMapping("/profile")
    @Operation(summary = "Get authenticated user profile")
    public ResponseEntity<UserProfileResponse> getProfile(
            @RequestHeader("X-User-Name") String username) {
        return ResponseEntity.ok(authService.getProfile(username));
    }

    /** GET /api/auth/admin/users  — called by Admin Service via Feign */
    @GetMapping("/admin/users")
    @Operation(summary = "Admin: list all users (called via Feign from Admin Service)")
    public ResponseEntity<List<UserProfileResponse>> getAllUsers(
            @RequestHeader("X-User-Name") String username) {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    /** PUT /api/auth/admin/users/{id}/toggle  — called by Admin Service via Feign */
    @PutMapping("/admin/users/{id}/toggle")
    @Operation(summary = "Admin: toggle user active/inactive status")
    public ResponseEntity<UserProfileResponse> toggleUserStatus(
            @PathVariable Long id,
            @RequestHeader("X-User-Name") String username) {
        return ResponseEntity.ok(authService.toggleUserStatus(id));
    }
}
