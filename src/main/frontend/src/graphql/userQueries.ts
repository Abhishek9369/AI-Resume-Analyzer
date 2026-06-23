/**
 * GraphQL queries for User Management
 */

/**
 * Query all users
 */
export const ALL_USERS = `
  query {
    allUsers {
      id
      username
      email
      fullName
      role
      isActive
      createdAt
      updatedAt
      lastLoginAt
    }
  }
`

/**
 * Query users by role
 */
export const USERS_BY_ROLE = `
  query UsersByRole($role: UserRole!) {
    usersByRole(role: $role) {
      id
      username
      email
      fullName
      role
      isActive
      createdAt
      updatedAt
      lastLoginAt
    }
  }
`

/**
 * Search users
 */
export const SEARCH_USERS = `
  query SearchUsers($searchTerm: String!) {
    searchUsers(searchTerm: $searchTerm) {
      id
      username
      email
      fullName
      role
      isActive
      createdAt
      updatedAt
      lastLoginAt
    }
  }
`

/**
 * Get user by ID
 */
export const GET_USER = `
  query GetUser($id: UUID!) {
    user(id: $id) {
      id
      username
      email
      fullName
      role
      isActive
      createdAt
      updatedAt
      lastLoginAt
    }
  }
`

/**
 * Create user mutation
 */
export const CREATE_USER = `
  mutation CreateUser($input: UserInput!) {
    createUser(input: $input) {
      id
      username
      email
      fullName
      role
      isActive
      createdAt
    }
  }
`

/**
 * Update user mutation
 */
export const UPDATE_USER = `
  mutation UpdateUser($id: UUID!, $input: UpdateUserInput!) {
    updateUser(id: $id, input: $input) {
      id
      username
      email
      fullName
      role
      isActive
      updatedAt
    }
  }
`

/**
 * Delete user mutation
 */
export const DELETE_USER = `
  mutation DeleteUser($id: UUID!) {
    deleteUser(id: $id)
  }
`

/**
 * Activate user mutation
 */
export const ACTIVATE_USER = `
  mutation ActivateUser($id: UUID!) {
    activateUser(id: $id) {
      id
      username
      isActive
    }
  }
`

/**
 * Deactivate user mutation
 */
export const DEACTIVATE_USER = `
  mutation DeactivateUser($id: UUID!) {
    deactivateUser(id: $id) {
      id
      username
      isActive
    }
  }
`

/**
 * Reset user password mutation
 */
export const RESET_USER_PASSWORD = `
  mutation ResetUserPassword($id: UUID!, $newPassword: String!) {
    resetUserPassword(id: $id, newPassword: $newPassword)
  }
`
