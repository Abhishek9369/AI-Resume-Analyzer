package io.subbu.ai.firedrill.services;

import io.subbu.ai.firedrill.config.SecurityUtils;
import io.subbu.ai.firedrill.entities.AuditLog;
import io.subbu.ai.firedrill.entities.Candidate;
import io.subbu.ai.firedrill.entities.Employee;
import io.subbu.ai.firedrill.entities.User;
import io.subbu.ai.firedrill.models.EmployeeStatus;
import io.subbu.ai.firedrill.models.EmploymentType;
import io.subbu.ai.firedrill.repositories.AuditLogRepository;
import io.subbu.ai.firedrill.repos.CandidateRepository;
import io.subbu.ai.firedrill.repositories.EmployeeRepository;
import io.subbu.ai.firedrill.repositories.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for employee management operations (HR module)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final CandidateRepository candidateRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuthenticationService authenticationService;

    /**
     * Get all employees (HR and Admin only)
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public Page<Employee> getAllEmployees(Pageable pageable) {
        log.debug("Getting all employees with pagination: {}", pageable);
        return employeeRepository.findAll(pageable);
    }

    /**
     * Get employee by ID
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public Optional<Employee> getEmployeeById(UUID id) {
        return employeeRepository.findById(id);
    }

    /**
     * Get employee by employee ID
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public Optional<Employee> getEmployeeByEmployeeId(String employeeId) {
        return employeeRepository.findByEmployeeId(employeeId);
    }

    /**
     * Get employees by department
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public List<Employee> getEmployeesByDepartment(String department) {
        log.debug("Getting employees by department: {}", department);
        return employeeRepository.findByDepartment(department);
    }

    /**
     * Get employees by status
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public List<Employee> getEmployeesByStatus(EmployeeStatus status) {
        log.debug("Getting employees by status: {}", status);
        return employeeRepository.findByStatus(status);
    }

    /**
     * Get active employees only
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public List<Employee> getActiveEmployees() {
        return employeeRepository.findByStatus(EmployeeStatus.ACTIVE);
    }

    /**
     * Create new employee
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Transactional
    public Employee createEmployee(String employeeId, UUID candidateId, BigDecimal salary,
                                   String department, String position, UUID managerId,
                                   EmploymentType employmentType, LocalDate hireDate,
                                   HttpServletRequest request) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));
        
        // Validate unique employee ID
        if (employeeRepository.findByEmployeeId(employeeId).isPresent()) {
            throw new RuntimeException("Employee ID already exists");
        }
        
        // Get candidate
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new RuntimeException("Candidate not found"));
        
        // Get manager if specified
        Employee manager = null;
        if (managerId != null) {
            manager = employeeRepository.findById(managerId)
                    .orElseThrow(() -> new RuntimeException("Manager not found"));
        }
        
        // Create employee
        Employee employee = new Employee();
        employee.setEmployeeId(employeeId);
        employee.setCandidate(candidate);
        employee.setSalary(salary);
        employee.setDepartment(department);
        employee.setPosition(position);
        employee.setManager(manager);
        employee.setEmploymentType(employmentType);
        employee.setStatus(EmployeeStatus.ACTIVE);
        employee.setHireDate(hireDate != null ? hireDate : LocalDate.now());
        employee.setCreatedBy(currentUser);
        employee.setCreatedAt(LocalDateTime.now());
        
        Employee savedEmployee = employeeRepository.save(employee);
        
        // Audit log
        createAuditLog(currentUser, "EMPLOYEE_CREATED", 
                "Created employee: " + employeeId + " for candidate: " + candidate.getName(),
                true, request);
        
        log.info("Employee created: {} by {}", employeeId, currentUser.getUsername());
        return savedEmployee;
    }

    /**
     * Update employee details
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Transactional
    public Employee updateEmployee(UUID id, BigDecimal salary, String department, 
                                   String position, UUID managerId, EmploymentType employmentType,
                                   EmployeeStatus status, HttpServletRequest request) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));
        
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        
        // Update fields
        if (salary != null) {
            employee.setSalary(salary);
        }
        
        if (department != null) {
            employee.setDepartment(department);
        }
        
        if (position != null) {
            employee.setPosition(position);
        }
        
        if (managerId != null) {
            Employee manager = employeeRepository.findById(managerId)
                    .orElseThrow(() -> new RuntimeException("Manager not found"));
            employee.setManager(manager);
        }
        
        if (employmentType != null) {
            employee.setEmploymentType(employmentType);
        }
        
        if (status != null) {
            employee.setStatus(status);
        }
        
        Employee updatedEmployee = employeeRepository.save(employee);
        
        // Audit log
        createAuditLog(currentUser, "EMPLOYEE_UPDATED",
                "Updated employee: " + employee.getEmployeeId(), true, request);
        
        log.info("Employee updated: {} by {}", id, currentUser.getUsername());
        return updatedEmployee;
    }

    /**
     * Terminate employee
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Transactional
    public void terminateEmployee(UUID id, LocalDate terminationDate, HttpServletRequest request) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));
        
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        
        employee.setStatus(EmployeeStatus.TERMINATED);
        employee.setTerminationDate(terminationDate != null ? terminationDate : LocalDate.now());
        employeeRepository.save(employee);
        
        // Audit log
        createAuditLog(currentUser, "EMPLOYEE_TERMINATED",
                "Terminated employee: " + employee.getEmployeeId(), true, request);
        
        log.info("Employee terminated: {} by {}", id, currentUser.getUsername());
    }

    /**
     * Delete employee (hard delete - admin only)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteEmployee(UUID id, HttpServletRequest request) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));
        
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        
        String employeeId = employee.getEmployeeId();
        employeeRepository.delete(employee);
        
        // Audit log
        createAuditLog(currentUser, "EMPLOYEE_DELETED",
                "Deleted employee: " + employeeId, true, request);
        
        log.info("Employee deleted: {} by {}", id, currentUser.getUsername());
    }

    /**
     * Get manager's subordinates
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public List<Employee> getManagerSubordinates(UUID managerId) {
        return employeeRepository.findByManagerId(managerId);
    }

    /**
     * Get all departments
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public List<String> getAllDepartments() {
        return employeeRepository.findAllDepartments();
    }

    /**
     * Search employees by name
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public List<Employee> searchEmployees(String query) {
        log.debug("Searching employees with query: {}", query);
        return employeeRepository.searchByName(query);
    }

    /**
     * Get employee statistics
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public Map<String, Object> getEmployeeStatistics() {
        long total = employeeRepository.count();
        long active = employeeRepository.findByStatus(EmployeeStatus.ACTIVE).size();
        long onLeave = employeeRepository.findByStatus(EmployeeStatus.ON_LEAVE).size();
        long suspended = employeeRepository.findByStatus(EmployeeStatus.SUSPENDED).size();
        long terminated = employeeRepository.findByStatus(EmployeeStatus.TERMINATED).size();
        
        // Get department breakdown
        Map<String, Long> departmentCounts = employeeRepository.findAll().stream()
                .collect(Collectors.groupingBy(Employee::getDepartment, Collectors.counting()));
        
        // Get employment type breakdown
        Map<EmploymentType, Long> employmentTypeCounts = employeeRepository.findAll().stream()
                .collect(Collectors.groupingBy(Employee::getEmploymentType, Collectors.counting()));
        
        return Map.of(
            "total", total,
            "active", active,
            "onLeave", onLeave,
            "suspended", suspended,
            "terminated", terminated,
            "byDepartment", departmentCounts,
            "byEmploymentType", employmentTypeCounts
        );
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
