package io.subbu.ai.firedrill.services;

import io.subbu.ai.firedrill.entities.Candidate;
import io.subbu.ai.firedrill.entities.ProcessTracker;
import io.subbu.ai.firedrill.models.*;
import io.subbu.ai.firedrill.repos.CandidateRepository;
import io.subbu.ai.firedrill.repos.ProcessTrackerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Service for processing resume uploads asynchronously.
 * Orchestrates file parsing, AI analysis, embedding generation, and database storage.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeProcessingService {

    private final FileParserService fileParserService;
    private final AIService aiService;
    private final EmbeddingService embeddingService;
    private final CandidateRepository candidateRepository;
    private final ProcessTrackerRepository trackerRepository;

    /**
     * Process a single resume file asynchronously.
     * 
     * @param fileData Binary file content
     * @param filename Original filename
     * @param trackerId Process tracker ID
     */
    @Async
    @Transactional
    public void processSingleResume(byte[] fileData, String filename, UUID trackerId) {
        log.info("Processing single resume: {} (Tracker: {})", filename, trackerId);

        ProcessTracker tracker = trackerRepository.findById(trackerId)
                .orElseThrow(() -> new IllegalArgumentException("Tracker not found: " + trackerId));

        try {
            processResumeFile(fileData, filename, tracker);
            tracker.updateStatus(ProcessStatus.COMPLETED, "Resume processed successfully");
        } catch (Exception e) {
            log.error("Error processing resume: {}", filename, e);
            tracker.incrementFailedFiles();
            tracker.updateStatus(ProcessStatus.FAILED, "Error: " + e.getMessage());
        } finally {
            trackerRepository.save(tracker);
        }
    }

    /**
     * Process multiple resumes asynchronously.
     * 
     * @param fileDataList List of binary file content
     * @param filenames List of filenames
     * @param trackerId Process tracker ID
     */
    @Async
    @Transactional
    public void processMultipleResumes(List<byte[]> fileDataList, List<String> filenames, UUID trackerId) {
        log.info("Processing {} resumes (Tracker: {})", filenames.size(), trackerId);

        ProcessTracker tracker = trackerRepository.findById(trackerId)
                .orElseThrow(() -> new IllegalArgumentException("Tracker not found: " + trackerId));

        int totalFiles = filenames.size();
        int processedFiles = 0;
        int failedFiles = 0;

        try {
            for (int i = 0; i < totalFiles; i++) {
                String filename = filenames.get(i);
                byte[] data = fileDataList.get(i);

                try {
                    processResumeFile(data, filename, tracker);
                    processedFiles++;
                    tracker.incrementProcessedFiles();
                } catch (Exception e) {
                    log.error("Error processing file: {}", filename, e);
                    failedFiles++;
                    tracker.incrementFailedFiles();
                }

                tracker.updateStatus(
                    ProcessStatus.INITIATED,
                    String.format("Processed %d/%d files", processedFiles + failedFiles, totalFiles)
                );
                trackerRepository.save(tracker);
            }

            String message = String.format(
                "Completed: %d successful, %d failed out of %d total",
                processedFiles, failedFiles, totalFiles
            );
            tracker.updateStatus(ProcessStatus.COMPLETED, message);

        } catch (Exception e) {
            log.error("Error processing batch upload", e);
            tracker.updateStatus(ProcessStatus.FAILED, "Batch processing error: " + e.getMessage());
        } finally {
            trackerRepository.save(tracker);
        }
    }

    /**
     * Process multiple resumes from a ZIP file asynchronously.
     * 
     * @param zipData Binary ZIP file content
     * @param filename Original ZIP filename
     * @param trackerId Process tracker ID
     */
    @Async
    @Transactional
    public void processZipFile(byte[] zipData, String filename, UUID trackerId) {
        log.info("Processing ZIP file: {} (Tracker: {})", filename, trackerId);

        ProcessTracker tracker = trackerRepository.findById(trackerId)
                .orElseThrow(() -> new IllegalArgumentException("Tracker not found: " + trackerId));

        tracker.updateStatus(ProcessStatus.INITIATED, "Extracting files from ZIP");
        trackerRepository.save(tracker);

        int totalFiles = 0;
        int processedFiles = 0;
        int failedFiles = 0;

        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipData))) {
            ZipEntry entry;

            // First pass: count files
            try (ZipInputStream countStream = new ZipInputStream(new ByteArrayInputStream(zipData))) {
                while ((entry = countStream.getNextEntry()) != null) {
                    if (!entry.isDirectory() && fileParserService.isValidFileFormat(entry.getName())) {
                        totalFiles++;
                    }
                }
            }

            tracker.setTotalFiles(totalFiles);
            trackerRepository.save(tracker);

            // Second pass: process files
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;

                String entryName = entry.getName();
                if (!fileParserService.isValidFileFormat(entryName)) {
                    log.warn("Skipping unsupported file: {}", entryName);
                    continue;
                }

                try {
                    byte[] entryData = zipInputStream.readAllBytes();
                    processResumeFile(entryData, entryName, tracker);
                    processedFiles++;
                    tracker.incrementProcessedFiles();
                } catch (Exception e) {
                    log.error("Error processing file from ZIP: {}", entryName, e);
                    failedFiles++;
                    tracker.incrementFailedFiles();
                }

                tracker.updateStatus(
                    ProcessStatus.INITIATED,
                    String.format("Processed %d/%d files", processedFiles + failedFiles, totalFiles)
                );
                trackerRepository.save(tracker);
            }

            String message = String.format(
                "Completed: %d successful, %d failed out of %d total",
                processedFiles, failedFiles, totalFiles
            );
            tracker.updateStatus(ProcessStatus.COMPLETED, message);

        } catch (IOException e) {
            log.error("Error processing ZIP file: {}", filename, e);
            tracker.updateStatus(ProcessStatus.FAILED, "ZIP extraction error: " + e.getMessage());
        } finally {
            trackerRepository.save(tracker);
        }
    }

    /**
     * Core method to process a single resume file.
     * Executes all processing steps: parse, analyze, embed, and store.
     * 
     * @param fileData Binary file content
     * @param filename Original filename
     * @param tracker Process tracker
     * @throws Exception if processing fails
     */
    private void processResumeFile(byte[] fileData, String filename, ProcessTracker tracker) throws Exception {
        log.debug("Processing resume file: {}", filename);

        // Step 1: Extract text from file
        String resumeContent = fileParserService.extractText(fileData, filename);
        log.debug("Extracted {} characters from {}", resumeContent.length(), filename);

        // Step 2: Analyze resume using AI
        ResumeAnalysisRequest analysisRequest = ResumeAnalysisRequest.builder()
                .resumeContent(resumeContent)
                .filename(filename)
                .build();

        ResumeAnalysisResponse analysisResponse = aiService.analyzeResume(analysisRequest);
        log.debug("Resume analyzed for: {}", analysisResponse.getName());

        tracker.updateStatus(ProcessStatus.RESUME_ANALYZED, "Resume analyzed");
        trackerRepository.save(tracker);

        // Step 3: Create candidate entity
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
        log.info("Candidate saved: {} (ID: {})", candidate.getName(), candidate.getId());

        // Step 4: Generate embeddings
        embeddingService.generateAndStoreEmbeddings(candidate, resumeContent);
        tracker.updateStatus(ProcessStatus.EMBED_GENERATED, "Embeddings generated");
        trackerRepository.save(tracker);

        tracker.updateStatus(ProcessStatus.VECTOR_DB_UPDATED, "Vector database updated");
        trackerRepository.save(tracker);

        log.info("Resume processing completed for: {}", filename);
    }
}
