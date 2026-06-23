# RBAC Implementation Plan - Resume Analyzer

**Date:** February 18, 2026  
**Status:** Planning Phase  
**Estimated Effort:** 40-60 hours  

---

## Overview

Implement comprehensive Role-Based Access Control (RBAC) with four roles and associated workflows for the Resume Analyzer application.

### Roles & Permissions Matrix

| Feature | Admin | Recruiter | HR | Hiring Manager |
|---------|-------|-----------|-------|----------------|
| **User Management** | ✅ Full | ❌ No | ❌ No | ❌ No |
| **Skills Master Data** | ✅ Full | ❌ Read | ❌ Read | ❌ Read |
| **Job Requirements** | ✅ Full | ✅ Full | ❌ Read | ❌ Read + Feedback |
| **Candidates** | ✅ Full | ✅ Full | ❌ Read | ❌ Read + Feedback |
| **Employees** | ✅ Full | ❌ No | ✅ Full | ❌ No |
| **Payroll** | ✅ Full | ❌ No | ✅ Full | ❌ No |
| **File Uploads** | ✅ All | ✅ Own | ✅ Own | ✅ Own |
| **Reports - Recruitment** | ✅ Yes | ✅ Yes | ❌ No | ✅ Yes |
| **Reports - Employee** | ✅ Yes | ❌ No | ✅ Yes | ❌ No |
| **System Settings** | ✅ Full | ❌ Read | ❌ Read | ❌ Read |
| **System Health** | ✅ Full | ❌ No | ❌ No | ❌ No |
| **Audit Logs** | ✅ Full | ❌ No | ❌ No | ❌ No |
| **Integrations** | ✅ Full | ❌ No | ❌ No | ❌ No |

---

## Workflow Design

### Recruitment Workflow

```
1. RECRUITER creates Job Requirement
   ↓
2. RECRUITER uploads candidate resumes
   ↓
3. SYSTEM processes and matches candidates
   ↓
4. RECRUITER reviews matches, shortlists candidates
   ↓
5. HIRING MANAGER reviews shortlisted candidates
   ↓
6. HIRING MANAGER provides feedback on candidates
   ↓
7. RECRUITER updates candidate status based on feedback
   ↓
8. [Future] RECRUITER schedules interviews
   ↓
9. [Future] HIRING MANAGER conducts interviews, provides feedback
   ↓
10. RECRUITER makes final selection
    ↓
11. HR onboards selected candidate as employee
```

### User Management Workflow

```
1. ADMIN creates user account
   ↓
2. ADMIN assigns role (Admin/Recruiter/HR/Hiring Manager)
   ↓
3. ADMIN sends activation email
   ↓
4. USER sets password and activates account
   ↓
5. USER logs in with assigned role
   ↓
6. SYSTEM shows role-appropriate features
```

---

## Phase 1: Backend Core RBAC (8-10 hours)

### 1.1 Database Schema

**New Tables:**

```sql
-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role VARCHAR(20) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    CONSTRAINT valid_role CHECK (role IN ('ADMIN', 'RECRUITER', 'HR', 'HIRING_MANAGER'))
);

-- Employee table (managed by HR)
CREATE TABLE employees (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id VARCHAR(50) UNIQUE NOT NULL,
    candidate_id UUID REFERENCES candidates(id),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20),
    department VARCHAR(100),
    position VARCHAR(100),
    hire_date DATE NOT NULL,
    salary DECIMAL(12, 2),
    employment_type VARCHAR(20),
    manager_id UUID REFERENCES employees(id),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    CONSTRAINT valid_employment_type CHECK (employment_type IN ('FULL_TIME', 'PART_TIME', 'CONTRACT', 'INTERN')),
    CONSTRAINT valid_status CHECK (status IN ('ACTIVE', 'ON_LEAVE', 'SUSPENDED', 'TERMINATED'))
);

-- Feedback table
CREATE TABLE feedback (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(20) NOT NULL,
    entity_id UUID NOT NULL,
    feedback_type VARCHAR(20) NOT NULL,
    rating INTEGER,
    comments TEXT,
    provided_by UUID REFERENCES users(id) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT valid_entity_type CHECK (entity_type IN ('CANDIDATE', 'JOB_REQUIREMENT')),
    CONSTRAINT valid_feedback_type CHECK (feedback_type IN ('SHORTLIST', 'REJECT', 'INTERVIEW', 'OFFER', 'GENERAL')),
    CONSTRAINT valid_rating CHECK (rating BETWEEN 1 AND 5)
);

-- Audit log table
CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50),
    entity_id UUID,
    details TEXT,
    ip_address VARCHAR(45),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- System health table
CREATE TABLE system_health (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_name VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    message TEXT,
    last_checked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    details JSONB,
    CONSTRAINT valid_status CHECK (status IN ('UP', 'DOWN', 'DEGRADED'))
);
```

### 1.2 Java Entities

**Files to create:**
- `User.java`
- `UserRole.java` (enum)
- `Employee.java`
- `EmploymentType.java` (enum)
- `EmployeeStatus.java` (enum)
- `Feedback.java`
- `FeedbackType.java` (enum)
- `AuditLog.java`
- `SystemHealth.java`
- `ServiceStatus.java` (enum)

### 1.3 Spring Security Configuration

**Dependencies to add (pom.xml):**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
```

**Configuration classes:**
- `SecurityConfig.java` - Main security configuration
- `JwtAuthenticationFilter.java` - JWT token validation
- `JwtTokenProvider.java` - Token generation/validation
- `UserDetailsServiceImpl.java` - Load user details
- `AuditLogAspect.java` - AOP for audit logging

### 1.4 Repositories

**Files to create:**
- `UserRepository.java`
- `EmployeeRepository.java`
- `FeedbackRepository.java`
- `AuditLogRepository.java`
- `SystemHealthRepository.java`

### 1.5 Services

**Files to create:**
- `AuthenticationService.java` - Login, token generation
- `UserService.java` - User CRUD operations
- `EmployeeService.java` - Employee management
- `FeedbackService.java` - Feedback management
- `AuditLogService.java` - Audit trail
- `SystemHealthService.java` - Health checks
- `PermissionService.java` - Permission checks

### 1.6 Controllers & Resolvers

**REST Controllers:**
- `AuthController.java` - /api/auth/** endpoints
- `UserController.java` - /api/users/** endpoints
- `EmployeeController.java` - /api/employees/** endpoints
- `SystemHealthController.java` - /api/health/** endpoints

**GraphQL Resolvers:**
- `UserResolver.java` - User queries and mutations
- `EmployeeResolver.java` - Employee queries and mutations
- `FeedbackResolver.java` - Feedback queries and mutations

### 1.7 Security Annotations

Update existing resolvers/controllers with:
- `@PreAuthorize("hasRole('ADMIN')")`
- `@PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")`
- Custom permission annotations

---

## Phase 2: Frontend Core RBAC (10-12 hours)

### 2.1 Authentication State Management

**Redux Slices:**
- `authSlice.ts` - Authentication state
- `usersSlice.ts` - User management state
- `employeesSlice.ts` - Employee management state
- `feedbackSlice.ts` - Feedback state
- `systemHealthSlice.ts` - System health state

**Sagas:**
- `authSaga.ts` - Login, logout, token refresh
- `usersSaga.ts` - User CRUD operations
- `employeesSaga.ts` - Employee operations
- `feedbackSaga.ts` - Feedback operations

### 2.2 API Services

**Files to create:**
- `src/services/auth.ts` - Authentication API calls
- `src/services/users.ts` - User management API calls
- `src/services/employees.ts` - Employee API calls
- `src/services/feedback.ts` - Feedback API calls
- `src/services/systemHealth.ts` - Health check API calls

### 2.3 Authentication Components

**Components:**
- `Login.tsx` - Login page
- `PrivateRoute.tsx` - Protected route wrapper
- `RoleBasedRoute.tsx` - Role-specific route wrapper
- `PermissionGate.tsx` - Component-level permission check

### 2.4 User Management Components

**Pages:**
- `UserManagement.tsx` - User list and management (Admin only)
- `CreateUser.tsx` - Create new user modal
- `EditUser.tsx` - Edit user modal
- `UserProfile.tsx` - User profile page

### 2.5 Employee Management Components

**Pages:**
- `EmployeeManagement.tsx` - Employee list (HR)
- `CreateEmployee.tsx` - Create employee modal
- `EditEmployee.tsx` - Edit employee modal
- `EmployeeProfile.tsx` - Employee details

### 2.6 Admin Dashboard

**Components:**
- `AdminDashboard.tsx` - Main admin dashboard
- `SystemHealthWidget.tsx` - System health status
- `QuickStatsWidget.tsx` - Quick statistics
- `RecentActivityWidget.tsx` - Recent user activity
- `UploadStatusWidget.tsx` - File upload status
- `AlertsWidget.tsx` - System alerts

### 2.7 Feedback Components

**Components:**
- `FeedbackForm.tsx` - Submit feedback
- `FeedbackList.tsx` - View feedback
- `FeedbackBadge.tsx` - Feedback rating badge

### 2.8 Modified Navigation

**Updates needed:**
- `Layout.tsx` - Role-based menu items
- `Navigation.tsx` - Dynamic navigation based on role
- Add role indicator in header
- Add logout button

---

## Phase 3: GraphQL Schema Updates (2-3 hours)

### 3.1 New Types

```graphql
type User {
  id: ID!
  username: String!
  email: String!
  firstName: String
  lastName: String
  role: UserRole!
  isActive: Boolean!
  lastLoginAt: DateTime
  createdAt: DateTime!
  updatedAt: DateTime!
}

enum UserRole {
  ADMIN
  RECRUITER
  HR
  HIRING_MANAGER
}

type Employee {
  id: ID!
  employeeId: String!
  candidate: Candidate
  firstName: String!
  lastName: String!
  email: String!
  phone: String
  department: String
  position: String
  hireDate: Date!
  salary: Float
  employmentType: EmploymentType!
  manager: Employee
  status: EmployeeStatus!
  createdAt: DateTime!
  updatedAt: DateTime!
}

enum EmploymentType {
  FULL_TIME
  PART_TIME
  CONTRACT
  INTERN
}

enum EmployeeStatus {
  ACTIVE
  ON_LEAVE
  SUSPENDED
  TERMINATED
}

type Feedback {
  id: ID!
  entityType: EntityType!
  entityId: ID!
  feedbackType: FeedbackType!
  rating: Int
  comments: String
  providedBy: User!
  createdAt: DateTime!
  updatedAt: DateTime!
}

enum EntityType {
  CANDIDATE
  JOB_REQUIREMENT
}

enum FeedbackType {
  SHORTLIST
  REJECT
  INTERVIEW
  OFFER
  GENERAL
}

type SystemHealth {
  serviceName: String!
  status: ServiceStatus!
  message: String
  lastCheckedAt: DateTime!
}

enum ServiceStatus {
  UP
  DOWN
  DEGRADED
}

type DashboardStats {
  totalCandidates: Int!
  totalJobs: Int!
  totalEmployees: Int!
  activeUploads: Int!
  pendingFeedback: Int!
  systemHealth: SystemHealth!
}

# Mutations
type Mutation {
  # Authentication
  login(username: String!, password: String!): AuthPayload!
  refreshToken(refreshToken: String!): AuthPayload!
  
  # User management (Admin only)
  createUser(input: CreateUserInput!): User!
  updateUser(id: ID!, input: UpdateUserInput!): User!
  deleteUser(id: ID!): Boolean!
  activateUser(id: ID!): User!
  deactivateUser(id: ID!): User!
  
  # Employee management (HR/Admin)
  createEmployee(input: CreateEmployeeInput!): Employee!
  updateEmployee(id: ID!, input: UpdateEmployeeInput!): Employee!
  deleteEmployee(id: ID!): Boolean!
  
  # Feedback (Hiring Manager/Admin)
  submitFeedback(input: SubmitFeedbackInput!): Feedback!
  updateFeedback(id: ID!, input: UpdateFeedbackInput!): Feedback!
  deleteFeedback(id: ID!): Boolean!
}

# Queries
type Query {
  # Current user
  currentUser: User!
  
  # Users (Admin only)
  allUsers: [User!]!
  user(id: ID!): User
  usersByRole(role: UserRole!): [User!]!
  
  # Employees (HR/Admin)
  allEmployees: [Employee!]!
  employee(id: ID!): Employee
  employeesByDepartment(department: String!): [Employee!]!
  employeesByStatus(status: EmployeeStatus!): [Employee!]!
  
  # Feedback
  feedbackForEntity(entityType: EntityType!, entityId: ID!): [Feedback!]!
  feedbackByUser(userId: ID!): [Feedback!]!
  
  # System health (Admin only)
  systemHealth: [SystemHealth!]!
  
  # Dashboard stats
  dashboardStats: DashboardStats!
}

type AuthPayload {
  token: String!
  refreshToken: String!
  user: User!
}

input CreateUserInput {
  username: String!
  email: String!
  password: String!
  firstName: String
  lastName: String
  role: UserRole!
}

input UpdateUserInput {
  email: String
  firstName: String
  lastName: String
  role: UserRole
}

input CreateEmployeeInput {
  employeeId: String!
  candidateId: ID
  firstName: String!
  lastName: String!
  email: String!
  phone: String
  department: String
  position: String
  hireDate: Date!
  salary: Float
  employmentType: EmploymentType!
  managerId: ID
}

input UpdateEmployeeInput {
  email: String
  phone: String
  department: String
  position: String
  salary: Float
  employmentType: EmploymentType
  managerId: ID
  status: EmployeeStatus
}

input SubmitFeedbackInput {
  entityType: EntityType!
  entityId: ID!
  feedbackType: FeedbackType!
  rating: Int
  comments: String
}

input UpdateFeedbackInput {
  rating: Int
  comments: String
}
```

### 3.2 Modified Types

Update `JobRequirement` to include:
```graphql
type JobRequirement {
  # ... existing fields
  createdBy: User
  feedback: [Feedback!]!
}
```

Update `Candidate` to include:
```graphql
type Candidate {
  # ... existing fields
  uploadedBy: User
  feedback: [Feedback!]!
  employee: Employee
}
```

Update `ProcessTracker` to include:
```graphql
type ProcessTracker {
  # ... existing fields
  uploadedBy: User
}
```

---

## Phase 4: Permission-Based UI Updates (6-8 hours)

### 4.1 Dashboard Page

**Admin View:**
- System health status
- Total users by role
- Upload statistics
- Recent activity log
- System alerts

**Recruiter View:**
- My job requirements
- My uploads
- Matching statistics
- Pending actions

**HR View:**
- Employee statistics
- Recent hires
- Department distribution
- Payroll alerts

**Hiring Manager View:**
- Pending feedback requests
- Jobs I'm reviewing
- My feedback history
- Candidate pipeline

### 4.2 Navigation Updates

```typescript
// Role-based menu items
const menuItems = {
  ADMIN: [
    { path: '/dashboard', label: 'Dashboard', icon: 'Dashboard' },
    { path: '/users', label: 'User Management', icon: 'People' },
    { path: '/upload', label: 'Upload Resumes', icon: 'Upload' },
    { path: '/candidates', label: 'Candidates', icon: 'Person' },
    { path: '/jobs', label: 'Job Requirements', icon: 'Work' },
    { path: '/employees', label: 'Employees', icon: 'Badge' },
    { path: '/matching', label: 'Candidate Matching', icon: 'Compare' },
    { path: '/skills', label: 'Skills Master', icon: 'Code' },
    { path: '/system-health', label: 'System Health', icon: 'Monitoring' },
    { path: '/audit-logs', label: 'Audit Logs', icon: 'History' },
    { path: '/settings', label: 'Settings', icon: 'Settings' }
  ],
  RECRUITER: [
    { path: '/dashboard', label: 'Dashboard', icon: 'Dashboard' },
    { path: '/upload', label: 'Upload Resumes', icon: 'Upload' },
    { path: '/candidates', label: 'Candidates', icon: 'Person' },
    { path: '/jobs', label: 'Job Requirements', icon: 'Work' },
    { path: '/matching', label: 'Candidate Matching', icon: 'Compare' }
  ],
  HR: [
    { path: '/dashboard', label: 'Dashboard', icon: 'Dashboard' },
    { path: '/employees', label: 'Employees', icon: 'Badge' },
    { path: '/candidates', label: 'Candidates', icon: 'Person' }
  ],
  HIRING_MANAGER: [
    { path: '/dashboard', label: 'Dashboard', icon: 'Dashboard' },
    { path: '/jobs', label: 'Job Requirements', icon: 'Work' },
    { path: '/candidates', label: 'Candidates', icon: 'Person' },
    { path: '/my-feedback', label: 'My Feedback', icon: 'Feedback' }
  ]
};
```

### 4.3 Permission Gates

```typescript
// Example usage
<PermissionGate roles={['ADMIN']}>
  <DeleteButton />
</PermissionGate>

<PermissionGate roles={['ADMIN', 'RECRUITER']}>
  <CreateJobButton />
</PermissionGate>

<PermissionGate roles={['ADMIN', 'HR']}>
  <EmployeeDetailsLink />
</PermissionGate>
```

---

## Phase 5: Testing & Validation (8-10 hours)

### 5.1 Backend Unit Tests

**Test files to create:**
- `UserServiceTest.java`
- `EmployeeServiceTest.java`
- `FeedbackServiceTest.java`
- `AuthenticationServiceTest.java`
- `UserResolverTest.java`
- `EmployeeResolverTest.java`
- `SecurityConfigTest.java`

**Test files to update:**
- Update all existing tests with authentication
- Add @WithMockUser annotations

### 5.2 Frontend Unit Tests

**Test files to create:**
- `Login.test.tsx`
- `UserManagement.test.tsx`
- `EmployeeManagement.test.tsx`
- `AdminDashboard.test.tsx`
- `PermissionGate.test.tsx`
- `authSlice.test.ts`
- `usersSlice.test.ts`

**Test files to update:**
- Update existing component tests with mock auth context

### 5.3 Integration Tests

**Scenarios to test:**
- Login flow
- User creation by admin
- Role assignment
- Permission checks
- Recruiter workflow (create job, upload resume)
- Hiring manager feedback workflow
- HR employee management
- Admin dashboard functionality

### 5.4 ESLint Fixes

Run and fix:
```bash
npm run lint --fix
```

Common issues to address:
- Unused imports
- Missing dependencies in useEffect
- Type assertions
- Console statements
- Any type usage

### 5.5 Browser Automation Testing

**Test scenarios with Playwright:**
1. Login as each role
2. Verify role-specific navigation
3. Test permission-based UI elements
4. Admin: Create user, assign role
5. Recruiter: Create job, upload resume
6. Hiring Manager: Provide feedback
7. HR: Create employee
8. Admin: View system health
9. Test logout and session expiration

---

## Phase 6: Documentation & Deployment (2-3 hours)

### 6.1 API Documentation

- Update GraphQL schema documentation
- Document REST endpoints
- Create Postman collection
- Add authentication examples

### 6.2 User Documentation

- Admin guide
- Recruiter guide
- HR guide
- Hiring manager guide
- Workflow diagrams

### 6.3 Deployment Checklist

- [ ] Create default admin user
- [ ] Set JWT secret in environment variables
- [ ] Configure session timeout
- [ ] Set up email notifications
- [ ] Configure HTTPS (already running on https)
- [ ] Database migrations
- [ ] Seed initial data (skills, admin user)

---

## Implementation Order

### Week 1: Backend Foundation (Days 1-5)
- Day 1: Database schema, entities, enums
- Day 2: Repositories, basic services
- Day 3: Spring Security configuration, JWT
- Day 4: Authentication endpoints, user service
- Day 5: Employee service, feedback service

### Week 2: Backend Integration (Days 6-10)
- Day 6: GraphQL schema updates
- Day 7: User/Employee resolvers
- Day 8: Permission annotations on existing endpoints
- Day 9: System health, audit logging
- Day 10: Backend testing

### Week 3: Frontend Core (Days 11-15)
- Day 11: Redux slices, authentication flow
- Day 12: Login page, private routes
- Day 13: User list Page, user CRUD
- Day 14: Employee management UI
- Day 15: Feedback components

### Week 4: Frontend Polish (Days 16-20)
- Day 16: Admin dashboard
- Day 17: Role-based navigation
- Day 18: Permission gates, UI updates
- Day 19: ESLint fixes, frontend testing
- Day 20: Browser automation, final validation

---

## Risks & Mitigation

### Risk 1: Breaking existing functionality
**Mitigation:** 
- Implement feature flags
- Progressive rollout
- Comprehensive testing
- Keep backward compatibility

### Risk 2: JWT token security
**Mitigation:**
- Use strong secret keys
- Short token expiration (15 min)
- Refresh token rotation
- HTTPS only

### Risk 3: Permission complexity
**Mitigation:**
- Clear permission matrix
- Centralized permission service
- Comprehensive testing
- Good documentation

### Risk 4: Database migration issues
**Mitigation:**
- Test migrations locally
- Backup before migration
- Rollback plan
- Gradual migration

---

## Success Criteria

- ✅ All four roles implemented
- ✅ Users can only access permitted features
- ✅ Admin can create and manage users
- ✅ Recruiter workflow functional
- ✅ Hiring manager can provide feedback
- ✅ HR can manage employees
- ✅ System health monitoring active
- ✅ Audit logging functional
- ✅ All tests passing
- ✅ No ESLint errors
- ✅ Browser automation tests pass
- ✅ Documentation complete

---

## Next Steps

1. Review and approve this plan
2. Set up feature branch: `feature/rbac-implementation`
3. Create database migration scripts
4. Begin Phase 1 implementation
5. Regular progress reviews

---

**Estimated Total Effort:** 40-60 hours  
**Estimated Timeline:** 3-4 weeks  
**Complexity:** High  
**Priority:** High  
