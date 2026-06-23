package io.subbu.ai.firedrill.repositories;

import io.subbu.ai.firedrill.entities.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for AuditLog entity
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Find all audit logs for a user
     */
    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find audit logs by action
     */
    Page<AuditLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);

    /**
     * Find audit logs for a specific entity
     */
    Page<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
        String entityType, UUID entityId, Pageable pageable
    );

    /**
     * Find audit logs within a time range
     */
    Page<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
        LocalDateTime startTime, LocalDateTime endTime, Pageable pageable
    );

    /**
     * Find failed audit logs
     */
    Page<AuditLog> findBySuccessFalseOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Count audit logs by user
     */
    long countByUserId(UUID userId);

    /**
     * Count failed actions
     */
    long countBySuccessFalse();

    /**
     * Get recent audit logs
     */
    @Query("SELECT a FROM AuditLog a ORDER BY a.createdAt DESC")
    List<AuditLog> findRecentLogs(Pageable pageable);

    /**
     * Search audit logs
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
           "LOWER(a.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(a.action) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(a.details) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<AuditLog> searchLogs(@Param("searchTerm") String searchTerm, Pageable pageable);
}
