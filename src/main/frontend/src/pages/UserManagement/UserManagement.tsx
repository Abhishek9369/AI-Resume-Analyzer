/**
 * User Management Page
 * Allows admins to manage system users
 */

import { useState, useEffect, FormEvent } from 'react'
import { graphqlClient } from '@/services/graphql'
import {
  ALL_USERS,
  CREATE_USER,
  UPDATE_USER,
  DELETE_USER,
  ACTIVATE_USER,
  DEACTIVATE_USER,
  RESET_USER_PASSWORD,
  SEARCH_USERS,
} from '@/graphql/userQueries'
import { UserRole } from '@/types/auth'
import styles from './UserManagement.module.css'

interface User {
  id: string
  username: string
  email: string
  fullName: string
  role: UserRole
  isActive: boolean
  createdAt: string
  updatedAt: string
  lastLoginAt?: string
}

interface UserFormData {
  username: string
  password: string
  email: string
  fullName: string
  role: UserRole
}

interface UpdateUserFormData {
  email: string
  fullName: string
  role: UserRole
  isActive: boolean
}

export default function UserManagement() {
  const [users, setUsers] = useState<User[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [searchTerm, setSearchTerm] = useState('')
  
  // Modal states
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [showEditModal, setShowEditModal] = useState(false)
  const [showDeleteModal, setShowDeleteModal] = useState(false)
  const [showPasswordModal, setShowPasswordModal] = useState(false)
  const [selectedUser, setSelectedUser] = useState<User | null>(null)
  
  // Form data
  const [createFormData, setCreateFormData] = useState<UserFormData>({
    username: '',
    password: '',
    email: '',
    fullName: '',
    role: UserRole.RECRUITER,
  })
  
  const [editFormData, setEditFormData] = useState<UpdateUserFormData>({
    email: '',
    fullName: '',
    role: UserRole.RECRUITER,
    isActive: true,
  })
  
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')

  // Fetch all users
  const fetchUsers = async () => {
    setLoading(true)
    setError(null)
    try {
      const result = await graphqlClient.request<{ allUsers: User[] }>(ALL_USERS)
      setUsers(result.allUsers)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load users')
    } finally {
      setLoading(false)
    }
  }

  // Search users
  const handleSearch = async (term: string) => {
    if (!term.trim()) {
      fetchUsers()
      return
    }
    
    setLoading(true)
    setError(null)
    try {
      const result = await graphqlClient.request<{ searchUsers: User[] }>(
        SEARCH_USERS,
        { searchTerm: term }
      )
      setUsers(result.searchUsers)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Search failed')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchUsers()
  }, [])

  // Create user
  const handleCreateUser = async (e: FormEvent) => {
    e.preventDefault()
    
    try {
      await graphqlClient.request(CREATE_USER, { input: createFormData })
      setShowCreateModal(false)
      setCreateFormData({
        username: '',
        password: '',
        email: '',
        fullName: '',
        role: UserRole.RECRUITER,
      })
      fetchUsers()
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to create user')
    }
  }

  // Update user
  const handleUpdateUser = async (e: FormEvent) => {
    e.preventDefault()
    if (!selectedUser) return
    
    try {
      await graphqlClient.request(UPDATE_USER, {
        id: selectedUser.id,
        input: editFormData,
      })
      setShowEditModal(false)
      setSelectedUser(null)
      fetchUsers()
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to update user')
    }
  }

  // Delete user
  const handleDeleteUser = async () => {
    if (!selectedUser) return
    
    try {
      await graphqlClient.request(DELETE_USER, { id: selectedUser.id })
      setShowDeleteModal(false)
      setSelectedUser(null)
      fetchUsers()
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to delete user')
    }
  }

  // Toggle user active status
  const handleToggleActive = async (user: User) => {
    try {
      if (user.isActive) {
        await graphqlClient.request(DEACTIVATE_USER, { id: user.id })
      } else {
        await graphqlClient.request(ACTIVATE_USER, { id: user.id })
      }
      fetchUsers()
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to toggle user status')
    }
  }

  // Reset password
  const handleResetPassword = async (e: FormEvent) => {
    e.preventDefault()
    if (!selectedUser) return
    
    if (newPassword !== confirmPassword) {
      alert('Passwords do not match')
      return
    }
    
    if (newPassword.length < 6) {
      alert('Password must be at least 6 characters')
      return
    }
    
    try {
      await graphqlClient.request(RESET_USER_PASSWORD, {
        id: selectedUser.id,
        newPassword,
      })
      setShowPasswordModal(false)
      setSelectedUser(null)
      setNewPassword('')
      setConfirmPassword('')
      alert('Password reset successfully')
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to reset password')
    }
  }

  // Open edit modal
  const openEditModal = (user: User) => {
    setSelectedUser(user)
    setEditFormData({
      email: user.email,
      fullName: user.fullName,
      role: user.role,
      isActive: user.isActive,
    })
    setShowEditModal(true)
  }

  // Open delete modal
  const openDeleteModal = (user: User) => {
    setSelectedUser(user)
    setShowDeleteModal(true)
  }

  // Open password modal
  const openPasswordModal = (user: User) => {
    setSelectedUser(user)
    setNewPassword('')
    setConfirmPassword('')
    setShowPasswordModal(true)
  }

  // Role badge color
  const getRoleBadgeClass = (role: UserRole) => {
    switch (role) {
      case UserRole.ADMIN:
        return styles.roleAdmin
      case UserRole.RECRUITER:
        return styles.roleRecruiter
      case UserRole.HR:
        return styles.roleHr
      case UserRole.HIRING_MANAGER:
        return styles.roleManager
      default:
        return ''
    }
  }

  if (loading && users.length === 0) {
    return (
      <div className={styles.container}>
        <div className={styles.loading}>
          <div className={styles.spinner}></div>
          <p>Loading users...</p>
        </div>
      </div>
    )
  }

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <h1>User Management</h1>
        <button onClick={() => setShowCreateModal(true)} className={styles.createButton}>
          + Create User
        </button>
      </div>

      {/* Search Bar */}
      <div className={styles.searchBar}>
        <input
          type="text"
          placeholder="Search users by name, email, or username..."
          value={searchTerm}
          onChange={(e) => {
            setSearchTerm(e.target.value)
            handleSearch(e.target.value)
          }}
          className={styles.searchInput}
        />
      </div>

      {error && (
        <div className={styles.error}>
          <p>{error}</p>
          <button onClick={fetchUsers} className={styles.retryButton}>
            Retry
          </button>
        </div>
      )}

      {/* Users Table */}
      <div className={styles.tableContainer}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>Username</th>
              <th>Full Name</th>
              <th>Email</th>
              <th>Role</th>
              <th>Status</th>
              <th>Last Login</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {users.map((user) => (
              <tr key={user.id}>
                <td className={styles.username}>{user.username}</td>
                <td>{user.fullName}</td>
                <td>{user.email}</td>
                <td>
                  <span className={`${styles.roleBadge} ${getRoleBadgeClass(user.role)}`}>
                    {user.role}
                  </span>
                </td>
                <td>
                  <span className={user.isActive ? styles.statusActive : styles.statusInactive}>
                    {user.isActive ? 'Active' : 'Inactive'}
                  </span>
                </td>
                <td className={styles.timestamp}>
                  {user.lastLoginAt
                    ? new Date(user.lastLoginAt).toLocaleString()
                    : 'Never'}
                </td>
                <td className={styles.actions}>
                  <button
                    onClick={() => openEditModal(user)}
                    className={styles.actionButton}
                    title="Edit user"
                  >
                    ✏️
                  </button>
                  <button
                    onClick={() => handleToggleActive(user)}
                    className={styles.actionButton}
                    title={user.isActive ? 'Deactivate' : 'Activate'}
                  >
                    {user.isActive ? '🔒' : '🔓'}
                  </button>
                  <button
                    onClick={() => openPasswordModal(user)}
                    className={styles.actionButton}
                    title="Reset password"
                  >
                    🔑
                  </button>
                  <button
                    onClick={() => openDeleteModal(user)}
                    className={`${styles.actionButton} ${styles.deleteButton}`}
                    title="Delete user"
                  >
                    🗑️
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Create User Modal */}
      {showCreateModal && (
        <div className={styles.modal} onClick={() => setShowCreateModal(false)}>
          <div className={styles.modalContent} onClick={(e) => e.stopPropagation()}>
            <h2>Create New User</h2>
            <form onSubmit={handleCreateUser}>
              <div className={styles.formGroup}>
                <label>Username *</label>
                <input
                  type="text"
                  required
                  value={createFormData.username}
                  onChange={(e) =>
                    setCreateFormData({ ...createFormData, username: e.target.value })
                  }
                />
              </div>
              <div className={styles.formGroup}>
                <label>Password *</label>
                <input
                  type="password"
                  required
                  minLength={6}
                  value={createFormData.password}
                  onChange={(e) =>
                    setCreateFormData({ ...createFormData, password: e.target.value })
                  }
                />
              </div>
              <div className={styles.formGroup}>
                <label>Full Name *</label>
                <input
                  type="text"
                  required
                  value={createFormData.fullName}
                  onChange={(e) =>
                    setCreateFormData({ ...createFormData, fullName: e.target.value })
                  }
                />
              </div>
              <div className={styles.formGroup}>
                <label>Email *</label>
                <input
                  type="email"
                  required
                  value={createFormData.email}
                  onChange={(e) =>
                    setCreateFormData({ ...createFormData, email: e.target.value })
                  }
                />
              </div>
              <div className={styles.formGroup}>
                <label>Role *</label>
                <select
                  required
                  value={createFormData.role}
                  onChange={(e) =>
                    setCreateFormData({
                      ...createFormData,
                      role: e.target.value as UserRole,
                    })
                  }
                >
                  <option value={UserRole.ADMIN}>Admin</option>
                  <option value={UserRole.RECRUITER}>Recruiter</option>
                  <option value={UserRole.HR}>HR</option>
                  <option value={UserRole.HIRING_MANAGER}>Hiring Manager</option>
                </select>
              </div>
              <div className={styles.modalActions}>
                <button type="button" onClick={() => setShowCreateModal(false)}>
                  Cancel
                </button>
                <button type="submit" className={styles.submitButton}>
                  Create User
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Edit User Modal */}
      {showEditModal && selectedUser && (
        <div className={styles.modal} onClick={() => setShowEditModal(false)}>
          <div className={styles.modalContent} onClick={(e) => e.stopPropagation()}>
            <h2>Edit User: {selectedUser.username}</h2>
            <form onSubmit={handleUpdateUser}>
              <div className={styles.formGroup}>
                <label>Full Name *</label>
                <input
                  type="text"
                  required
                  value={editFormData.fullName}
                  onChange={(e) =>
                    setEditFormData({ ...editFormData, fullName: e.target.value })
                  }
                />
              </div>
              <div className={styles.formGroup}>
                <label>Email *</label>
                <input
                  type="email"
                  required
                  value={editFormData.email}
                  onChange={(e) =>
                    setEditFormData({ ...editFormData, email: e.target.value })
                  }
                />
              </div>
              <div className={styles.formGroup}>
                <label>Role *</label>
                <select
                  required
                  value={editFormData.role}
                  onChange={(e) =>
                    setEditFormData({
                      ...editFormData,
                      role: e.target.value as UserRole,
                    })
                  }
                >
                  <option value={UserRole.ADMIN}>Admin</option>
                  <option value={UserRole.RECRUITER}>Recruiter</option>
                  <option value={UserRole.HR}>HR</option>
                  <option value={UserRole.HIRING_MANAGER}>Hiring Manager</option>
                </select>
              </div>
              <div className={styles.formGroup}>
                <label className={styles.checkboxLabel}>
                  <input
                    type="checkbox"
                    checked={editFormData.isActive}
                    onChange={(e) =>
                      setEditFormData({ ...editFormData, isActive: e.target.checked })
                    }
                  />
                  Active
                </label>
              </div>
              <div className={styles.modalActions}>
                <button type="button" onClick={() => setShowEditModal(false)}>
                  Cancel
                </button>
                <button type="submit" className={styles.submitButton}>
                  Update User
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {showDeleteModal && selectedUser && (
        <div className={styles.modal} onClick={() => setShowDeleteModal(false)}>
          <div className={styles.modalContent} onClick={(e) => e.stopPropagation()}>
            <h2>Delete User</h2>
            <p>
              Are you sure you want to delete user <strong>{selectedUser.username}</strong> (
              {selectedUser.fullName})?
            </p>
            <p className={styles.warningText}>This action cannot be undone.</p>
            <div className={styles.modalActions}>
              <button type="button" onClick={() => setShowDeleteModal(false)}>
                Cancel
              </button>
              <button
                type="button"
                onClick={handleDeleteUser}
                className={styles.deleteConfirmButton}
              >
                Delete User
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Reset Password Modal */}
      {showPasswordModal && selectedUser && (
        <div className={styles.modal} onClick={() => setShowPasswordModal(false)}>
          <div className={styles.modalContent} onClick={(e) => e.stopPropagation()}>
            <h2>Reset Password: {selectedUser.username}</h2>
            <form onSubmit={handleResetPassword}>
              <div className={styles.formGroup}>
                <label>New Password *</label>
                <input
                  type="password"
                  required
                  minLength={6}
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                />
              </div>
              <div className={styles.formGroup}>
                <label>Confirm Password *</label>
                <input
                  type="password"
                  required
                  minLength={6}
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                />
              </div>
              <div className={styles.modalActions}>
                <button type="button" onClick={() => setShowPasswordModal(false)}>
                  Cancel
                </button>
                <button type="submit" className={styles.submitButton}>
                  Reset Password
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
