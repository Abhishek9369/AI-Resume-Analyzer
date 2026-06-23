package io.subbu.ai.firedrill.resolver;

import io.subbu.ai.firedrill.entities.Feedback;
import io.subbu.ai.firedrill.models.*;
import io.subbu.ai.firedrill.services.FeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * GraphQL resolver for Feedback queries and mutations.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class FeedbackResolver {

    private final FeedbackService feedbackService;

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER', 'HIRING_MANAGER', 'HR')")
    public List<Feedback> feedbackForCandidate(@Argument UUID candidateId) {
        log.info("Fetching feedback for candidate: {}", candidateId);
        return feedbackService.getFeedbackForCandidate(candidateId);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER', 'HIRING_MANAGER')")
    public List<Feedback> feedbackForJob(@Argument UUID jobRequirementId) {
        log.info("Fetching feedback for job: {}", jobRequirementId);
        return feedbackService.getFeedbackForJob(jobRequirementId);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<Feedback> feedbackByUser(@Argument UUID userId) {
        log.info("Fetching feedback by user: {}", userId);
        return feedbackService.getFeedbackByUser(userId);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Double averageRatingForCandidate(@Argument UUID candidateId) {
        log.info("Fetching average rating for candidate: {}", candidateId);
        return feedbackService.getAverageRating(EntityType.CANDIDATE, candidateId);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Double averageRatingForJob(@Argument UUID jobRequirementId) {
        log.info("Fetching average rating for job: {}", jobRequirementId);
        return feedbackService.getAverageRating(EntityType.JOB_REQUIREMENT, jobRequirementId);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public FeedbackStatistics feedbackStatistics(@Argument UUID entityId, @Argument EntityType entityType) {
        log.info("Fetching feedback statistics for {} {}", entityType, entityId);
        Map<String, Object> stats = feedbackService.getFeedbackStats(entityType, entityId);

        long total = toLong(stats.get("total"));
        Double avgRating = (Double) stats.get("averageRating");

        @SuppressWarnings("unchecked")
        Map<FeedbackType, Long> byType = (Map<FeedbackType, Long>) stats.get("byType");
        List<FeedbackTypeCount> typeCounts = byType != null
                ? byType.entrySet().stream()
                        .map(e -> new FeedbackTypeCount(e.getKey(), e.getValue()))
                        .collect(Collectors.toList())
                : List.of();

        return new FeedbackStatistics(total, avgRating, typeCounts);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER', 'HIRING_MANAGER')")
    public Feedback submitFeedback(@Argument("input") Map<String, Object> input) {
        log.info("Submitting feedback for entity: {}", input.get("entityId"));
        EntityType entityType = EntityType.valueOf((String) input.get("entityType"));
        UUID entityId = UUID.fromString((String) input.get("entityId"));
        FeedbackType feedbackType = FeedbackType.valueOf((String) input.get("feedbackType"));
        String comments = (String) input.get("comments");
        Integer rating = input.get("rating") != null ? ((Number) input.get("rating")).intValue() : null;
        boolean isVisible = input.get("isVisible") == null || Boolean.TRUE.equals(input.get("isVisible"));

        return feedbackService.submitFeedback(entityType, entityId, feedbackType, comments, rating, isVisible, null);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Feedback updateFeedback(@Argument UUID id, @Argument("input") Map<String, Object> input) {
        log.info("Updating feedback: {}", id);
        FeedbackType feedbackType = input.get("feedbackType") != null
                ? FeedbackType.valueOf((String) input.get("feedbackType")) : null;
        String comments = (String) input.get("comments");
        Integer rating = input.get("rating") != null ? ((Number) input.get("rating")).intValue() : null;
        Boolean isVisible = (Boolean) input.get("isVisible");

        return feedbackService.updateFeedback(id, feedbackType, comments, rating, isVisible, null);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Boolean deleteFeedback(@Argument UUID id) {
        log.info("Deleting feedback: {}", id);
        feedbackService.deleteFeedback(id, null);
        return true;
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    public Feedback hideFeedback(@Argument UUID id) {
        log.info("Hiding feedback: {}", id);
        return feedbackService.updateFeedback(id, null, null, null, false, null);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    public Feedback showFeedback(@Argument UUID id) {
        log.info("Showing feedback: {}", id);
        return feedbackService.updateFeedback(id, null, null, null, true, null);
    }

    private long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Long l) return l;
        if (value instanceof Integer i) return i.longValue();
        return Long.parseLong(value.toString());
    }
}
