package com.smartcourier.auth;

import com.smartcourier.auth.dto.AuthDTOs.*;
import com.smartcourier.auth.entity.Role;
import com.smartcourier.auth.entity.User;
import com.smartcourier.auth.repository.RoleRepository;
import com.smartcourier.auth.repository.UserRepository;
import com.smartcourier.auth.security.JwtUtils;
import com.smartcourier.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtils jwtUtils;

    @InjectMocks private AuthService authService;

    private User testUser;
    private Role userRole;

    @BeforeEach
    void setUp() {
        userRole = Role.builder().id(1L).name(Role.ERole.ROLE_USER).build();
        testUser = User.builder()
                .id(1L).username("john").email("john@test.com")
                .password("encoded").fullName("John Doe")
                .isActive(true).roles(Set.of(userRole))
                .build();
    }

    // ─── Signup Tests ─────────────────────────────────────────────────────────

    @Test
    void signup_Success() {
        SignupRequest req = new SignupRequest();
        req.setUsername("john"); req.setEmail("john@test.com");
        req.setPassword("pass123"); req.setFullName("John Doe");

        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@test.com")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("encoded");
        when(roleRepository.findByName(Role.ERole.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        MessageResponse response = authService.signup(req);

        assertThat(response.getMessage()).contains("successfully");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void signup_DuplicateUsername_ThrowsException() {
        SignupRequest req = new SignupRequest();
        req.setUsername("john"); req.setEmail("other@test.com"); req.setPassword("pass");

        when(userRepository.existsByUsername("john")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Username is already taken");
    }

    @Test
    void signup_DuplicateEmail_ThrowsException() {
        SignupRequest req = new SignupRequest();
        req.setUsername("newuser"); req.setEmail("john@test.com"); req.setPassword("pass");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("john@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email is already in use");
    }

    // ─── Login Tests ──────────────────────────────────────────────────────────

    @Test
    void login_Success_ReturnsJwtResponse() {
        LoginRequest req = new LoginRequest();
        req.setUsername("john"); req.setPassword("pass123");

        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(testUser));
        when(jwtUtils.generateToken("john", "ROLE_USER")).thenReturn("mock-jwt-token");

        JwtResponse response = authService.login(req);

        assertThat(response.getToken()).isEqualTo("mock-jwt-token");
        assertThat(response.getUsername()).isEqualTo("john");
        assertThat(response.getRole()).isEqualTo("ROLE_USER");
    }

    @Test
    void login_UserNotFound_ThrowsException() {
        LoginRequest req = new LoginRequest();
        req.setUsername("ghost"); req.setPassword("pass");

        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    // ─── GetProfile Tests ─────────────────────────────────────────────────────

    @Test
    void getProfile_Success() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(testUser));

        UserProfileResponse profile = authService.getProfile("john");

        assertThat(profile.getUsername()).isEqualTo("john");
        assertThat(profile.getEmail()).isEqualTo("john@test.com");
        assertThat(profile.getRole()).isEqualTo("ROLE_USER");
        assertThat(profile.getIsActive()).isTrue();
    }

    @Test
    void getProfile_NotFound_ThrowsException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getProfile("ghost"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    // ─── User Management Tests ────────────────────────────────────────────────

    @Test
    void toggleUserStatus_ActiveToInactive() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileResponse result = authService.toggleUserStatus(1L);

        assertThat(result.getIsActive()).isFalse();
    }

    @Test
    void toggleUserStatus_UserNotFound_ThrowsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.toggleUserStatus(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void getAllUsers_ReturnsAllUsers() {
        when(userRepository.findAll()).thenReturn(java.util.List.of(testUser));

        var users = authService.getAllUsers();

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getUsername()).isEqualTo("john");
    }
}
