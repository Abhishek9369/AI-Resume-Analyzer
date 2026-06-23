package io.subbu.ai.firedrill.services;

import io.subbu.ai.firedrill.config.JwtTokenProvider;
import io.subbu.ai.firedrill.entities.AuditLog;
import io.subbu.ai.firedrill.entities.User;
import io.subbu.ai.firedrill.models.UserRole;
import io.subbu.ai.firedrill.repositories.AuditLogRepository;
import io.subbu.ai.firedrill.repositories.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Authentication service for user login, registration, and token management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    /**
     * Authenticate user and generate tokens
     */
    @Transactional
    public AuthResponse login(String username, String password, HttpServletRequest request) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            // Load user
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Record login
            user.recordLogin();
            userRepository.save(user);

            // Generate tokens
            String accessToken = tokenProvider.generateAccessToken(user);
            String refreshToken = tokenProvider.generateRefreshToken(user);

            // Log successful login
            logAudit(user, "LOGIN", "User logged in successfully", request, true);

            log.info("User {} logged in successfully", username);

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .build();

        } catch (AuthenticationException e) {
            // Log failed login attempt
            logAudit(null, "LOGIN_FAILED", "Failed login attempt for username: " + username,
                    request, false);
            log.warn("Failed login attempt for username: {}", username);
            throw new RuntimeException("Invalid username or password");
        }
    }

    /**
     * Register new user (admin only)
     */
    @Transactional
    public User register(String username, String email, String password, String firstName,
                         String lastName, UserRole role, User createdBy, HttpServletRequest request) {
        // Check if username exists
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }

        // Check if email exists
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }

        // Create new user
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role);
        user.setActive(true);
        user.setCreatedBy(createdBy);

        User savedUser = userRepository.save(user);

        // Log user creation
        logAudit(createdBy, "USER_CREATED", "Created new user: " + username + " with role: " + role,
                request, true);

        log.info("New user created: {} with role: {}", username, role);

        return savedUser;
    }

    /**
     * Refresh access token using refresh token
     */
    public AuthResponse refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String tokenType = tokenProvider.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new RuntimeException("Not a refresh token");
        }

        String userId = tokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isActive()) {
            throw new RuntimeException("User account is inactive");
        }

        // Generate new access token
        String newAccessToken = tokenProvider.generateAccessToken(user);

        log.debug("Access token refreshed for user: {}", user.getUsername());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    /**
     * Validate token
     */
    public boolean validateToken(String token) {
        return tokenProvider.validateToken(token);
    }

    /**
     * Get user from token
     */
    public Optional<User> getUserFromToken(String token) {
        if (!tokenProvider.validateToken(token)) {
            return Optional.empty();
        }

        String userId = tokenProvider.getUserIdFromToken(token);
        return userRepository.findById(UUID.fromString(userId));
    }

    /**
     * Change password
     */
    @Transactional
    public void changePassword(User user, String oldPassword, String newPassword, HttpServletRequest request) {
        // Verify old password
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            logAudit(user, "PASSWORD_CHANGE_FAILED", "Incorrect old password", request, false);
            throw new RuntimeException("Incorrect old password");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Log password change
        logAudit(user, "PASSWORD_CHANGED", "Password changed successfully", request, true);

        log.info("Password changed for user: {}", user.getUsername());
    }

    /**
     * Reset password (admin only)
     */
    @Transactional
    public void resetPassword(User user, String newPassword, User admin, HttpServletRequest request) {
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Log password reset
        logAudit(admin, "PASSWORD_RESET", "Reset password for user: " + user.getUsername(),
                request, true);

        log.info("Password reset for user: {} by admin: {}", user.getUsername(), admin.getUsername());
    }

    /**
     * Deactivate user account
     */
    @Transactional
    public void deactivateUser(User user, User admin, HttpServletRequest request) {
        user.setActive(false);
        userRepository.save(user);

        // Log deactivation
        logAudit(admin, "USER_DEACTIVATED", "Deactivated user: " + user.getUsername(),
                request, true);

        log.info("User {} deactivated by admin: {}", user.getUsername(), admin.getUsername());
    }

    /**
     * Activate user account
     */
    @Transactional
    public void activateUser(User user, User admin, HttpServletRequest request) {
        user.setActive(true);
        userRepository.save(user);

        // Log activation
        logAudit(admin, "USER_ACTIVATED", "Activated user: " + user.getUsername(),
                request, true);

        log.info("User {} activated by admin: {}", user.getUsername(), admin.getUsername());
    }

    /**
     * Log audit entry
     */
    private void logAudit(User user, String action, String details, HttpServletRequest request, boolean success) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUser(user);
            auditLog.setUsername(user != null ? user.getUsername() : "anonymous");
            auditLog.setAction(action);
            auditLog.setDetails(details);
            auditLog.setSuccess(success);

            if (request != null) {
                auditLog.setIpAddress(getClientIpAddress(request));
                auditLog.setUserAgent(request.getHeader("User-Agent"));
            }

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to log audit entry", e);
        }
    }

    /**
     * Get client IP address from request
     */
    public String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Authentication response DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private UUID userId;
        private String username;
        private String email;
        private UserRole role;
    }
}
