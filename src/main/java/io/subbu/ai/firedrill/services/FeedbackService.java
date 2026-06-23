package io.subbu.ai.firedrill.services;

import io.subbu.ai.firedrill.config.SecurityUtils;
import io.subbu.ai.firedrill.entities.AuditLog;
import io.subbu.ai.firedrill.entities.Feedback;
import io.subbu.ai.firedrill.entities.User;
import io.subbu.ai.firedrill.models.EntityType;
import io.subbu.ai.firedrill.models.FeedbackType;
import io.subbu.ai.firedrill.models.UserRole;
import io.subbu.ai.firedrill.repositories.AuditLogRepository;
import io.subbu.ai.firedrill.repositories.FeedbackRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for feedback management operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuthenticationService authenticationService;

    /**
     * Get feedback for a candidate
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER', 'HIRING_MANAGER', 'HR')")
    public List<Feedback> getFeedbackForCandidate(UUID candidateId) {
        log.debug("Getting feedback for candidate: {}", candidateId);
        return feedbackRepository.findByEntityTypeAndEntityId(EntityType.CANDIDATE, candidateId);
    }

    /**
     * Get feedback for a job requirement
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER', 'HIRING_MANAGER')")
    public List<Feedback> getFeedbackForJob(UUID jobId) {
        log.debug("Getting feedback for job: {}", jobId);
        return feedbackRepository.findByEntityTypeAndEntityId(EntityType.JOB_REQUIREMENT, jobId);
    }

    /**
     * Get feedback provided by a specific user
     */
    public List<Feedback> getFeedbackByUser(UUID userId) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));
        
        // Users can view their own feedback, admins can view anyone's
        if (!currentUser.isAdmin() && !currentUser.getId().equals(userId)) {
            throw new RuntimeException("Access denied: Cannot view other users' feedback");
        }
        
        return feedbackRepository.findByProvidedById(userId);
    }

    /**
     * Submit new feedback
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER', 'HIRING_MANAGER')")
    @Transactional
    public Feedback submitFeedback(EntityType entityType, UUID entityId, FeedbackType feedbackType,
                                   String comments, Integer rating, boolean isVisible,
                                   HttpServletRequest request) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));
        
        // Validate rating if provided
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new RuntimeException("Rating must be between 1 and 5");
        }
        
        // Hiring managers can only provide feedback, not reject/shortlist candidates
        if (currentUser.hasRole(UserRole.HIRING_MANAGER) && 
            (feedbackType == FeedbackType.SHORTLIST || feedbackType == FeedbackType.REJECT)) {
            throw new RuntimeException("Hiring managers cannot shortlist or reject candidates directly");
        }
        
        // Create feedback
        Feedback feedback = new Feedback();
        feedback.setEntityType(entityType);
        feedback.setEntityId(entityId);
        feedback.setFeedbackType(feedbackType);
        feedback.setComments(comments);
        feedback.setRating(rating);
        feedback.setProvidedBy(currentUser);
        feedback.setIsVisible(isVisible);
        feedback.setCreatedAt(LocalDateTime.now());
        
        Feedback savedFeedback = feedbackRepository.save(feedback);
        
        // Audit log
        createAuditLog(currentUser, "FEEDBACK_SUBMITTED",
                "Submitted " + feedbackType + " feedback for " + entityType + ": " + entityId,
                true, request);
        
        log.info("Feedback submitted: {} for {} {} by {}", 
                feedbackType, entityType, entityId, currentUser.getUsername());
        
        return savedFeedback;
    }

    /**
     * Update existing feedback (only by owner or admin)
     */
    @Transactional
    public Feedback updateFeedback(UUID feedbackId, FeedbackType feedbackType, String comments,
                                   Integer rating, Boolean isVisible, HttpServletRequest request) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));
        
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Feedback not found"));
        
        // Only owner or admin can update
        if (!currentUser.isAdmin() && !feedback.getProvidedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied: Can only update your own feedback");
        }
        
        // Update fields
        if (feedbackType != null) {
            feedback.setFeedbackType(feedbackType);
        }
        
        if (comments != null) {
            feedback.setComments(comments);
        }
        
        if (rating != null) {
            if (rating < 1 || rating > 5) {
                throw new RuntimeException("Rating must be between 1 and 5");
            }
            feedback.setRating(rating);
        }
        
        if (isVisible != null) {
            feedback.setIsVisible(isVisible);
        }
        
        Feedback updatedFeedback = feedbackRepository.save(feedback);
        
        // Audit log
        createAuditLog(currentUser, "FEEDBACK_UPDATED",
                "Updated feedback: " + feedbackId, true, request);
        
        log.info("Feedback updated: {} by {}", feedbackId, currentUser.getUsername());
        return updatedFeedback;
    }

    /**
     * Delete feedback (only by owner or admin)
     */
    @Transactional
    public void deleteFeedback(UUID feedbackId, HttpServletRequest request) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));
        
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Feedback not found"));
        
        // Only owner or admin can delete
        if (!currentUser.isAdmin() && !feedback.getProvidedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied: Can only delete your own feedback");
        }
        
        feedbackRepository.delete(feedback);
        
        // Audit log
        createAuditLog(currentUser, "FEEDBACK_DELETED",
                "Deleted feedback: " + feedbackId, true, request);
        
        log.info("Feedback deleted: {} by {}", feedbackId, currentUser.getUsername());
    }

    /**
     * Get average rating for an entity
     */
    public Double getAverageRating(EntityType entityType, UUID entityId) {
        return feedbackRepository.getAverageRating(entityType, entityId);
    }

    /**
     * Get feedback statistics for an entity
     */
    public Map<String, Object> getFeedbackStats(EntityType entityType, UUID entityId) {
        List<Feedback> feedbackList = feedbackRepository.findByEntityTypeAndEntityId(entityType, entityId);
        
        long total = feedbackList.size();
        long withRating = feedbackList.stream().filter(Feedback::hasRating).count();
        Double avgRating = feedbackRepository.getAverageRating(entityType, entityId);
        
        // Count by feedback type
        Map<FeedbackType, Long> typeCounts = new HashMap<>();
        for (FeedbackType type : FeedbackType.values()) {
            long count = feedbackRepository.countByEntityTypeAndEntityIdAndFeedbackType(
                    entityType, entityId, type);
            if (count > 0) {
                typeCounts.put(type, count);
            }
        }
        
        return Map.of(
            "total", total,
            "withRating", withRating,
            "averageRating", avgRating != null ? avgRating : 0.0,
            "byType", typeCounts
        );
    }

    /**
     * Get visible feedback only (for candidates viewing their own feedback)
     */
    public List<Feedback> getVisibleFeedback(EntityType entityType, UUID entityId) {
        return feedbackRepository.findByEntityTypeAndEntityIdAndIsVisibleTrue(entityType, entityId);
    }

    /**
     * Hide feedback (admin or owner only)
     */
    @Transactional
    public void hideFeedback(UUID feedbackId, HttpServletRequest request) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));
        
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Feedback not found"));
        
        if (!currentUser.isAdmin() && !feedback.getProvidedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }
        
        feedback.setIsVisible(false);
        feedbackRepository.save(feedback);
        
        createAuditLog(currentUser, "FEEDBACK_HIDDEN",
                "Hidden feedback: " + feedbackId, true, request);
    }

    /**
     * Show feedback (admin or owner only)
     */
    @Transactional
    public void showFeedback(UUID feedbackId, HttpServletRequest request) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));
        
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Feedback not found"));
        
        if (!currentUser.isAdmin() && !feedback.getProvidedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }
        
        feedback.setIsVisible(true);
        feedbackRepository.save(feedback);
        
        createAuditLog(currentUser, "FEEDBACK_SHOWN",
                "Made feedback visible: " + feedbackId, true, request);
    }

    /**
     * Helper method to create audit log
     */
    private void createAuditLog(User user, String action, String details,
                                boolean success, HttpServletRequest request) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUser(user);
        auditLog.setUsername(user.getUsername());
        auditLog.setAction(action);
        auditLog.setDetails(details);
        auditLog.setSuccess(success);
        if (request != null) {
            auditLog.setIpAddress(authenticationService.getClientIpAddress(request));
            auditLog.setUserAgent(request.getHeader("User-Agent"));
        }
        auditLog.setCreatedAt(LocalDateTime.now());
        
        auditLogRepository.save(auditLog);
    }
}
