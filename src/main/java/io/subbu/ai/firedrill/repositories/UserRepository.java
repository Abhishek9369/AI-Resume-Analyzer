package io.subbu.ai.firedrill.repositories;

import io.subbu.ai.firedrill.entities.User;
import io.subbu.ai.firedrill.models.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);

    /**
     * Find all users by role
     */
    List<User> findByRole(UserRole role);

    /**
     * Find all active users
     */
    List<User> findByIsActive(boolean isActive);

    /**
     * Find all users by role and active status
     */
    List<User> findByRoleAndIsActive(UserRole role, boolean isActive);

    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Count users by role
     */
    long countByRole(UserRole role);

    /**
     * Count active users
     */
    long countByIsActive(boolean isActive);

    /**
     * Count active users (convenience method)
     */
    long countByIsActiveTrue();

    /**
     * Find all active users (convenience method)
     */
    List<User> findByIsActiveTrue();

    /**
     * Find all inactive users (convenience method)
     */
    List<User> findByIsActiveFalse();

    /**
     * Search users by name or username (case-insensitive)
     */
    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<User> searchByNameOrUsername(@org.springframework.data.repository.query.Param("searchTerm") String searchTerm);
}
