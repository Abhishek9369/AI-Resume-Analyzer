package io.subbu.ai.firedrill.services;

import io.subbu.ai.firedrill.entities.JobQueue;
import io.subbu.ai.firedrill.models.JobPriority;
import io.subbu.ai.firedrill.models.JobStatus;
import io.subbu.ai.firedrill.models.JobType;
import io.subbu.ai.firedrill.repositories.JobQueueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobQueueServiceTest {

    @Mock
    private JobQueueRepository jobQueueRepository;

    @InjectMocks
    private JobQueueService jobQueueService;

    private UUID testJobId;
    private String testCorrelationId;
    private Map<String, Object> testMetadata;
    private byte[] testFileData;

    @BeforeEach
    void setUp() {
        testJobId = UUID.randomUUID();
        testCorrelationId = "test-correlation-id";
        testMetadata = new HashMap<>();
        testMetadata.put("filename", "test-resume.pdf");
        testMetadata.put("trackerId", UUID.randomUUID().toString());
        testFileData = "test file content".getBytes();

        // Set default configuration values
        ReflectionTestUtils.setField(jobQueueService, "staleJobThresholdMinutes", 15);
        ReflectionTestUtils.setField(jobQueueService, "workerId", "test-worker");
    }

    @Test
    void testCreateJob_Success() {
        // Arrange
        JobQueue expectedJob = JobQueue.builder()
                .id(testJobId)
                .jobType(JobType.RESUME_PROCESSING)
                .status(JobStatus.PENDING)
                .priority(JobPriority.NORMAL.getValue())
                .fileData(testFileData)
                .metadata(testMetadata)
                .correlationId(testCorrelationId)
                .retryCount(0)
                .maxRetries(3)
                .build();

        when(jobQueueRepository.save(any(JobQueue.class))).thenReturn(expectedJob);

        // Act
        JobQueue result = jobQueueService.createJob(
                JobType.RESUME_PROCESSING,
                testFileData,
                testMetadata,
                JobPriority.NORMAL,
                testCorrelationId
        );

        // Assert
        assertNotNull(result);
        assertEquals(JobType.RESUME_PROCESSING, result.getJobType());
        assertEquals(JobStatus.PENDING, result.getStatus());
        assertEquals(JobPriority.NORMAL.getValue(), result.getPriority());
        assertEquals(testCorrelationId, result.getCorrelationId());
        assertEquals(0, result.getRetryCount());
        assertEquals(3, result.getMaxRetries());

        verify(jobQueueRepository).save(any(JobQueue.class));
    }

    @Test
    void testCreateJob_WithDefaultPriority() {
        // Arrange
        JobQueue expectedJob = JobQueue.builder()
                .id(testJobId)
                .jobType(JobType.RESUME_PROCESSING)
                .status(JobStatus.PENDING)
                .priority(JobPriority.NORMAL.getValue())
                .build();

        when(jobQueueRepository.save(any(JobQueue.class))).thenReturn(expectedJob);

        // Act
        JobQueue result = jobQueueService.createJob(
                JobType.RESUME_PROCESSING,
                testFileData,
                testMetadata,
                testCorrelationId
        );

        // Assert
        assertNotNull(result);
        assertEquals(JobPriority.NORMAL.getValue(), result.getPriority());
        verify(jobQueueRepository).save(any(JobQueue.class));
    }

    @Test
    void testCreateScheduledJob_Success() {
        // Arrange
        LocalDateTime scheduledTime = LocalDateTime.now().plusHours(1);
        JobQueue expectedJob = JobQueue.builder()
                .id(testJobId)
                .jobType(JobType.RESUME_PROCESSING)
                .status(JobStatus.PENDING)
                .scheduledFor(scheduledTime)
                .build();

        when(jobQueueRepository.save(any(JobQueue.class))).thenReturn(expectedJob);

        // Act
        JobQueue result = jobQueueService.createScheduledJob(
                JobType.RESUME_PROCESSING,
                testFileData,
                testMetadata,
                JobPriority.HIGH,
                testCorrelationId,
                scheduledTime
        );

        // Assert
        assertNotNull(result);
        assertEquals(scheduledTime, result.getScheduledFor());
        verify(jobQueueRepository).save(any(JobQueue.class));
    }

    @Test
    void testClaimJobs_Success() {
        // Arrange
        JobQueue job1 = createMockJob(UUID.randomUUID(), JobStatus.PENDING);
        JobQueue job2 = createMockJob(UUID.randomUUID(), JobStatus.PENDING);
        List<JobQueue> pendingJobs = Arrays.asList(job1, job2);

        when(jobQueueRepository.findAndLockPendingJobs(
                eq(JobType.RESUME_PROCESSING),
                any(Pageable.class)
        )).thenReturn(pendingJobs);

        when(jobQueueRepository.saveAll(anyList())).thenReturn(pendingJobs);

        // Act
        List<JobQueue> result = jobQueueService.claimJobs(JobType.RESUME_PROCESSING, 5);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(jobQueueRepository).findAndLockPendingJobs(
                eq(JobType.RESUME_PROCESSING),
                any(Pageable.class)
        );
        verify(jobQueueRepository).saveAll(anyList());
    }

    @Test
    void testClaimJobs_NoPendingJobs() {
        // Arrange
        when(jobQueueRepository.findAndLockPendingJobs(
                eq(JobType.RESUME_PROCESSING),
                any(Pageable.class)
        )).thenReturn(Collections.emptyList());

        // Act
        List<JobQueue> result = jobQueueService.claimJobs(JobType.RESUME_PROCESSING, 5);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(jobQueueRepository).findAndLockPendingJobs(
                eq(JobType.RESUME_PROCESSING),
                any(Pageable.class)
        );
        verify(jobQueueRepository, never()).saveAll(anyList());
    }

    @Test
    void testUpdateHeartbeat_Success() {
        // Arrange
        JobQueue job = createMockJob(testJobId, JobStatus.PROCESSING);
        when(jobQueueRepository.findById(testJobId)).thenReturn(Optional.of(job));
        when(jobQueueRepository.save(any(JobQueue.class))).thenReturn(job);

        // Act
        jobQueueService.updateHeartbeat(testJobId);

        // Assert
        verify(jobQueueRepository).findById(testJobId);
        verify(jobQueueRepository).save(any(JobQueue.class));
    }

    @Test
    void testUpdateHeartbeat_JobNotFound() {
        // Arrange
        when(jobQueueRepository.findById(testJobId)).thenReturn(Optional.empty());

        // Act
        jobQueueService.updateHeartbeat(testJobId);

        // Assert
        verify(jobQueueRepository).findById(testJobId);
        verify(jobQueueRepository, never()).save(any(JobQueue.class));
    }

    @Test
    void testMarkJobCompleted_Success() {
        // Arrange
        JobQueue job = createMockJob(testJobId, JobStatus.PROCESSING);
        Map<String, Object> result = new HashMap<>();
        result.put("candidateId", UUID.randomUUID().toString());

        when(jobQueueRepository.findById(testJobId)).thenReturn(Optional.of(job));
        when(jobQueueRepository.save(any(JobQueue.class))).thenReturn(job);

        // Act
        jobQueueService.markJobCompleted(testJobId, result);

        // Assert
        verify(jobQueueRepository).findById(testJobId);
        verify(jobQueueRepository).save(any(JobQueue.class));
    }

    @Test
    void testMarkJobFailed_WithRetry() {
        // Arrange
        JobQueue job = createMockJob(testJobId, JobStatus.PROCESSING);
        job.setRetryCount(1);
        job.setMaxRetries(3);

        when(jobQueueRepository.findById(testJobId)).thenReturn(Optional.of(job));
        when(jobQueueRepository.save(any(JobQueue.class))).thenReturn(job);

        // Act
        jobQueueService.markJobFailed(testJobId, "Test error", true);

        // Assert
        verify(jobQueueRepository).findById(testJobId);
        verify(jobQueueRepository).save(any(JobQueue.class));
    }

    @Test
    void testMarkJobFailed_NoRetry() {
        // Arrange
        JobQueue job = createMockJob(testJobId, JobStatus.PROCESSING);

        when(jobQueueRepository.findById(testJobId)).thenReturn(Optional.of(job));
        when(jobQueueRepository.save(any(JobQueue.class))).thenReturn(job);

        // Act
        jobQueueService.markJobFailed(testJobId, "Test error", false);

        // Assert
        verify(jobQueueRepository).findById(testJobId);
        verify(jobQueueRepository).save(any(JobQueue.class));
    }

    @Test
    void testCancelJob_Success() {
        // Arrange
        JobQueue job = createMockJob(testJobId, JobStatus.PENDING);
        when(jobQueueRepository.findByIdWithLock(testJobId)).thenReturn(Optional.of(job));
        when(jobQueueRepository.save(any(JobQueue.class))).thenReturn(job);

        // Act
        boolean result = jobQueueService.cancelJob(testJobId);

        // Assert
        assertTrue(result);
        verify(jobQueueRepository).findByIdWithLock(testJobId);
        verify(jobQueueRepository).save(any(JobQueue.class));
    }

    @Test
    void testCancelJob_NotFound() {
        // Arrange
        when(jobQueueRepository.findByIdWithLock(testJobId)).thenReturn(Optional.empty());

        // Act
        boolean result = jobQueueService.cancelJob(testJobId);

        // Assert
        assertFalse(result);
        verify(jobQueueRepository).findByIdWithLock(testJobId);
        verify(jobQueueRepository, never()).save(any(JobQueue.class));
    }

    @Test
    void testCancelJob_AlreadyCompleted() {
        // Arrange
        JobQueue job = createMockJob(testJobId, JobStatus.COMPLETED);
        when(jobQueueRepository.findByIdWithLock(testJobId)).thenReturn(Optional.of(job));

        // Act
        boolean result = jobQueueService.cancelJob(testJobId);

        // Assert
        assertFalse(result);
        verify(jobQueueRepository).findByIdWithLock(testJobId);
        verify(jobQueueRepository, never()).save(any(JobQueue.class));
    }

    @Test
    void testResetStaleJobs_WithRetryableJobs() {
        // Arrange
        JobQueue staleJob = createMockJob(testJobId, JobStatus.PROCESSING);
        staleJob.setRetryCount(1);
        staleJob.setMaxRetries(3);
        staleJob.setHeartbeatAt(LocalDateTime.now().minusMinutes(20));

        when(jobQueueRepository.findStaleJobs(any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(staleJob));
        when(jobQueueRepository.saveAll(anyList())).thenReturn(Collections.singletonList(staleJob));

        // Act
        int result = jobQueueService.resetStaleJobs();

        // Assert
        assertEquals(1, result);
        verify(jobQueueRepository).findStaleJobs(any(LocalDateTime.class));
        verify(jobQueueRepository).saveAll(anyList());
    }

    @Test
    void testResetStaleJobs_NoStaleJobs() {
        // Arrange
        when(jobQueueRepository.findStaleJobs(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // Act
        int result = jobQueueService.resetStaleJobs();

        // Assert
        assertEquals(0, result);
        verify(jobQueueRepository).findStaleJobs(any(LocalDateTime.class));
        verify(jobQueueRepository, never()).saveAll(anyList());
    }

    @Test
    void testGetJob_Found() {
        // Arrange
        JobQueue job = createMockJob(testJobId, JobStatus.PENDING);
        when(jobQueueRepository.findById(testJobId)).thenReturn(Optional.of(job));

        // Act
        Optional<JobQueue> result = jobQueueService.getJob(testJobId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testJobId, result.get().getId());
        verify(jobQueueRepository).findById(testJobId);
    }

    @Test
    void testGetJobsByCorrelationId_Success() {
        // Arrange
        JobQueue job1 = createMockJob(UUID.randomUUID(), JobStatus.PENDING);
        JobQueue job2 = createMockJob(UUID.randomUUID(), JobStatus.PROCESSING);
        List<JobQueue> jobs = Arrays.asList(job1, job2);

        when(jobQueueRepository.findByCorrelationId(testCorrelationId)).thenReturn(jobs);

        // Act
        List<JobQueue> result = jobQueueService.getJobsByCorrelationId(testCorrelationId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(jobQueueRepository).findByCorrelationId(testCorrelationId);
    }

    @Test
    void testGetJobsByStatus_Success() {
        // Arrange
        JobQueue job = createMockJob(testJobId, JobStatus.PENDING);
        Page<JobQueue> page = new PageImpl<>(Collections.singletonList(job));
        Pageable pageable = PageRequest.of(0, 20);

        when(jobQueueRepository.findByStatus(JobStatus.PENDING, pageable)).thenReturn(page);

        // Act
        Page<JobQueue> result = jobQueueService.getJobsByStatus(JobStatus.PENDING, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(jobQueueRepository).findByStatus(JobStatus.PENDING, pageable);
    }

    @Test
    void testGetQueueDepth_Success() {
        // Arrange
        when(jobQueueRepository.getQueueDepth(JobType.RESUME_PROCESSING)).thenReturn(10L);

        // Act
        long result = jobQueueService.getQueueDepth(JobType.RESUME_PROCESSING);

        // Assert
        assertEquals(10L, result);
        verify(jobQueueRepository).getQueueDepth(JobType.RESUME_PROCESSING);
    }

    @Test
    void testGetJobCount_Success() {
        // Arrange
        when(jobQueueRepository.countByStatus(JobStatus.COMPLETED)).thenReturn(25L);

        // Act
        long result = jobQueueService.getJobCount(JobStatus.COMPLETED);

        // Assert
        assertEquals(25L, result);
        verify(jobQueueRepository).countByStatus(JobStatus.COMPLETED);
    }

    @Test
    void testGetAverageProcessingDuration_Success() {
        // Arrange
        when(jobQueueRepository.getAverageProcessingDuration(JobType.RESUME_PROCESSING.name()))
                .thenReturn(45.5);

        // Act
        double result = jobQueueService.getAverageProcessingDuration(JobType.RESUME_PROCESSING);

        // Assert
        assertEquals(45.5, result, 0.01);
        verify(jobQueueRepository).getAverageProcessingDuration(JobType.RESUME_PROCESSING.name());
    }

    @Test
    void testGetJobStats_Success() {
        // Arrange
        List<Object[]> mockStats = Arrays.asList(
                new Object[]{JobStatus.PENDING, 5L},
                new Object[]{JobStatus.PROCESSING, 2L},
                new Object[]{JobStatus.COMPLETED, 10L}
        );

        when(jobQueueRepository.getJobStatsByType(JobType.RESUME_PROCESSING))
                .thenReturn(mockStats);

        // Act
        Map<JobStatus, Long> result = jobQueueService.getJobStats(JobType.RESUME_PROCESSING);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(5L, result.get(JobStatus.PENDING));
        assertEquals(2L, result.get(JobStatus.PROCESSING));
        assertEquals(10L, result.get(JobStatus.COMPLETED));
        verify(jobQueueRepository).getJobStatsByType(JobType.RESUME_PROCESSING);
    }

    @Test
    void testCleanupOldJobs_Success() {
        // Arrange
        when(jobQueueRepository.deleteCompletedJobsBefore(any(LocalDateTime.class)))
                .thenReturn(15);

        // Act
        int result = jobQueueService.cleanupOldJobs(30);

        // Assert
        assertEquals(15, result);
        verify(jobQueueRepository).deleteCompletedJobsBefore(any(LocalDateTime.class));
    }

    @Test
    void testGetJobsForRetry_Success() {
        // Arrange
        JobQueue failedJob = createMockJob(testJobId, JobStatus.FAILED);
        failedJob.setRetryCount(1);
        failedJob.setMaxRetries(3);

        when(jobQueueRepository.findJobsForRetry(
                eq(JobType.RESUME_PROCESSING),
                any(Pageable.class)
        )).thenReturn(Collections.singletonList(failedJob));

        // Act
        List<JobQueue> result = jobQueueService.getJobsForRetry(JobType.RESUME_PROCESSING, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(jobQueueRepository).findJobsForRetry(
                eq(JobType.RESUME_PROCESSING),
                any(Pageable.class)
        );
    }

    // Helper method to create mock JobQueue
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
