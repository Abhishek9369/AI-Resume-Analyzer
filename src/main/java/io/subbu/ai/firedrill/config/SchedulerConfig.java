package io.subbu.ai.firedrill.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration for job scheduler and executor services.
 * Only enabled when app.scheduler.enabled=true.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "true", matchIfMissing = false)
@EnableScheduling
public class SchedulerConfig {

    @Value("${app.scheduler.thread-pool-size:5}")
    private int threadPoolSize;

    @Value("${app.scheduler.worker-id:default-worker}")
    private String workerId;

    /**
     * Configure ExecutorService for job processing.
     * Jobs claimed from the queue will be executed using this thread pool.
     * 
     * @return ExecutorService configured with custom thread pool
     */
    @Bean(name = "jobExecutorService", destroyMethod = "shutdown")
    public ExecutorService jobExecutorService() {
        log.info("Configuring job executor service: threadPoolSize={}, workerId={}", 
                 threadPoolSize, workerId);
        
        ExecutorService executorService = Executors.newFixedThreadPool(
            threadPoolSize,
            r -> {
                Thread thread = new Thread(r);
                thread.setName("job-processor-" + Thread.currentThread().getId());
                thread.setDaemon(false); // Ensure threads complete before shutdown
                return thread;
            }
        );
        
        log.info("Job executor service configured successfully");
        return executorService;
    }

    /**
     * Log configuration details on startup.
     */
    @Bean
    public SchedulerConfigLogger schedulerConfigLogger() {
        return new SchedulerConfigLogger(threadPoolSize, workerId);
    }

    /**
     * Helper class to log scheduler configuration on startup.
     */
    @Slf4j
    private static class SchedulerConfigLogger {
        public SchedulerConfigLogger(int threadPoolSize, String workerId) {
            log.info("=============================================================");
            log.info("  JOB SCHEDULER CONFIGURATION");
            log.info("=============================================================");
            log.info("  Scheduler Enabled: true");
            log.info("  Thread Pool Size: {}", threadPoolSize);
            log.info("  Worker ID: {}", workerId);
            log.info("=============================================================");
            log.info("  NOTE: Scheduler mode is ACTIVE");
            log.info("  File uploads will be queued and processed by scheduler");
            log.info("  Legacy async processing is DISABLED");
            log.info("=============================================================");
        }
    }
}
