package io.subbu.ai.firedrill.services;

import io.subbu.ai.firedrill.entities.Candidate;
import io.subbu.ai.firedrill.entities.JobQueue;
import io.subbu.ai.firedrill.entities.ProcessTracker;
import io.subbu.ai.firedrill.models.ProcessStatus;
import io.subbu.ai.firedrill.models.ResumeAnalysisRequest;
import io.subbu.ai.firedrill.models.ResumeAnalysisResponse;
import io.subbu.ai.firedrill.repos.CandidateRepository;
import io.subbu.ai.firedrill.repos.ProcessTrackerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Processor for resume processing jobs.
 * Handles the actual resume parsing, AI analysis, embedding generation, and storage.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeJobProcessor {

    private final FileParserService fileParserService;
    private final AIService aiService;
    private final EmbeddingService embeddingService;
    private final CandidateRepository candidateRepository;
    private final ProcessTrackerRepository trackerRepository;
    private final JobQueueService jobQueueService;

    /**
     * Process a resume job from the queue.
     * 
     * @param job The job to process
     */
    @Transactional
    public void processJob(JobQueue job) {
        UUID jobId = job.getId();
        log.info("Starting resume job processing: jobId={}, priority={}, retryCount={}", 
                 jobId, job.getPriority(), job.getRetryCount());

        try {
            // Extract metadata
            Map<String, Object> metadata = job.getMetadata();
            if (metadata == null) {
                throw new IllegalStateException("Job metadata is null");
            }

            String filename = (String) metadata.get("filename");
            String trackerIdStr = (String) metadata.get("trackerId");
            
            if (filename == null || trackerIdStr == null) {
                throw new IllegalStateException("Missing required metadata: filename or trackerId");
            }

            UUID trackerId = UUID.fromString(trackerIdStr);
            byte[] fileData = job.getFileData();

            log.info("Processing resume job: jobId={}, filename={}, trackerId={}, dataSize={} bytes", 
                     jobId, filename, trackerId, fileData != null ? fileData.length : 0);

            // Get process tracker
            ProcessTracker tracker = trackerRepository.findById(trackerId)
                    .orElseThrow(() -> new IllegalArgumentException("Tracker not found: " + trackerId));

            // Link job to tracker
            tracker.setJobId(jobId);
            tracker.setCorrelationId(job.getCorrelationId());
            trackerRepository.save(tracker);

            // Send heartbeat before starting
            jobQueueService.updateHeartbeat(jobId);

            // Step 1: Extract text from file
            log.debug("Extracting text from resume: jobId={}, filename={}", jobId, filename);
            String resumeContent = fileParserService.extractText(fileData, filename);
            log.info("Text extraction complete: jobId={}, contentLength={} chars", 
                     jobId, resumeContent.length());

            // Send heartbeat after parsing
            jobQueueService.updateHeartbeat(jobId);

            // Step 2: Analyze resume using AI
            log.debug("Analyzing resume with AI: jobId={}, filename={}", jobId, filename);
            ResumeAnalysisRequest analysisRequest = ResumeAnalysisRequest.builder()
                    .resumeContent(resumeContent)
                    .filename(filename)
                    .build();

            ResumeAnalysisResponse analysisResponse = aiService.analyzeResume(analysisRequest);
            log.info("AI analysis complete: jobId={}, candidateName={}, skillsPresent={}", 
                     jobId, analysisResponse.getName(), 
                     analysisResponse.getSkills() != null && !analysisResponse.getSkills().isEmpty());

            tracker.updateStatus(ProcessStatus.RESUME_ANALYZED, "Resume analyzed");
            trackerRepository.save(tracker);

            // Send heartbeat after AI analysis
            jobQueueService.updateHeartbeat(jobId);

            // Step 3: Create candidate entity
            log.debug("Creating candidate entity: jobId={}, name={}", jobId, analysisResponse.getName());
            Candidate candidate = Candidate.builder()
                    .name(analysisResponse.getName())
                    .email(analysisResponse.getEmail())
                    .mobile(analysisResponse.getMobile())
                    .resumeFilename(filename)
                    .resumeContent(resumeContent)
                    .resumeFile(fileData)
                    .experienceSummary(analysisResponse.getExperienceSummary())
                    .skills(analysisResponse.getSkills())
                    .domainKnowledge(analysisResponse.getDomainKnowledge())
                    .academicBackground(analysisResponse.getAcademicBackground())
                    .yearsOfExperience(analysisResponse.getYearsOfExperience())
                    .build();

            candidate = candidateRepository.save(candidate);
            log.info("Candidate saved: jobId={}, candidateId={}, name={}", 
                     jobId, candidate.getId(), candidate.getName());

            // Send heartbeat after saving candidate
            jobQueueService.updateHeartbeat(jobId);

            // Step 4: Generate embeddings
            log.debug("Generating embeddings: jobId={}, candidateId={}", jobId, candidate.getId());
            embeddingService.generateAndStoreEmbeddings(candidate, resumeContent);
            log.info("Embeddings generated and stored: jobId={}, candidateId={}", 
                     jobId, candidate.getId());

            tracker.updateStatus(ProcessStatus.EMBED_GENERATED, "Embeddings generated");
            trackerRepository.save(tracker);

            tracker.updateStatus(ProcessStatus.VECTOR_DB_UPDATED, "Vector database updated");
            trackerRepository.save(tracker);

            // Prepare result metadata
            Map<String, Object> result = new HashMap<>();
            result.put("candidateId", candidate.getId().toString());
            result.put("candidateName", candidate.getName());
            result.put("filename", filename);
            result.put("trackerId", trackerId.toString());
            result.put("skillsPresent", analysisResponse.getSkills() != null && !analysisResponse.getSkills().isEmpty());
            result.put("yearsOfExperience", analysisResponse.getYearsOfExperience());

            // Mark job as completed
            jobQueueService.markJobCompleted(jobId, result);
            tracker.updateStatus(ProcessStatus.COMPLETED, "Resume processing completed successfully");
            trackerRepository.save(tracker);

            log.info("Resume job processing completed successfully: jobId={}, candidateId={}, duration={}s", 
                     jobId, candidate.getId(), 
                     job.getStartedAt() != null ? 
                         java.time.Duration.between(job.getStartedAt(), java.time.LocalDateTime.now()).getSeconds() : 0);

        } catch (Exception e) {
            log.error("Error processing resume job: jobId={}, error={}", jobId, e.getMessage(), e);
            handleJobFailure(job, e);
        }
    }

    /**
     * Handle job failure by updating status and determining if retry is needed.
     * 
     * @param job The failed job
     * @param error The exception that caused the failure
     */
    private void handleJobFailure(JobQueue job, Exception error) {
        UUID jobId = job.getId();
        String errorMessage = error.getMessage();
        
        log.warn("Handling job failure: jobId={}, retryCount={}/{}, error={}", 
                 jobId, job.getRetryCount(), job.getMaxRetries(), errorMessage);

        // Determine if the error is retryable
        boolean shouldRetry = isRetryableError(error);
        
        if (!shouldRetry) {
            log.error("Non-retryable error encountered: jobId={}, errorType={}", 
                     jobId, error.getClass().getSimpleName());
        }

        // Mark job as failed (service will handle retry logic)
        jobQueueService.markJobFailed(jobId, errorMessage, shouldRetry);

        // Update process tracker
        try {
            Map<String, Object> metadata = job.getMetadata();
            if (metadata != null && metadata.containsKey("trackerId")) {
                String trackerIdStr = (String) metadata.get("trackerId");
                UUID trackerId = UUID.fromString(trackerIdStr);
                
                ProcessTracker tracker = trackerRepository.findById(trackerId).orElse(null);
                if (tracker != null) {
                    String statusMessage = shouldRetry && job.canRetry() 
                            ? String.format("Processing failed, will retry (attempt %d/%d): %s", 
                                          job.getRetryCount() + 1, job.getMaxRetries(), errorMessage)
                            : String.format("Processing failed permanently: %s", errorMessage);
                    
                    tracker.updateStatus(ProcessStatus.FAILED, statusMessage);
                    tracker.incrementFailedFiles();
                    trackerRepository.save(tracker);
                    
                    log.debug("Updated process tracker: trackerId={}, status=FAILED", trackerId);
                }
            }
        } catch (Exception e) {
            log.error("Error updating process tracker after job failure: jobId={}", jobId, e);
        }
    }

    /**
     * Determine if an error is retryable.
     * Transient errors (network issues, timeouts) should be retried.
     * Permanent errors (invalid data, parsing errors) should not be retried.
     * 
     * @param error The exception to check
     * @return true if the error is retryable
     */
    private boolean isRetryableError(Exception error) {
        // Check exception type
        String errorClass = error.getClass().getSimpleName();
        String errorMessage = error.getMessage() != null ? error.getMessage().toLowerCase() : "";

        // Non-retryable errors
        if (errorClass.contains("IllegalArgument") || errorClass.contains("IllegalState")) {
            return false;
        }
        
        if (errorMessage.contains("invalid") || errorMessage.contains("malformed") || 
            errorMessage.contains("unsupported")) {
            return false;
        }

        // Retryable errors (network, timeout, temporary issues)
        if (errorClass.contains("Timeout") || errorClass.contains("Connection") || 
            errorClass.contains("Socket") || errorClass.contains("IO")) {
            return true;
        }
        
        if (errorMessage.contains("timeout") || errorMessage.contains("connection") || 
            errorMessage.contains("network") || errorMessage.contains("unavailable")) {
            return true;
        }

        // Default: retry for unknown errors
        log.debug("Unknown error type, defaulting to retryable: {}", errorClass);
        return true;
    }
}
