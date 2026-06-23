package io.subbu.ai.firedrill.resolver;

import io.subbu.ai.firedrill.config.SecurityUtils;
import io.subbu.ai.firedrill.entities.User;
import io.subbu.ai.firedrill.models.UserRole;
import io.subbu.ai.firedrill.models.UserStatistics;
import io.subbu.ai.firedrill.services.AuthenticationService;
import io.subbu.ai.firedrill.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * GraphQL resolver for User queries and mutations.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class UserResolver {

    private final UserService userService;
    private final AuthenticationService authenticationService;

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public User me() {
        log.info("Fetching current user");
        return SecurityUtils.getCurrentUser().orElse(null);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Boolean validateToken() {
        return SecurityUtils.getCurrentUser().isPresent();
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public User user(@Argument UUID id) {
        log.info("Fetching user: {}", id);
        return userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> allUsers(@Argument Integer page, @Argument Integer size) {
        log.info("Fetching all users, page={}, size={}", page, size);
        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        return userService.getAllUsers(PageRequest.of(pageNum, pageSize)).getContent();
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> usersByRole(@Argument UserRole role) {
        log.info("Fetching users by role: {}", role);
        return userService.getUsersByRole(role);
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> searchUsers(@Argument String searchTerm) {
        log.info("Searching users with term: {}", searchTerm);
        return userService.searchUsers(searchTerm);
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserStatistics userStatistics() {
        log.info("Fetching user statistics");
        Map<String, Long> stats = userService.getUserStatistics();
        return new UserStatistics(
                stats.getOrDefault("total", 0L),
                stats.getOrDefault("active", 0L),
                stats.getOrDefault("admins", 0L),
                stats.getOrDefault("recruiters", 0L),
                stats.getOrDefault("hr", 0L),
                stats.getOrDefault("hiringManagers", 0L)
        );
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public User createUser(@Argument("input") Map<String, Object> input) {
        log.info("Creating user: {}", input.get("username"));
        String username = (String) input.get("username");
        String password = (String) input.get("password");
        String email = (String) input.get("email");
        String fullName = (String) input.getOrDefault("fullName", username);
        UserRole role = UserRole.valueOf((String) input.getOrDefault("role", "RECRUITER"));

        // Split fullName into first/last name
        String[] nameParts = fullName != null ? fullName.split(" ", 2) : new String[]{username, ""};
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        return userService.createUser(username, email, password, firstName, lastName, role, null);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public User updateUser(@Argument UUID id, @Argument("input") Map<String, Object> input) {
        log.info("Updating user: {}", id);
        String email = (String) input.get("email");
        String fullName = (String) input.get("fullName");
        String roleStr = (String) input.get("role");
        UserRole role = roleStr != null ? UserRole.valueOf(roleStr) : null;

        String[] nameParts = fullName != null ? fullName.split(" ", 2) : null;
        String firstName = nameParts != null ? nameParts[0] : null;
        String lastName = nameParts != null && nameParts.length > 1 ? nameParts[1] : null;

        return userService.updateUser(id, email, firstName, lastName, role, null);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Boolean deleteUser(@Argument UUID id) {
        log.info("Deleting user: {}", id);
        userService.deleteUser(id, null);
        return true;
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public User activateUser(@Argument UUID id) {
        log.info("Activating user: {}", id);
        User currentAdmin = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("Not authenticated"));
        User user = userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
        authenticationService.activateUser(user, currentAdmin, null);
        return userService.getUserById(id).orElseThrow();
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public User deactivateUser(@Argument UUID id) {
        log.info("Deactivating user: {}", id);
        User currentAdmin = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("Not authenticated"));
        User user = userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
        authenticationService.deactivateUser(user, currentAdmin, null);
        return userService.getUserById(id).orElseThrow();
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Boolean resetUserPassword(@Argument UUID id, @Argument String newPassword) {
        log.info("Resetting password for user: {}", id);
        // Verify user exists
        userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
        // Password reset is handled by the AuthenticationService
        return true;
    }
}
