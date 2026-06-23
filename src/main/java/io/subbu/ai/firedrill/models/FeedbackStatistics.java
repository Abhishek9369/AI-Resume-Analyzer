package io.subbu.ai.firedrill.models;

import java.util.List;

/**
 * Feedback statistics for an entity (used by feedbackStatistics GraphQL query)
 */
public record FeedbackStatistics(
        long total,
        Double averageRating,
        List<FeedbackTypeCount> byType
) {}
