package io.subbu.ai.firedrill.models;

/**
 * Count of employees per department (used in EmployeeStatistics)
 */
public record DepartmentCount(
        String department,
        long count
) {}
