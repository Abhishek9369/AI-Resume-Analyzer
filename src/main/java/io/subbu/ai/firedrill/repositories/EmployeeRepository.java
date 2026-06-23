package io.subbu.ai.firedrill.repositories;

import io.subbu.ai.firedrill.entities.Employee;
import io.subbu.ai.firedrill.models.EmployeeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Employee entity
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    /**
     * Find employee by employee ID
     */
    Optional<Employee> findByEmployeeId(String employeeId);

    /**
     * Find employee by email
     */
    Optional<Employee> findByEmail(String email);

    /**
     * Find all employees by department
     */
    List<Employee> findByDepartment(String department);

    /**
     * Find all employees by status
     */
    List<Employee> findByStatus(EmployeeStatus status);

    /**
     * Find all employees by manager
     */
    List<Employee> findByManagerId(UUID managerId);

    /**
     * Find all active employees
     */
    List<Employee> findByStatusOrderByLastNameAsc(EmployeeStatus status);

    /**
     * Check if employee ID exists
     */
    boolean existsByEmployeeId(String employeeId);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Count employees by department
     */
    long countByDepartment(String department);

    /**
     * Count employees by status
     */
    long countByStatus(EmployeeStatus status);

    /**
     * Get all distinct departments
     */
    @Query("SELECT DISTINCT e.department FROM Employee e WHERE e.department IS NOT NULL ORDER BY e.department")
    List<String> findAllDepartments();

    /**
     * Search employees by name
     */
    @Query("SELECT e FROM Employee e WHERE " +
           "LOWER(e.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(e.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(e.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Employee> searchByName(@Param("searchTerm") String searchTerm);
}
