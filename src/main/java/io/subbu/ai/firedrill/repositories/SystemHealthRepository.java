package io.subbu.ai.firedrill.repositories;

import io.subbu.ai.firedrill.entities.SystemHealth;
import io.subbu.ai.firedrill.models.ServiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for SystemHealth entity
 */
@Repository
public interface SystemHealthRepository extends JpaRepository<SystemHealth, UUID> {

    /**
     * Find system health by service name
     */
    Optional<SystemHealth> findByServiceName(String serviceName);

    /**
     * Find all health checks by status
     */
    List<SystemHealth> findByStatus(ServiceStatus status);

    /**
     * Find all unhealthy services
     */
    List<SystemHealth> findByStatusIn(List<ServiceStatus> statuses);

    /**
     * Find all services ordered by last checked time
     */
    List<SystemHealth> findAllByOrderByLastCheckedAtDesc();

    /**
     * Check if service exists
     */
    boolean existsByServiceName(String serviceName);

    /**
     * Count services by status
     */
    long countByStatus(ServiceStatus status);

    /**
     * Delete health check by service name
     */
    void deleteByServiceName(String serviceName);
}
