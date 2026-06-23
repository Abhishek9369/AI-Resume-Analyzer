package io.subbu.ai.firedrill.controller;

import io.subbu.ai.firedrill.entities.ProcessTracker;
import io.subbu.ai.firedrill.services.FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for file upload operations.
 * GraphQL does not support file uploads well, so we use REST for this endpoint.
 */
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    /**
     * Handle resume file upload (single file or ZIP).
     * Returns a tracking UUID for status monitoring.
     * 
     * @param file Uploaded file
     * @return Response with tracker UUID
     */
    @PostMapping("/resume")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    public ResponseEntity<Map<String, Object>> uploadResume(
            @RequestParam("files") java.util.List<MultipartFile> files) {
        
        log.info("Received upload request with {} files", files.size());

        try {
            UUID trackerId = fileUploadService.handleMultipleFileUpload(files);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("trackerId", trackerId.toString());
            response.put("message", "File upload initiated successfully");
            response.put("filename", files.size() + " files");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Upload error", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get processing status for a tracker ID.
     * 
     * @param trackerId Tracker UUID
     * @return Process tracker details
     */
    @GetMapping("/status/{trackerId}")
    public ResponseEntity<ProcessTracker> getUploadStatus(@PathVariable String trackerId) {
        try {
            UUID uuid = UUID.fromString(trackerId);
            ProcessTracker tracker = fileUploadService.getProcessStatus(uuid);
            return ResponseEntity.ok(tracker);
        } catch (IllegalArgumentException e) {
            log.error("Invalid tracker ID: {}", trackerId);
            return ResponseEntity.notFound().build();
        }
    }
}
