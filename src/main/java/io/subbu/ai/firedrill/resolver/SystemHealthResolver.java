package io.subbu.ai.firedrill.resolver;

import io.subbu.ai.firedrill.entities.SystemHealth;
import io.subbu.ai.firedrill.models.ServiceStatus;
import io.subbu.ai.firedrill.services.SystemHealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * GraphQL resolver for system health queries and mutations.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class SystemHealthResolver {

    private final SystemHealthService systemHealthService;

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<SystemHealth> systemHealthReport() {
        log.info("Fetching system health report");
        return systemHealthService.getSystemHealthReport();
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ServiceStatus overallSystemStatus() {
        log.info("Fetching overall system status");
        var statusMap = systemHealthService.getOverallSystemStatus();
        return (ServiceStatus) statusMap.get("overallStatus");
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public SystemHealth systemHealth(@Argument String serviceName) {
        log.info("Fetching health for service: {}", serviceName);
        return systemHealthService.getServiceHealth(serviceName)
                .orElseThrow(() -> new RuntimeException("Service not found: " + serviceName));
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public SystemHealth checkServiceHealth(@Argument String serviceName) {
        log.info("Triggering health check for service: {}", serviceName);
        return systemHealthService.checkServiceHealth(serviceName);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public SystemHealth resetFailureCount(@Argument String serviceName) {
        log.info("Resetting failure count for service: {}", serviceName);
        systemHealthService.resetFailureCount(serviceName);
        return systemHealthService.getServiceHealth(serviceName)
                .orElseThrow(() -> new RuntimeException("Service not found: " + serviceName));
    }
}
