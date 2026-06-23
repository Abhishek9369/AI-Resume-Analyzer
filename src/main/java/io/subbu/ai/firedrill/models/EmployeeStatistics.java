package io.subbu.ai.firedrill.models;

import java.util.List;

/**
 * Statistics about employees in the system (returned by employeeStatistics GraphQL query)
 */
public record EmployeeStatistics(
        long total,
        long active,
        long onLeave,
        long suspended,
        long terminated,
        List<DepartmentCount> byDepartment,
        List<EmploymentTypeCount> byEmploymentType
) {}
