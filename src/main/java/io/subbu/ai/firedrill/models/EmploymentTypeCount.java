package io.subbu.ai.firedrill.models;

/**
 * Count of employees per employment type (used in EmployeeStatistics)
 */
public record EmploymentTypeCount(
        EmploymentType employmentType,
        long count
) {}
