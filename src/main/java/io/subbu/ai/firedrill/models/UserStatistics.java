package io.subbu.ai.firedrill.models;

/**
 * Statistics about users in the system (returned by userStatistics GraphQL query)
 */
public record UserStatistics(
        long total,
        long active,
        long admins,
        long recruiters,
        long hr,
        long hiringManagers
) {}
