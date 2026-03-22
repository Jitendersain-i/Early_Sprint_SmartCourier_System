package com.smartcourier.auth.service;

import com.smartcourier.auth.dto.AuthDTOs.*;
import com.smartcourier.auth.entity.Role;
import com.smartcourier.auth.entity.User;
import com.smartcourier.auth.repository.RoleRepository;
import com.smartcourier.auth.repository.UserRepository;
import com.smartcourier.auth.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    // ─── Signup ───────────────────────────────────────────────────────────────

    @Transactional
    public MessageResponse signup(SignupRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username is already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already in use: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .isActive(true)
                .build();

        Set<Role> roles = new HashSet<>();
        Role.ERole eRole = resolveRole(request.getRole());
        Role role = roleRepository.findByName(eRole)
                .orElseGet(() -> roleRepository.save(Role.builder().name(eRole).build()));
        roles.add(role);
        user.setRoles(roles);

        userRepository.save(user);
        log.info("New user registered: {}", user.getUsername());
        return new MessageResponse("User registered successfully!");
    }

    // ─── Login ────────────────────────────────────────────────────────────────

    public JwtResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String roleName = user.getRoles().stream().findFirst()
                .map(r -> r.getName().name()).orElse("ROLE_USER");

        String token = jwtUtils.generateToken(user.getUsername(), roleName);
        log.info("User logged in: {}", user.getUsername());
        return new JwtResponse(token, user.getId(), user.getUsername(), user.getEmail(), roleName);
    }

    // ─── Profile ──────────────────────────────────────────────────────────────

    public UserProfileResponse getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return toProfileResponse(user);
    }

    // ─── Admin: User Management ───────────────────────────────────────────────

    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toProfileResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserProfileResponse toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        user.setIsActive(!Boolean.TRUE.equals(user.getIsActive()));
        User saved = userRepository.save(user);
        log.info("User {} active status toggled to {}", saved.getUsername(), saved.getIsActive());
        return toProfileResponse(saved);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private UserProfileResponse toProfileResponse(User user) {
        UserProfileResponse p = new UserProfileResponse();
        p.setId(user.getId());
        p.setUsername(user.getUsername());
        p.setEmail(user.getEmail());
        p.setFullName(user.getFullName());
        p.setPhoneNumber(user.getPhoneNumber());
        p.setIsActive(user.getIsActive());
        p.setRole(user.getRoles().stream().findFirst()
                .map(r -> r.getName().name()).orElse("ROLE_USER"));
        return p;
    }

    private Role.ERole resolveRole(String roleStr) {
        if (roleStr == null) return Role.ERole.ROLE_USER;
        return switch (roleStr.toUpperCase()) {
            case "ROLE_ADMIN", "ADMIN"   -> Role.ERole.ROLE_ADMIN;
            case "ROLE_DRIVER", "DRIVER" -> Role.ERole.ROLE_DRIVER;
            default                      -> Role.ERole.ROLE_USER;
        };
    }
}
