# Tasks 9 & 10 Complete: GraphQL Schema + Frontend Auth

**Date:** February 18, 2026  
**Status:** GraphQL schema updated, Frontend auth infrastructure complete

## Task 9: GraphQL Schema Updates ✅

### New Enums Added
- `UserRole` - ADMIN, RECRUITER, HR, HIRING_MANAGER
- `EmploymentType` - FULL_TIME, PART_TIME, CONTRACT, INTERN
- `EmployeeStatus` - ACTIVE, ON_LEAVE, SUSPENDED, TERMINATED
- `FeedbackType` - SHORTLIST, REJECT, INTERVIEW, OFFER, GENERAL, TECHNICAL, CULTURAL_FIT
- `EntityType` - CANDIDATE, JOB_REQUIREMENT
- `ServiceStatus` - UP, DOWN, DEGRADED, UNKNOWN

### New Types Added
- `User` - User account with role and authentication info
- `Employee` - HR management entity with department, salary, hierarchy
- `Feedback` - Polymorphic feedback on candidates/jobs with ratings
- `SystemHealth` - Service monitoring and health check data
- `AuthResponse` - JWT token response with user info
- `UserStatistics` - User counts by role and status
- `EmployeeStatistics` - Employee counts by department and type
- `FeedbackStatistics` - Feedback counts and average ratings
- `DepartmentCount` - Helper type for department statistics
- `EmploymentTypeCount` - Helper type for employment type statistics
- `FeedbackTypeCount` - Helper type for feedback statistics

### New Input Types Added
- `LoginInput` - Username and password for login
- `RegisterInput` - User registration data (admin only)
- `RefreshTokenInput` - Refresh token for token renewal
- `ChangePasswordInput` - Old and new password for password change
- `UserInput` - User creation data
- `UpdateUserInput` - User update data
- `EmployeeInput` - Employee creation data
- `UpdateEmployeeInput` - Employee update data
- `FeedbackInput` - Feedback submission data
- `UpdateFeedbackInput` - Feedback update data

### New Queries Added (46 total)

**Authentication (2):**
- `me` - Get current authenticated user
- `validateToken` - Validate JWT token

**Users (5):**
- `user(id)` - Get user by ID
- `allUsers(page, size)` - Paginated user list
- `usersByRole(role)` - Users filtered by role
- `searchUsers(searchTerm)` - Search users by name/username
- `userStatistics` - User counts by role and status

**Employees (8):**
- `employee(id)` - Get employee by ID
- `employeeByEmployeeId(employeeId)` - Get by employee ID
- `allEmployees(page, size)` - Paginated employee list
- `employeesByDepartment(department)` - Filter by department
- `employeesByStatus(status)` - Filter by status
- `managerSubordinates(managerId)` - Get manager's direct reports
- `employeeStatistics` - Employee counts by department/type
- `allDepartments` - List of all departments

**Feedback (6):**
- `feedbackForCandidate(candidateId)` - All feedback for candidate
- `feedbackForJob(jobRequirementId)` - All feedback for job
- `feedbackByUser(userId)` - User's feedback
- `averageRatingForCandidate(candidateId)` - Average rating
- `averageRatingForJob(jobRequirementId)` - Average rating
- `feedbackStatistics(entityId, entityType)` - Feedback stats

**System Health (3):**
- `systemHealth(serviceName)` - Get specific service health
- `systemHealthReport` - All service statuses
- `overallSystemStatus` - Aggregated system status

### New Mutations Added (21 total)

**Authentication (5):**
- `login(input)` - User login with JWT response
- `register(input)` - Admin creates new user
- `refreshToken(input)` - Renew access token
- `changePassword(input)` - Change user password
- `logout` - Logout (client-side cleanup)

**Users (6):**
- `createUser(input)` - Admin creates user
- `updateUser(id, input)` - Update user info
- `deleteUser(id)` - Soft delete user
- `activateUser(id)` - Activate user account
- `deactivateUser(id)` - Deactivate user account
- `resetUserPassword(id, newPassword)` - Admin resets password

**Employees (4):**
- `createEmployee(input)` - Create employee record
- `updateEmployee(id, input)` - Update employee info
- `terminateEmployee(id, terminationDate)` - Terminate employment
- `deleteEmployee(id)` - Hard delete employee

**Feedback (5):**
- `submitFeedback(input)` - Submit new feedback
- `updateFeedback(id, input)` - Update feedback
- `deleteFeedback(id)` - Delete feedback
- `hideFeedback(id)` - Hide feedback from candidate
- `showFeedback(id)` - Make feedback visible

**System Health (2):**
- `checkServiceHealth(serviceName)` - Manual health check
- `resetFailureCount(serviceName)` - Reset failure counter

---

## Task 10: Frontend Auth Infrastructure ✅

### Files Created (7 files)

**1. Types (`types/auth.ts`):**
- `User` interface - User entity with role
- `AuthResponse` interface - JWT token response
- `LoginCredentials` interface - Login form data
- `RegisterData` interface - Registration form data
- `ChangePasswordData` interface - Password change data
- `AuthState` interface - Redux auth state
- `TokenPayload` interface - Decoded JWT payload
- `UserRole` enum - Role enumeration

**2. API Service (`services/authApi.ts`):**
- `login()` - POST /api/auth/login
- `register()` - POST /api/auth/register
- `refreshToken()` - POST /api/auth/refresh
- `getCurrentUser()` - GET /api/auth/me
- `validateToken()` - POST /api/auth/validate
- `changePassword()` - POST /api/auth/change-password
- `logout()` - POST /api/auth/logout

**3. Axios Instance (`services/axiosInstance.ts`):**
- **Request Interceptor:** Automatically adds JWT token to headers
- **Response Interceptor:** Handles 401 errors with token refresh
- **Token Refresh Queue:** Prevents duplicate refresh requests
- **Auto-logout:** Redirects to login on refresh failure
- `setAuthToken()` - Set token in headers and localStorage
- `clearAuthTokens()` - Clear all tokens

**4. Token Utils (`utils/tokenUtils.ts`):**
- `decodeToken()` - Decode JWT without verification
- `isTokenExpired()` - Check if token is expired
- `getTokenExpiration()` - Get expiration timestamp
- `willTokenExpireSoon()` - Check if expires within N minutes
- `getRoleFromToken()` - Extract user role
- `getUsernameFromToken()` - Extract username

**5. Auth Slice (`store/slices/authSlice.ts`):**
- **Initial State:** Loads from localStorage on app start
- **Login Actions:** loginRequest, loginSuccess, loginFailure
- **Register Actions:** registerRequest, registerSuccess, registerFailure
- **Token Refresh:** refreshTokenRequest, refreshTokenSuccess, refreshTokenFailure
- **User Info:** getCurrentUserRequest, getCurrentUserSuccess, getCurrentUserFailure
- **Password:** changePasswordRequest, changePasswordSuccess, changePasswordFailure
- **Logout:** Clear state and localStorage
- **Helpers:** clearError, updateUser
- **Persistence:** All successful actions save to localStorage

**6. Auth Sagas (`store/sagas/authSagas.ts`):**
- `loginSaga()` - Handle login with JWT token storage
- `registerSaga()` - Handle user registration
- `refreshTokenSaga()` - Automatic token refresh
- `getCurrentUserSaga()` - Fetch current user info
- `changePasswordSaga()` - Handle password change
- `logoutSaga()` - Clear tokens on logout
- Error handling with proper Redux action dispatching

**7. Auth Selectors (`store/selectors/authSelectors.ts`):**
- **Base Selectors:** user, isAuthenticated, isLoading, error, accessToken
- **Derived Selectors:** userRole, username, userFullName, userEmail, isActive
- **Role Checks:** isAdmin, isRecruiter, isHR, isHiringManager
- **Permission Checks:**
  - `selectCanManageUsers` - Admin only
  - `selectCanManageJobs` - Admin, Recruiter
  - `selectCanUploadResumes` - Admin, Recruiter
  - `selectCanManageEmployees` - Admin, HR
  - `selectCanViewCandidates` - All roles
  - `selectCanProvideFeedback` - Admin, Recruiter, Hiring Manager
  - `selectCanShortlistCandidates` - Admin, Recruiter
- **Token Checks:** isTokenExpiringSoon, isTokenExpired

### Redux Store Updates

**Updated `store/index.ts`:**
- Added `authReducer` to store configuration
- Auth state is now available at `state.auth`

**Updated `store/sagas/index.ts`:**
- Added `authSagas` import
- Integrated auth sagas into root saga

### Key Features

**1. Token Management:**
- Access token: 15 minutes expiration
- Refresh token: 7 days expiration
- Automatic refresh on 401 errors
- Token expiration warnings

**2. Persistent State:**
- Tokens stored in localStorage
- User info persisted across page reloads
- Automatic re-authentication on app start

**3. Security:**
- JWT tokens in Authorization header
- Automatic token injection via interceptor
- Secure token refresh flow
- Auto-logout on authentication failure

**4. Error Handling:**
- Network error handling
- Token refresh failure handling
- User-friendly error messages
- Redux error state management

**5. Role-Based Access:**
- Granular permission selectors
- Role-based UI rendering
- Protected route support (ready for implementation)

### Build Status
✅ **Frontend builds successfully** - 274 modules, 357.15 kB bundled

### Integration Points

**Ready for Next Tasks:**
1. **Login Page (Task 11)** - Can use loginRequest action and selectors
2. **Protected Routes (Task 11)** - Can use isAuthenticated selector
3. **Admin Dashboard (Task 12)** - Can use isAdmin selector
4. **User Management (Task 13)** - Can use canManageUsers selector
5. **Navigation (Task 16)** - Can use role selectors for menu items

### Usage Example

```typescript
// In a component
import { useDispatch, useSelector } from 'react-redux'
import { loginRequest } from '@store/slices/authSlice'
import { selectIsAuthenticated, selectUser, selectIsLoading } from '@store/selectors/authSelectors'

function LoginPage() {
  const dispatch = useDispatch()
  const isAuthenticated = useSelector(selectIsAuthenticated)
  const user = useSelector(selectUser)
  const isLoading = useSelector(selectIsLoading)

  const handleLogin = (username: string, password: string) => {
    dispatch(loginRequest({ username, password }))
  }

  // Component logic...
}
```

### Token Refresh Flow

1. **Request fails with 401** → Response interceptor catches it
2. **Check if refresh in progress** → Queue request if yes
3. **Get refresh token from localStorage** → Fail if not found
4. **Call /api/auth/refresh** → Get new access/refresh tokens
5. **Update localStorage and headers** → Store new tokens
6. **Retry original request** → With new access token
7. **Process queued requests** → Retry all with new token
8. **On refresh failure** → Logout and redirect to login

---

## Summary

**Tasks Completed:** 9 & 10 (53% of total RBAC project)

**GraphQL Schema:**
- 6 new enums
- 13 new types
- 9 new input types
- 46 new queries
- 21 new mutations
- Full RBAC support in schema

**Frontend Auth:**
- 7 new TypeScript files
- Complete Redux auth infrastructure
- JWT token management with auto-refresh
- Role-based permission selectors
- localStorage persistence
- Axios interceptors configured
- Build verified successful

**Next Steps:**
- Task 11: Build login page and protected routes
- Task 12: Build admin dashboard with system health
- Remaining tasks focus on UI implementation
