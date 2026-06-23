package io.subbu.ai.firedrill.services;

import io.subbu.ai.firedrill.entities.ProcessTracker;
import io.subbu.ai.firedrill.models.JobPriority;
import io.subbu.ai.firedrill.models.JobType;
import io.subbu.ai.firedrill.models.ProcessStatus;
import io.subbu.ai.firedrill.repos.ProcessTrackerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for handling file uploads.
 * Validates files, saves temporarily, and initiates processing.
 * 
 * Processing Mode:
 * - If app.scheduler.enabled=true: Creates jobs in queue for scheduler to process
 * - If app.scheduler.enabled=false: Uses async processing (legacy mode)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadService {

    private final FileParserService fileParserService;
    private final ResumeProcessingService resumeProcessingService;
    private final ProcessTrackerRepository trackerRepository;
    private final JobQueueService jobQueueService;

    @Value("${app.upload.directory:./uploads}")
    private String uploadDirectory;

    @Value("${app.upload.allowed-extensions:.doc,.docx,.pdf}")
    private String allowedExtensions;

    @Value("${app.scheduler.enabled:false}")
    private boolean schedulerEnabled;


    /**
     * Handle multiple file upload.
     * Creates a process tracker and initiates processing (async or scheduler-based).
     * 
     * @param files List of uploaded files
     * @return UUID of the process tracker
     * @throws IOException if file operations fail
     */
    public UUID handleMultipleFileUpload(List<MultipartFile> files) throws IOException {
        log.info("Handling multiple file upload, count: {}, schedulerMode={}", files.size(), schedulerEnabled);

        if (files.isEmpty()) {
            throw new IllegalArgumentException("No files provided");
        }

        // Validate all files first
        for (MultipartFile file : files) {
            validateFile(file);
        }

        // Create process tracker
        ProcessTracker tracker = ProcessTracker.builder()
                .status(ProcessStatus.INITIATED)
                .uploadedFilename(files.size() + " files")
                .totalFiles(files.size())
                .processedFiles(0)
                .failedFiles(0)
                .message("Received " + files.size() + " files, processing initiated")
                .build();

        tracker = trackerRepository.save(tracker);
        String correlationId = "batch-" + tracker.getId().toString();
        tracker.setCorrelationId(correlationId);
        trackerRepository.save(tracker);
        
        log.info("Created process tracker for batch: {}, correlationId={}", tracker.getId(), correlationId);

        if (schedulerEnabled) {
            // SCHEDULER MODE: Create jobs for each file
            log.info("Using scheduler mode for batch upload: {} files", files.size());
            
            for (MultipartFile file : files) {
                byte[] fileData = file.getBytes();
                String filename = file.getOriginalFilename();
                
                // Create job metadata
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("filename", filename);
                metadata.put("trackerId", tracker.getId().toString());
                metadata.put("uploadedAt", java.time.LocalDateTime.now().toString());
                metadata.put("fileSize", fileData.length);
                
                // Create job in queue
                jobQueueService.createJob(
                    JobType.RESUME_PROCESSING,
                    fileData,
                    metadata,
                    JobPriority.NORMAL,
                    correlationId
                );
                
                log.debug("Job created for file: filename={}, trackerId={}", filename, tracker.getId());
            }
            
            tracker.updateStatus(ProcessStatus.INITIATED, 
                String.format("Created %d jobs in queue for processing", files.size()));
            trackerRepository.save(tracker);
            
            log.info("Created {} jobs in queue for batch upload", files.size());
            
        } else {
            // ASYNC MODE: Process using legacy async service
            log.info("Using async mode for batch upload: {} files", files.size());
            
            // Read file data
            List<byte[]> fileDataList = new java.util.ArrayList<>();
            List<String> filenames = new java.util.ArrayList<>();

            for (MultipartFile file : files) {
                fileDataList.add(file.getBytes());
                filenames.add(file.getOriginalFilename());
            }

            // Process resumes asynchronously
            resumeProcessingService.processMultipleResumes(fileDataList, filenames, tracker.getId());
        }

        return tracker.getId();
    }

    /**
     * Handle single or ZIP file upload.
     * Creates a process tracker and initiates processing (async or scheduler-based).
     * 
     * @param file Uploaded file
     * @return UUID of the process tracker
     * @throws IOException if file operations fail
     */
    public UUID handleFileUpload(MultipartFile file) throws IOException {
        log.info("Handling file upload: {}, schedulerMode={}", file.getOriginalFilename(), schedulerEnabled);

        // Validate file
        validateFile(file);

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Create process tracker
        ProcessTracker tracker = ProcessTracker.builder()
                .status(ProcessStatus.INITIATED)
                .uploadedFilename(file.getOriginalFilename())
                .totalFiles(1)
                .processedFiles(0)
                .failedFiles(0)
                .message("Upload received, processing initiated")
                .build();

        tracker = trackerRepository.save(tracker);
        String correlationId = "upload-" + tracker.getId().toString();
        tracker.setCorrelationId(correlationId);
        trackerRepository.save(tracker);
        
        log.info("Created process tracker: {}, correlationId={}", tracker.getId(), correlationId);

        // Read file data
        byte[] fileData = file.getBytes();
        String filename = file.getOriginalFilename();

        if (schedulerEnabled) {
            // SCHEDULER MODE: Create job in queue
            log.info("Using scheduler mode for file upload: {}", filename);
            
            // Determine if ZIP or single file
            boolean isZip = filename != null && filename.toLowerCase().endsWith(".zip");
            
            if (isZip) {
                // TODO: Handle ZIP files in scheduler mode
                // For now, fall back to async processing for ZIP files
                log.warn("ZIP file processing not yet implemented in scheduler mode: {}, using async", filename);
                resumeProcessingService.processZipFile(fileData, filename, tracker.getId());
            } else {
                // Create job metadata
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("filename", filename);
                metadata.put("trackerId", tracker.getId().toString());
                metadata.put("uploadedAt", java.time.LocalDateTime.now().toString());
                metadata.put("fileSize", fileData.length);
                
                // Create job in queue
                jobQueueService.createJob(
                    JobType.RESUME_PROCESSING,
                    fileData,
                    metadata,
                    JobPriority.NORMAL,
                    correlationId
                );
                
                tracker.updateStatus(ProcessStatus.INITIATED, "Job created in queue for processing");
                trackerRepository.save(tracker);
                
                log.info("Job created in queue: filename={}, trackerId={}, jobId will be assigned by scheduler", 
                         filename, tracker.getId());
            }
            
        } else {
            // ASYNC MODE: Process using legacy async service
            log.info("Using async mode for file upload: {}", filename);
            
            // Determine if ZIP or single file
            if (filename != null && filename.toLowerCase().endsWith(".zip")) {
                // Process ZIP file asynchronously
                resumeProcessingService.processZipFile(fileData, filename, tracker.getId());
            } else {
                // Process single resume asynchronously
                resumeProcessingService.processSingleResume(fileData, filename, tracker.getId());
            }
        }

        return tracker.getId();
    }

    /**
     * Validate uploaded file.
     * Checks file size, extension, and content.
     * 
     * @param file Uploaded file
     * @throws IllegalArgumentException if validation fails
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Filename is required");
        }

        // Check if ZIP or allowed resume format
        boolean isZip = filename.toLowerCase().endsWith(".zip");
        boolean isValidFormat = fileParserService.isValidFileFormat(filename);

        if (!isZip && !isValidFormat) {
            throw new IllegalArgumentException(
                "Unsupported file format. Allowed formats: " + allowedExtensions + ", .zip"
            );
        }

        // Check file size (already handled by Spring Boot multipart config, 
        // but we can add custom validation here)
        long maxSize = 50 * 1024 * 1024; // 50 MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException(
                "File size exceeds maximum allowed size of 50 MB"
            );
        }

        log.debug("File validation passed: {}", filename);
    }

    /**
     * Get process tracker by ID.
     * 
     * @param trackerId Tracker UUID
     * @return Process tracker
     */
    public ProcessTracker getProcessStatus(UUID trackerId) {
        return trackerRepository.findById(trackerId)
                .orElseThrow(() -> new IllegalArgumentException("Tracker not found: " + trackerId));
    }
}
