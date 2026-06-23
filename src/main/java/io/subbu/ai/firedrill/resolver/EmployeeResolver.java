package io.subbu.ai.firedrill.resolver;

import io.subbu.ai.firedrill.entities.Employee;
import io.subbu.ai.firedrill.models.*;
import io.subbu.ai.firedrill.services.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * GraphQL resolver for Employee queries and mutations.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class EmployeeResolver {

    private final EmployeeService employeeService;

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public Employee employee(@Argument UUID id) {
        log.info("Fetching employee: {}", id);
        return employeeService.getEmployeeById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + id));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public Employee employeeByEmployeeId(@Argument String employeeId) {
        log.info("Fetching employee by employeeId: {}", employeeId);
        return employeeService.getEmployeeByEmployeeId(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public List<Employee> allEmployees(@Argument Integer page, @Argument Integer size) {
        log.info("Fetching all employees, page={}, size={}", page, size);
        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        return employeeService.getAllEmployees(PageRequest.of(pageNum, pageSize)).getContent();
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public List<Employee> employeesByDepartment(@Argument String department) {
        log.info("Fetching employees by department: {}", department);
        return employeeService.getEmployeesByDepartment(department);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public List<Employee> employeesByStatus(@Argument EmployeeStatus status) {
        log.info("Fetching employees by status: {}", status);
        return employeeService.getEmployeesByStatus(status);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public List<Employee> managerSubordinates(@Argument UUID managerId) {
        log.info("Fetching subordinates for manager: {}", managerId);
        return employeeService.getManagerSubordinates(managerId);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public EmployeeStatistics employeeStatistics() {
        log.info("Fetching employee statistics");
        Map<String, Object> stats = employeeService.getEmployeeStatistics();

        @SuppressWarnings("unchecked")
        Map<String, Long> byDept = (Map<String, Long>) stats.get("byDepartment");
        List<DepartmentCount> departmentCounts = byDept != null
                ? byDept.entrySet().stream()
                        .map(e -> new DepartmentCount(e.getKey(), e.getValue()))
                        .collect(Collectors.toList())
                : List.of();

        @SuppressWarnings("unchecked")
        Map<EmploymentType, Long> byType = (Map<EmploymentType, Long>) stats.get("byEmploymentType");
        List<EmploymentTypeCount> employmentTypeCounts = byType != null
                ? byType.entrySet().stream()
                        .map(e -> new EmploymentTypeCount(e.getKey(), e.getValue()))
                        .collect(Collectors.toList())
                : List.of();

        return new EmployeeStatistics(
                toLong(stats.get("total")),
                toLong(stats.get("active")),
                toLong(stats.get("onLeave")),
                toLong(stats.get("suspended")),
                toLong(stats.get("terminated")),
                departmentCounts,
                employmentTypeCounts
        );
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public List<String> allDepartments() {
        log.info("Fetching all departments");
        return employeeService.getAllDepartments();
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public Employee createEmployee(@Argument("input") Map<String, Object> input) {
        log.info("Creating employee: {}", input.get("employeeId"));
        String employeeId = (String) input.get("employeeId");
        UUID candidateId = UUID.fromString((String) input.get("candidateId"));
        BigDecimal salary = new BigDecimal(input.get("salary").toString());
        String department = (String) input.get("department");
        String position = (String) input.get("position");
        UUID managerId = input.get("managerId") != null ? UUID.fromString((String) input.get("managerId")) : null;
        EmploymentType employmentType = EmploymentType.valueOf((String) input.getOrDefault("employmentType", "FULL_TIME"));
        LocalDate hireDate = input.get("hireDate") != null
                ? LocalDate.parse(input.get("hireDate").toString().substring(0, 10))
                : LocalDate.now();

        return employeeService.createEmployee(employeeId, candidateId, salary, department,
                position, managerId, employmentType, hireDate, null);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public Employee updateEmployee(@Argument UUID id, @Argument("input") Map<String, Object> input) {
        log.info("Updating employee: {}", id);
        BigDecimal salary = input.get("salary") != null ? new BigDecimal(input.get("salary").toString()) : null;
        String department = (String) input.get("department");
        String position = (String) input.get("position");
        UUID managerId = input.get("managerId") != null ? UUID.fromString((String) input.get("managerId")) : null;
        EmploymentType employmentType = input.get("employmentType") != null
                ? EmploymentType.valueOf((String) input.get("employmentType")) : null;
        EmployeeStatus status = input.get("status") != null
                ? EmployeeStatus.valueOf((String) input.get("status")) : null;

        return employeeService.updateEmployee(id, salary, department, position, managerId, employmentType, status, null);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public Employee terminateEmployee(@Argument UUID id, @Argument String terminationDate) {
        log.info("Terminating employee: {}", id);
        LocalDate date = terminationDate != null ? LocalDate.parse(terminationDate.substring(0, 10)) : null;
        employeeService.terminateEmployee(id, date, null);
        return employeeService.getEmployeeById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + id));
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Boolean deleteEmployee(@Argument UUID id) {
        log.info("Deleting employee: {}", id);
        employeeService.deleteEmployee(id, null);
        return true;
    }

    private long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Long l) return l;
        if (value instanceof Integer i) return i.longValue();
        return Long.parseLong(value.toString());
    }
}
