package io.subbu.ai.firedrill.services;

import io.subbu.ai.firedrill.entities.SystemHealth;
import io.subbu.ai.firedrill.models.ServiceStatus;
import io.subbu.ai.firedrill.repositories.SystemHealthRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for system health monitoring
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemHealthService {

    private final SystemHealthRepository systemHealthRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${spring.ai.openai.base-url:http://localhost:1234/v1}")
    private String llmStudioBaseUrl;

    private static final int TIMEOUT_MS = 5000;

    /**
     * Initialize health records for all services
     */
    @PostConstruct
    public void initializeHealthRecords() {
        createOrUpdateHealthRecord("database", "PostgreSQL Database");
        createOrUpdateHealthRecord("llm-studio", "LM Studio API");
        createOrUpdateHealthRecord("application", "Spring Boot Application");
    }

    /**
     * Check all services health (scheduled every 5 minutes)
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void checkAllServices() {
        log.debug("Running scheduled health checks");
        checkDatabaseHealth();
        checkLLMStudioHealth();
        checkApplicationHealth();
    }

    /**
     * Get system health report (admin only)
     */
    @PreAuthorize("hasRole('ADMIN')")
    public List<SystemHealth> getSystemHealthReport() {
        return systemHealthRepository.findAll();
    }

    /**
     * Get health status for a specific service
     */
    @PreAuthorize("hasRole('ADMIN')")
    public Optional<SystemHealth> getServiceHealth(String serviceName) {
        return systemHealthRepository.findByServiceName(serviceName);
    }

    /**
     * Check database health
     */
    @Transactional
    public SystemHealth checkDatabaseHealth() {
        long startTime = System.currentTimeMillis();
        SystemHealth health = getOrCreateHealthRecord("database", "PostgreSQL Database");
        
        try {
            // Simple query to check database connectivity
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            
            long responseTime = System.currentTimeMillis() - startTime;
            health.recordSuccess(responseTime, "Database connection successful");
            
            log.debug("Database health check: SUCCESS ({}ms)", responseTime);
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            health.recordFailure(responseTime, "Database connection failed: " + e.getMessage());
            
            log.error("Database health check: FAILED", e);
        }
        
        return systemHealthRepository.save(health);
    }

    /**
     * Check LLM Studio health
     */
    @Transactional
    public SystemHealth checkLLMStudioHealth() {
        long startTime = System.currentTimeMillis();
        SystemHealth health = getOrCreateHealthRecord("llm-studio", "LM Studio API");
        
        try {
            // Parse host and port from the configured LLM Studio base URL
            // (e.g. http://host.docker.internal:1234/v1 inside Docker, http://localhost:1234/v1 locally)
            java.net.URI uri = new java.net.URI(llmStudioBaseUrl);
            String llmHost = uri.getHost() != null ? uri.getHost() : "localhost";
            int llmPort = uri.getPort() > 0 ? uri.getPort() : 1234;

            // Try to connect to LM Studio port
            try (Socket socket = new Socket()) {
                socket.connect(new java.net.InetSocketAddress(llmHost, llmPort), TIMEOUT_MS);
                
                long responseTime = System.currentTimeMillis() - startTime;
                health.recordSuccess(responseTime, "LM Studio is running on " + llmHost + ":" + llmPort);
                
                log.debug("LLM Studio health check: SUCCESS ({}ms, host: {}:{})", responseTime, llmHost, llmPort);
            }
        } catch (IOException e) {
            long responseTime = System.currentTimeMillis() - startTime;
            health.recordFailure(responseTime, 
                "LM Studio is not running or not accessible at " + llmStudioBaseUrl);
            
            log.warn("LLM Studio health check: NOT RUNNING at {} (expected if LM Studio is not started)", llmStudioBaseUrl);
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            health.recordFailure(responseTime,
                "LM Studio health check failed: " + e.getMessage());

            log.error("LLM Studio health check: FAILED", e);
        }
        
        return systemHealthRepository.save(health);
    }

    /**
     * Check application health
     */
    @Transactional
    public SystemHealth checkApplicationHealth() {
        long startTime = System.currentTimeMillis();
        SystemHealth health = getOrCreateHealthRecord("application", "Spring Boot Application");
        
        try {
            // Check application is running (if we got here, it is)
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Set status based on memory usage
            if (memoryUsagePercent > 90) {
                health.recordDegraded(
                    String.format("High memory usage: %.2f%% (Used: %dMB, Max: %dMB)", 
                        memoryUsagePercent, usedMemory / 1024 / 1024, maxMemory / 1024 / 1024), 
                    (int) responseTime);
            } else {
                health.recordSuccess(responseTime,
                    String.format("Application healthy. Memory usage: %.2f%% (Used: %dMB, Max: %dMB)",
                        memoryUsagePercent, usedMemory / 1024 / 1024, maxMemory / 1024 / 1024));
            }
            
            log.debug("Application health check: SUCCESS ({}ms, memory: {:.2f}%)", 
                    responseTime, memoryUsagePercent);
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            health.recordFailure(responseTime, "Application health check failed: " + e.getMessage());
            
            log.error("Application health check: FAILED", e);
        }
        
        return systemHealthRepository.save(health);
    }

    /**
     * Get overall system status
     */
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> getOverallSystemStatus() {
        List<SystemHealth> allHealth = systemHealthRepository.findAll();
        
        long upCount = systemHealthRepository.countByStatus(ServiceStatus.UP);
        long downCount = systemHealthRepository.countByStatus(ServiceStatus.DOWN);
        long degradedCount = systemHealthRepository.countByStatus(ServiceStatus.DEGRADED);
        
        // Overall status: DOWN if any service is down, DEGRADED if any degraded, otherwise UP
        ServiceStatus overallStatus;
        if (downCount > 0) {
            overallStatus = ServiceStatus.DOWN;
        } else if (degradedCount > 0) {
            overallStatus = ServiceStatus.DEGRADED;
        } else {
            overallStatus = ServiceStatus.UP;
        }
        
        return Map.of(
            "overallStatus", overallStatus,
            "services", allHealth,
            "summary", Map.of(
                "up", upCount,
                "down", downCount,
                "degraded", degradedCount,
                "total", allHealth.size()
            )
        );
    }

    /**
     * Manually trigger health check for a specific service
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public SystemHealth checkServiceHealth(String serviceName) {
        return switch (serviceName.toLowerCase()) {
            case "database" -> checkDatabaseHealth();
            case "llm-studio" -> checkLLMStudioHealth();
            case "application" -> checkApplicationHealth();
            default -> throw new RuntimeException("Unknown service: " + serviceName);
        };
    }

    /**
     * Get or create health record for a service
     */
    private SystemHealth getOrCreateHealthRecord(String serviceName, String description) {
        return systemHealthRepository.findByServiceName(serviceName)
                .orElseGet(() -> createOrUpdateHealthRecord(serviceName, description));
    }

    /**
     * Create or update health record
     */
    private SystemHealth createOrUpdateHealthRecord(String serviceName, String description) {
        Optional<SystemHealth> existing = systemHealthRepository.findByServiceName(serviceName);
        
        if (existing.isPresent()) {
            return existing.get();
        } else {
            SystemHealth health = new SystemHealth();
            health.setServiceName(serviceName);
            health.setStatus(ServiceStatus.UNKNOWN);
            health.setMessage(description + " - Not yet checked");
            health.setLastCheckedAt(LocalDateTime.now());
            return systemHealthRepository.save(health);
        }
    }

    /**
     * Reset failure count for a service (admin only)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void resetFailureCount(String serviceName) {
        SystemHealth health = systemHealthRepository.findByServiceName(serviceName)
                .orElseThrow(() -> new RuntimeException("Service not found: " + serviceName));
        
        health.setFailureCount(0);
        systemHealthRepository.save(health);
        
        log.info("Reset failure count for service: {}", serviceName);
    }
}
