package io.subbu.ai.firedrill.entities;

import io.subbu.ai.firedrill.models.ServiceStatus;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * System health entity for monitoring service health
 */
@Entity
@Table(name = "system_health")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemHealth {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "service_name", nullable = false, unique = true, length = 50)
    private String serviceName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ServiceStatus status;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @Column(name = "last_checked_at")
    private LocalDateTime lastCheckedAt;

    @Column(name = "last_success_at")
    private LocalDateTime lastSuccessAt;

    @Column(name = "last_failure_at")
    private LocalDateTime lastFailureAt;

    @Column(name = "failure_count", nullable = false)
    private Integer failureCount = 0;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> details;

    /**
     * Check if service is healthy
     */
    public boolean isHealthy() {
        return this.status == ServiceStatus.UP;
    }

    /**
     * Check if service is down
     */
    public boolean isDown() {
        return this.status == ServiceStatus.DOWN;
    }

    /**
     * Record successful health check
     */
    public void recordSuccess(int responseTime) {
        this.status = ServiceStatus.UP;
        this.lastCheckedAt = LocalDateTime.now();
        this.lastSuccessAt = LocalDateTime.now();
        this.responseTimeMs = responseTime;
        this.failureCount = 0;
        this.message = "Service is operational";
    }

    /**
     * Record successful health check with custom message
     */
    public void recordSuccess(long responseTime, String message) {
        this.status = ServiceStatus.UP;
        this.lastCheckedAt = LocalDateTime.now();
        this.lastSuccessAt = LocalDateTime.now();
        this.responseTimeMs = (int) responseTime;
        this.failureCount = 0;
        this.message = message;
    }

    /**
     * Record failed health check
     */
    public void recordFailure(String errorMessage) {
        this.status = ServiceStatus.DOWN;
        this.lastCheckedAt = LocalDateTime.now();
        this.lastFailureAt = LocalDateTime.now();
        this.failureCount++;
        this.message = errorMessage;
    }

    /**
     * Record failed health check with response time
     */
    public void recordFailure(long responseTime, String errorMessage) {
        this.status = ServiceStatus.DOWN;
        this.lastCheckedAt = LocalDateTime.now();
        this.lastFailureAt = LocalDateTime.now();
        this.responseTimeMs = (int) responseTime;
        this.failureCount++;
        this.message = errorMessage;
    }

    /**
     * Record degraded health check
     */
    public void recordDegraded(String reason, int responseTime) {
        this.status = ServiceStatus.DEGRADED;
        this.lastCheckedAt = LocalDateTime.now();
        this.responseTimeMs = responseTime;
        this.message = reason;
    }
}
