package io.subbu.ai.firedrill.models;

/**
 * Count of feedback per type (used in FeedbackStatistics)
 */
public record FeedbackTypeCount(
        FeedbackType feedbackType,
        long count
) {}
