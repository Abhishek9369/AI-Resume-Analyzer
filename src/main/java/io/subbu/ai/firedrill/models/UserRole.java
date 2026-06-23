package io.subbu.ai.firedrill.models;

/**
 * User role enumeration for RBAC
 * 
 * Role hierarchy (highest to lowest):
 * - ADMIN: Full system access
 * - RECRUITER: Job and candidate management
 * - HR: Employee and payroll management
 * - HIRING_MANAGER: Review and feedback access
 */
public enum UserRole {
    /**
     * System administrator - full access to all features
     */
    ADMIN("Administrator", "Full system access including user management, settings, and audit logs"),
    
    /**
     * Recruiter - manages job requirements and candidates
     */
    RECRUITER("Recruiter", "Create and manage job requirements, upload and review candidates"),
    
    /**
     * Human Resources - manages employees and payroll
     */
    HR("Human Resources", "Manage employees, payroll, and onboarding processes"),
    
    /**
     * Hiring Manager - reviews candidates and provides feedback
     */
    HIRING_MANAGER("Hiring Manager", "Review job requirements and candidates, provide feedback");

    private final String displayName;
    private final String description;

    UserRole(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if this role has higher or equal privilege than another role
     */
    public boolean hasPrivilege(UserRole otherRole) {
        return this.ordinal() <= otherRole.ordinal();
    }

    /**
     * Check if this role can manage users
     */
    public boolean canManageUsers() {
        return this == ADMIN;
    }

    /**
     * Check if this role can access recruitment features
     */
    public boolean canAccessRecruitment() {
        return this == ADMIN || this == RECRUITER || this == HIRING_MANAGER;
    }

    /**
     * Check if this role can manage employees
     */
    public boolean canManageEmployees() {
        return this == ADMIN || this == HR;
    }

    /**
     * Check if this role can provide feedback
     */
    public boolean canProvideFeedback() {
        return this == ADMIN || this == HIRING_MANAGER;
    }
}
