package io.subbu.ai.firedrill.services;

import io.subbu.ai.firedrill.config.SecurityUtils;
import io.subbu.ai.firedrill.entities.AuditLog;
import io.subbu.ai.firedrill.entities.User;
import io.subbu.ai.firedrill.models.UserRole;
import io.subbu.ai.firedrill.repositories.AuditLogRepository;
import io.subbu.ai.firedrill.repositories.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for user management operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuthenticationService authenticationService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Get all users (admin only)
     */
    @PreAuthorize("hasRole('ADMIN')")
    public Page<User> getAllUsers(Pageable pageable) {
        log.debug("Getting all users with pagination: {}", pageable);
        return userRepository.findAll(pageable);
    }

    /**
     * Get user by ID
     */
    public Optional<User> getUserById(UUID id) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));
        
        // Users can view their own profile, admins can view any user
        if (!currentUser.isAdmin() && !currentUser.getId().equals(id)) {
            throw new RuntimeException("Access denied: Cannot view other user profiles");
        }
        
        return userRepository.findById(id);
    }

    /**
     * Get users by role (admin only)
     */
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> getUsersByRole(UserRole role) {
        log.debug("Getting users by role: {}", role);
        return userRepository.findByRole(role);
    }

    /**
     * Get user by username
     */
    public Optional<User> getUserByUsername(String username) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));
        
        // Users can search for their own username, admins can search any username
        if (!currentUser.isAdmin() && !currentUser.getUsername().equals(username)) {
            throw new RuntimeException("Access denied: Cannot search other users");
        }
        
        return userRepository.findByUsername(username);
    }

    /**
     * Get user by email
     */
    public Optional<User> getUserByEmail(String email) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));
        
        // Users can search for their own email, admins can search any email
        if (!currentUser.isAdmin() && !currentUser.getEmail().equals(email)) {
            throw new RuntimeException("Access denied: Cannot search other users");
        }
        
        return userRepository.findByEmail(email);
    }

    /**
     * Create new user (admin only) - delegates to AuthenticationService
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public User createUser(String username, String email, String password, 
                          String firstName, String lastName, UserRole role,
                          HttpServletRequest request) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));
        
        return authenticationService.register(username, email, password, 
                firstName, lastName, role, currentUser, request);
    }

    /**
     * Update user details
     */
    @Transactional
    public User updateUser(UUID userId, String email, String firstName, String lastName,
                          UserRole role, HttpServletRequest request) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Users can update their own profile (except role), admins can update any user
        if (!currentUser.isAdmin() && !currentUser.getId().equals(userId)) {
            throw new RuntimeException("Access denied: Cannot update other user profiles");
        }
        
        // Only admins can change roles
        if (role != null && !role.equals(user.getRole()) && !currentUser.isAdmin()) {
            throw new RuntimeException("Access denied: Only admins can change user roles");
        }
        
        // Update fields
        if (email != null && !email.equals(user.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                throw new RuntimeException("Email already exists");
            }
            user.setEmail(email);
        }
        
        if (firstName != null) {
            user.setFirstName(firstName);
        }
        
        if (lastName != null) {
            user.setLastName(lastName);
        }
        
        if (role != null && currentUser.isAdmin()) {
            user.setRole(role);
        }
        
        User updatedUser = userRepository.save(user);
        
        // Audit log
        createAuditLog(currentUser, "USER_UPDATED", 
                "Updated user: " + user.getUsername(), true, request);
        
        log.info("User updated: {} by {}", userId, currentUser.getUsername());
        return updatedUser;
    }

    /**
     * Delete user (soft delete - deactivate)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteUser(UUID userId, HttpServletRequest request) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Cannot delete yourself
        if (currentUser.getId().equals(userId)) {
            throw new RuntimeException("Cannot delete your own account");
        }
        
        // Soft delete by deactivating
        authenticationService.deactivateUser(user, currentUser, request);
        
        log.info("User deleted (deactivated): {} by {}", userId, currentUser.getUsername());
    }

    /**
     * Search users by name or username (admin only)
     */
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> searchUsers(String query) {
        log.debug("Searching users with query: {}", query);
        return userRepository.searchByNameOrUsername(query);
    }

    /**
     * Count users by role
     */
    @PreAuthorize("hasRole('ADMIN')")
    public Long countByRole(UserRole role) {
        return userRepository.countByRole(role);
    }

    /**
     * Get user statistics (admin only)
     */
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Long> getUserStatistics() {
        return Map.of(
            "total", userRepository.count(),
            "active", userRepository.countByIsActiveTrue(),
            "admins", userRepository.countByRole(UserRole.ADMIN),
            "recruiters", userRepository.countByRole(UserRole.RECRUITER),
            "hr", userRepository.countByRole(UserRole.HR),
            "hiringManagers", userRepository.countByRole(UserRole.HIRING_MANAGER)
        );
    }

    /**
     * Get active users only
     */
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> getActiveUsers() {
        return userRepository.findByIsActiveTrue();
    }

    /**
     * Get inactive users only
     */
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> getInactiveUsers() {
        return userRepository.findByIsActiveFalse();
    }

    /**
     * Helper method to create audit log
     */
    private void createAuditLog(User user, String action, String details, 
                                boolean success, HttpServletRequest request) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUser(user);
        auditLog.setUsername(user.getUsername());
        auditLog.setAction(action);
        auditLog.setDetails(details);
        auditLog.setSuccess(success);
        if (request != null) {
            auditLog.setIpAddress(authenticationService.getClientIpAddress(request));
            auditLog.setUserAgent(request.getHeader("User-Agent"));
        }
        auditLog.setCreatedAt(LocalDateTime.now());
        
        auditLogRepository.save(auditLog);
    }
}
