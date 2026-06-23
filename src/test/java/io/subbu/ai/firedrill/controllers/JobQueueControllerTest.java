package io.subbu.ai.firedrill.controllers;

import io.subbu.ai.firedrill.config.JwtTokenProvider;
import io.subbu.ai.firedrill.entities.JobQueue;
import io.subbu.ai.firedrill.models.JobPriority;
import io.subbu.ai.firedrill.models.JobStatus;
import io.subbu.ai.firedrill.models.JobType;
import io.subbu.ai.firedrill.repositories.UserRepository;
import io.subbu.ai.firedrill.services.JobQueueService;
import io.subbu.ai.firedrill.services.JobSchedulerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = JobQueueController.class)
@TestPropertySource(properties = {"app.scheduler.enabled=true"})
@WithMockUser(username = "testuser", roles = {"ADMIN"})
class JobQueueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JobQueueService jobQueueService;

    @MockBean
    private JobSchedulerService jobSchedulerService;
    
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    
    @MockBean
    private UserRepository userRepository;

    private UUID testJobId;
    private String testCorrelationId;

    @BeforeEach
    void setUp() {
        testJobId = UUID.randomUUID();
        testCorrelationId = "test-correlation-id";
    }

    @Test
    void testGetQueueHealth_Success() throws Exception {
        // Arrange
        Map<String, Object> health = new HashMap<>();
        health.put("activeJobs", 2);
        health.put("pendingJobs", 5L);
        health.put("processingJobs", 3L);
        health.put("averageProcessingTime", 45.5);

        when(jobSchedulerService.getQueueHealth()).thenReturn(health);

        // Act & Assert
        mockMvc.perform(get("/api/jobs/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeJobs").value(2))
                .andExpect(jsonPath("$.pendingJobs").value(5))
                .andExpect(jsonPath("$.processingJobs").value(3))
                .andExpect(jsonPath("$.averageProcessingTime").value(45.5));

        verify(jobSchedulerService).getQueueHealth();
    }

    @Test
    void testGetQueueStatistics_Success() throws Exception {
        // Arrange
        when(jobQueueService.getJobCount(JobStatus.PENDING)).thenReturn(10L);
        when(jobQueueService.getJobCount(JobStatus.PROCESSING)).thenReturn(3L);
        when(jobQueueService.getJobCount(JobStatus.COMPLETED)).thenReturn(50L);
        when(jobQueueService.getJobCount(JobStatus.FAILED)).thenReturn(2L);
        when(jobQueueService.getJobCount(JobStatus.CANCELLED)).thenReturn(1L);
        when(jobQueueService.getQueueDepth(JobType.RESUME_PROCESSING)).thenReturn(10L);
        when(jobQueueService.getAverageProcessingDuration(JobType.RESUME_PROCESSING)).thenReturn(35.0);
        when(jobSchedulerService.getActiveJobCount()).thenReturn(3);

        Map<JobStatus, Long> stats = new HashMap<>();
        stats.put(JobStatus.PENDING, 10L);
        stats.put(JobStatus.COMPLETED, 50L);
        when(jobQueueService.getJobStats(JobType.RESUME_PROCESSING)).thenReturn(stats);

        // Act & Assert
        mockMvc.perform(get("/api/jobs/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingCount").value(10))
                .andExpect(jsonPath("$.processingCount").value(3))
                .andExpect(jsonPath("$.completedCount").value(50))
                .andExpect(jsonPath("$.failedCount").value(2))
                .andExpect(jsonPath("$.queueDepth").value(10))
                .andExpect(jsonPath("$.activeWorkers").value(3));
    }

    @Test
    void testGetJob_Found() throws Exception {
        // Arrange
        JobQueue job = createMockJob(testJobId, JobStatus.COMPLETED);
        when(jobQueueService.getJob(testJobId)).thenReturn(Optional.of(job));

        // Act & Assert
        mockMvc.perform(get("/api/jobs/{jobId}", testJobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testJobId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(jobQueueService).getJob(testJobId);
    }

    @Test
    void testGetJob_NotFound() throws Exception {
        // Arrange
        when(jobQueueService.getJob(testJobId)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/jobs/{jobId}", testJobId))
                .andExpect(status().isNotFound());

        verify(jobQueueService).getJob(testJobId);
    }

    @Test
    void testGetJobsByCorrelationId_Success() throws Exception {
        // Arrange
        JobQueue job1 = createMockJob(UUID.randomUUID(), JobStatus.PENDING);
        JobQueue job2 = createMockJob(UUID.randomUUID(), JobStatus.COMPLETED);
        List<JobQueue> jobs = Arrays.asList(job1, job2);

        when(jobQueueService.getJobsByCorrelationId(testCorrelationId)).thenReturn(jobs);

        // Act & Assert
        mockMvc.perform(get("/api/jobs/correlation/{correlationId}", testCorrelationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(jobQueueService).getJobsByCorrelationId(testCorrelationId);
    }

    @Test
    void testGetJobsByStatus_Success() throws Exception {
        // Arrange
        JobQueue job = createMockJob(testJobId, JobStatus.PENDING);
        Page<JobQueue> page = new PageImpl<>(Collections.singletonList(job));

        when(jobQueueService.getJobsByStatus(
                eq(JobStatus.PENDING),
                any(PageRequest.class)
        )).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/jobs/status/{status}", "PENDING")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));

        verify(jobQueueService).getJobsByStatus(
                eq(JobStatus.PENDING),
                any(PageRequest.class)
        );
    }

    @Test
    void testCancelJob_Success() throws Exception {
        // Arrange
        when(jobQueueService.cancelJob(testJobId)).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/api/jobs/{jobId}/cancel", testJobId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(testJobId.toString()))
                .andExpect(jsonPath("$.cancelled").value(true))
                .andExpect(jsonPath("$.message").value("Job cancelled successfully"));

        verify(jobQueueService).cancelJob(testJobId);
    }

    @Test
    void testCancelJob_Failure() throws Exception {
        // Arrange
        when(jobQueueService.cancelJob(testJobId)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/api/jobs/{jobId}/cancel", testJobId).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.jobId").value(testJobId.toString()))
                .andExpect(jsonPath("$.cancelled").value(false));

        verify(jobQueueService).cancelJob(testJobId);
    }

    @Test
    void testResetStaleJobs_Success() throws Exception {
        // Arrange
        when(jobQueueService.resetStaleJobs()).thenReturn(3);

        // Act & Assert
        mockMvc.perform(post("/api/jobs/stale/reset").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resetCount").value(3))
                .andExpect(jsonPath("$.message").value("Reset 3 stale jobs"));

        verify(jobQueueService).resetStaleJobs();
    }

    @Test
    void testResetStaleJobs_NoStaleJobs() throws Exception {
        // Arrange
        when(jobQueueService.resetStaleJobs()).thenReturn(0);

        // Act & Assert
        mockMvc.perform(post("/api/jobs/stale/reset").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resetCount").value(0))
                .andExpect(jsonPath("$.message").value("No stale jobs found"));

        verify(jobQueueService).resetStaleJobs();
    }

    @Test
    @Disabled("Methods countJobsInTimeRange, countCompletedJobsInTimeRange, countFailedJobsInTimeRange not implemented in JobQueueService")
    void testGetRecentMetrics_Success() throws Exception {
        // Arrange
        when(jobQueueService.countJobsInTimeRange(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(100L);
        when(jobQueueService.countCompletedJobsInTimeRange(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(90L);
        when(jobQueueService.countFailedJobsInTimeRange(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(10L);
        when(jobQueueService.getAverageProcessingDuration(JobType.RESUME_PROCESSING))
                .thenReturn(42.5);

        // Act & Assert
        mockMvc.perform(get("/api/jobs/metrics/recent")
                        .param("hours", "24"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalJobs").value(100))
                .andExpect(jsonPath("$.completedJobs").value(90))
                .andExpect(jsonPath("$.failedJobs").value(10))
                .andExpect(jsonPath("$.successRate").value("90.00%"))
                .andExpect(jsonPath("$.failureRate").value("10.00%"));

        verify(jobQueueService).countJobsInTimeRange(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(jobQueueService).countCompletedJobsInTimeRange(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(jobQueueService).countFailedJobsInTimeRange(any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void testCleanupOldJobs_Success() throws Exception {
        // Arrange
        when(jobQueueService.cleanupOldJobs(30)).thenReturn(25);

        // Act & Assert
        mockMvc.perform(post("/api/jobs/cleanup")
                        .param("daysToKeep", "30")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedCount").value(25))
                .andExpect(jsonPath("$.daysToKeep").value(30))
                .andExpect(jsonPath("$.message").value("Deleted 25 old completed jobs"));

        verify(jobQueueService).cleanupOldJobs(30);
    }

    @Test
    void testGetQueueDepth_Success() throws Exception {
        // Arrange
        when(jobQueueService.getQueueDepth(JobType.RESUME_PROCESSING)).thenReturn(15L);

        // Act & Assert
        mockMvc.perform(get("/api/jobs/queue/depth"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RESUME_PROCESSING").value(15))
                .andExpect(jsonPath("$.total").value(15));

        verify(jobQueueService).getQueueDepth(JobType.RESUME_PROCESSING);
    }

    // Helper method
    private JobQueue createMockJob(UUID id, JobStatus status) {
        return JobQueue.builder()
                .id(id)
                .jobType(JobType.RESUME_PROCESSING)
                .status(status)
                .priority(JobPriority.NORMAL.getValue())
                .correlationId(testCorrelationId)
                .retryCount(0)
                .maxRetries(3)
                .build();
    }
}
