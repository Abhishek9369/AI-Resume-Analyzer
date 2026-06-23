/**
 * Employee Management Page
 * Allows HR to manage employee records
 */

import { useState, useEffect, FormEvent } from 'react'
import { graphqlClient, GET_ALL_CANDIDATES } from '@/services/graphql'
import {
  ALL_EMPLOYEES,
  CREATE_EMPLOYEE,
  UPDATE_EMPLOYEE,
  TERMINATE_EMPLOYEE,
  DELETE_EMPLOYEE,
  ALL_DEPARTMENTS,
} from '@/graphql/employeeQueries'
import styles from './EmployeeManagement.module.css'

enum EmploymentType {
  FULL_TIME = 'FULL_TIME',
  PART_TIME = 'PART_TIME',
  CONTRACT = 'CONTRACT',
  INTERN = 'INTERN',
}

enum EmployeeStatus {
  ACTIVE = 'ACTIVE',
  ON_LEAVE = 'ON_LEAVE',
  SUSPENDED = 'SUSPENDED',
  TERMINATED = 'TERMINATED',
}

interface Candidate {
  id: string
  name: string
  email: string
}

interface User {
  id: string
  username: string
  fullName: string
}

interface Manager {
  id: string
  employeeId: string
  user?: {
    fullName: string
  }
}

interface Employee {
  id: string
  employeeId: string
  candidate?: Candidate
  user?: User
  department: string
  position: string
  salary: number
  employmentType: EmploymentType
  hireDate: string
  manager?: Manager
  status: EmployeeStatus
  terminationDate?: string
  createdAt: string
  updatedAt: string
}

interface CreateEmployeeFormData {
  employeeId: string
  candidateId: string
  userId: string
  department: string
  position: string
  salary: number
  employmentType: EmploymentType
  hireDate: string
  managerId: string
}

interface UpdateEmployeeFormData {
  department: string
  position: string
  salary: number
  employmentType: EmploymentType
  managerId: string
  status: EmployeeStatus
}

export default function EmployeeManagement() {
  const [employees, setEmployees] = useState<Employee[]>([])
  const [candidates, setCandidates] = useState<Candidate[]>([])
  const [departments, setDepartments] = useState<string[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [filterDepartment, setFilterDepartment] = useState('')
  const [filterStatus, setFilterStatus] = useState('')
  
  // Modal states
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [showEditModal, setShowEditModal] = useState(false)
  const [showDeleteModal, setShowDeleteModal] = useState(false)
  const [showTerminateModal, setShowTerminateModal] = useState(false)
  const [selectedEmployee, setSelectedEmployee] = useState<Employee | null>(null)
  
  // Form data
  const [createFormData, setCreateFormData] = useState<CreateEmployeeFormData>({
    employeeId: '',
    candidateId: '',
    userId: '',
    department: '',
    position: '',
    salary: 0,
    employmentType: EmploymentType.FULL_TIME,
    hireDate: new Date().toISOString().split('T')[0],
    managerId: '',
  })
  
  const [editFormData, setEditFormData] = useState<UpdateEmployeeFormData>({
    department: '',
    position: '',
    salary: 0,
    employmentType: EmploymentType.FULL_TIME,
    managerId: '',
    status: EmployeeStatus.ACTIVE,
  })
  
  const [terminationDate, setTerminationDate] = useState('')

  // Fetch all employees
  const fetchEmployees = async () => {
    setLoading(true)
    setError(null)
    try {
      const result = await graphqlClient.request<{ allEmployees: Employee[] }>(ALL_EMPLOYEES)
      setEmployees(result.allEmployees)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load employees')
    } finally {
      setLoading(false)
    }
  }

  // Fetch candidates
  const fetchCandidates = async () => {
    try {
      const result = await graphqlClient.request<{ allCandidates: Candidate[] }>(GET_ALL_CANDIDATES)
      setCandidates(result.allCandidates)
    } catch (err) {
      console.error('Failed to load candidates:', err)
    }
  }

  // Fetch departments
  const fetchDepartments = async () => {
    try {
      const result = await graphqlClient.request<{ allDepartments: string[] }>(ALL_DEPARTMENTS)
      setDepartments(result.allDepartments)
    } catch (err) {
      console.error('Failed to load departments:', err)
    }
  }

  useEffect(() => {
    fetchEmployees()
    fetchCandidates()
    fetchDepartments()
  }, [])

  // Create employee
  const handleCreateEmployee = async (e: FormEvent) => {
    e.preventDefault()
    
    try {
      const input: any = {
        employeeId: createFormData.employeeId,
        candidateId: createFormData.candidateId,
        department: createFormData.department,
        position: createFormData.position,
        salary: createFormData.salary,
        employmentType: createFormData.employmentType,
        hireDate: createFormData.hireDate,
      }
      
      if (createFormData.userId) {
        input.userId = createFormData.userId
      }
      
      if (createFormData.managerId) {
        input.managerId = createFormData.managerId
      }
      
      await graphqlClient.request(CREATE_EMPLOYEE, { input })
      setShowCreateModal(false)
      setCreateFormData({
        employeeId: '',
        candidateId: '',
        userId: '',
        department: '',
        position: '',
        salary: 0,
        employmentType: EmploymentType.FULL_TIME,
        hireDate: new Date().toISOString().split('T')[0],
        managerId: '',
      })
      fetchEmployees()
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to create employee')
    }
  }

  // Update employee
  const handleUpdateEmployee = async (e: FormEvent) => {
    e.preventDefault()
    if (!selectedEmployee) return
    
    try {
      const input: any = {
        department: editFormData.department,
        position: editFormData.position,
        salary: editFormData.salary,
        employmentType: editFormData.employmentType,
        status: editFormData.status,
      }
      
      if (editFormData.managerId) {
        input.managerId = editFormData.managerId
      }
      
      await graphqlClient.request(UPDATE_EMPLOYEE, {
        id: selectedEmployee.id,
        input,
      })
      setShowEditModal(false)
      setSelectedEmployee(null)
      fetchEmployees()
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to update employee')
    }
  }

  // Terminate employee
  const handleTerminateEmployee = async (e: FormEvent) => {
    e.preventDefault()
    if (!selectedEmployee) return
    
    try {
      await graphqlClient.request(TERMINATE_EMPLOYEE, {
        id: selectedEmployee.id,
        terminationDate: terminationDate || null,
      })
      setShowTerminateModal(false)
      setSelectedEmployee(null)
      setTerminationDate('')
      fetchEmployees()
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to terminate employee')
    }
  }

  // Delete employee
  const handleDeleteEmployee = async () => {
    if (!selectedEmployee) return
    
    try {
      await graphqlClient.request(DELETE_EMPLOYEE, { id: selectedEmployee.id })
      setShowDeleteModal(false)
      setSelectedEmployee(null)
      fetchEmployees()
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to delete employee')
    }
  }

  // Open edit modal
  const openEditModal = (employee: Employee) => {
    setSelectedEmployee(employee)
    setEditFormData({
      department: employee.department,
      position: employee.position,
      salary: employee.salary,
      employmentType: employee.employmentType,
      managerId: employee.manager?.id || '',
      status: employee.status,
    })
    setShowEditModal(true)
  }

  // Open delete modal
  const openDeleteModal = (employee: Employee) => {
    setSelectedEmployee(employee)
    setShowDeleteModal(true)
  }

  // Open terminate modal
  const openTerminateModal = (employee: Employee) => {
    setSelectedEmployee(employee)
    setTerminationDate(new Date().toISOString().split('T')[0])
    setShowTerminateModal(true)
  }

  // Get employment type badge class
  const getEmploymentTypeBadgeClass = (type: EmploymentType) => {
    switch (type) {
      case EmploymentType.FULL_TIME:
        return styles.typeFullTime
      case EmploymentType.PART_TIME:
        return styles.typePartTime
      case EmploymentType.CONTRACT:
        return styles.typeContract
      case EmploymentType.INTERN:
        return styles.typeIntern
      default:
        return ''
    }
  }

  // Get status badge class
  const getStatusBadgeClass = (status: EmployeeStatus) => {
    switch (status) {
      case EmployeeStatus.ACTIVE:
        return styles.statusActive
      case EmployeeStatus.ON_LEAVE:
        return styles.statusOnLeave
      case EmployeeStatus.SUSPENDED:
        return styles.statusSuspended
      case EmployeeStatus.TERMINATED:
        return styles.statusTerminated
      default:
        return ''
    }
  }

  // Filter employees
  const filteredEmployees = employees.filter((emp) => {
    if (filterDepartment && emp.department !== filterDepartment) return false
    if (filterStatus && emp.status !== filterStatus) return false
    return true
  })

  if (loading && employees.length === 0) {
    return (
      <div className={styles.container}>
        <div className={styles.loading}>
          <div className={styles.spinner}></div>
          <p>Loading employees...</p>
        </div>
      </div>
    )
  }

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <h1>Employee Management</h1>
        <button onClick={() => setShowCreateModal(true)} className={styles.createButton}>
          + Add Employee
        </button>
      </div>

      {/* Filters */}
      <div className={styles.filters}>
        <select
          value={filterDepartment}
          onChange={(e) => setFilterDepartment(e.target.value)}
          className={styles.filterSelect}
        >
          <option value="">All Departments</option>
          {departments.map((dept) => (
            <option key={dept} value={dept}>
              {dept}
            </option>
          ))}
        </select>
        
        <select
          value={filterStatus}
          onChange={(e) => setFilterStatus(e.target.value)}
          className={styles.filterSelect}
        >
          <option value="">All Statuses</option>
          <option value={EmployeeStatus.ACTIVE}>Active</option>
          <option value={EmployeeStatus.ON_LEAVE}>On Leave</option>
          <option value={EmployeeStatus.SUSPENDED}>Suspended</option>
          <option value={EmployeeStatus.TERMINATED}>Terminated</option>
        </select>
        
        <div className={styles.employeeCount}>
          Showing {filteredEmployees.length} of {employees.length} employees
        </div>
      </div>

      {error && (
        <div className={styles.error}>
          <p>{error}</p>
          <button onClick={fetchEmployees} className={styles.retryButton}>
            Retry
          </button>
        </div>
      )}

      {/* Employees Table */}
      <div className={styles.tableContainer}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>Employee ID</th>
              <th>Name</th>
              <th>Department</th>
              <th>Position</th>
              <th>Type</th>
              <th>Salary</th>
              <th>Status</th>
              <th>Hire Date</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filteredEmployees.map((employee) => (
              <tr key={employee.id}>
                <td className={styles.employeeId}>{employee.employeeId}</td>
                <td>
                  {employee.user?.fullName || employee.candidate?.name || 'N/A'}
                </td>
                <td>{employee.department}</td>
                <td>{employee.position}</td>
                <td>
                  <span
                    className={`${styles.typeBadge} ${getEmploymentTypeBadgeClass(
                      employee.employmentType
                    )}`}
                  >
                    {employee.employmentType.replace('_', ' ')}
                  </span>
                </td>
                <td className={styles.salary}>
                  ${employee.salary.toLocaleString()}
                </td>
                <td>
                  <span
                    className={`${styles.statusBadge} ${getStatusBadgeClass(
                      employee.status
                    )}`}
                  >
                    {employee.status.replace('_', ' ')}
                  </span>
                </td>
                <td className={styles.timestamp}>
                  {new Date(employee.hireDate).toLocaleDateString()}
                </td>
                <td className={styles.actions}>
                  <button
                    onClick={() => openEditModal(employee)}
                    className={styles.actionButton}
                    title="Edit employee"
                  >
                    ✏️
                  </button>
                  {employee.status !== EmployeeStatus.TERMINATED && (
                    <button
                      onClick={() => openTerminateModal(employee)}
                      className={styles.actionButton}
                      title="Terminate employee"
                    >
                      🚫
                    </button>
                  )}
                  <button
                    onClick={() => openDeleteModal(employee)}
                    className={`${styles.actionButton} ${styles.deleteButton}`}
                    title="Delete employee"
                  >
                    🗑️
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Create Employee Modal */}
      {showCreateModal && (
        <div className={styles.modal} onClick={() => setShowCreateModal(false)}>
          <div className={styles.modalContent} onClick={(e) => e.stopPropagation()}>
            <h2>Add New Employee</h2>
            <form onSubmit={handleCreateEmployee}>
              <div className={styles.formGroup}>
                <label>Employee ID *</label>
                <input
                  type="text"
                  required
                  value={createFormData.employeeId}
                  onChange={(e) =>
                    setCreateFormData({ ...createFormData, employeeId: e.target.value })
                  }
                  placeholder="EMP-001"
                />
              </div>
              <div className={styles.formGroup}>
                <label>Candidate *</label>
                <select
                  required
                  value={createFormData.candidateId}
                  onChange={(e) =>
                    setCreateFormData({ ...createFormData, candidateId: e.target.value })
                  }
                >
                  <option value="">Select Candidate</option>
                  {candidates.map((candidate) => (
                    <option key={candidate.id} value={candidate.id}>
                      {candidate.name} ({candidate.email})
                    </option>
                  ))}
                </select>
              </div>
              <div className={styles.formGroup}>
                <label>Department *</label>
                <input
                  type="text"
                  required
                  value={createFormData.department}
                  onChange={(e) =>
                    setCreateFormData({ ...createFormData, department: e.target.value })
                  }
                  placeholder="Engineering, HR, Sales..."
                />
              </div>
              <div className={styles.formGroup}>
                <label>Position *</label>
                <input
                  type="text"
                  required
                  value={createFormData.position}
                  onChange={(e) =>
                    setCreateFormData({ ...createFormData, position: e.target.value })
                  }
                  placeholder="Software Engineer, Manager..."
                />
              </div>
              <div className={styles.formRow}>
                <div className={styles.formGroup}>
                  <label>Salary *</label>
                  <input
                    type="number"
                    required
                    min="0"
                    step="1000"
                    value={createFormData.salary}
                    onChange={(e) =>
                      setCreateFormData({
                        ...createFormData,
                        salary: parseFloat(e.target.value),
                      })
                    }
                  />
                </div>
                <div className={styles.formGroup}>
                  <label>Employment Type *</label>
                  <select
                    required
                    value={createFormData.employmentType}
                    onChange={(e) =>
                      setCreateFormData({
                        ...createFormData,
                        employmentType: e.target.value as EmploymentType,
                      })
                    }
                  >
                    <option value={EmploymentType.FULL_TIME}>Full Time</option>
                    <option value={EmploymentType.PART_TIME}>Part Time</option>
                    <option value={EmploymentType.CONTRACT}>Contract</option>
                    <option value={EmploymentType.INTERN}>Intern</option>
                  </select>
                </div>
              </div>
              <div className={styles.formGroup}>
                <label>Hire Date *</label>
                <input
                  type="date"
                  required
                  value={createFormData.hireDate}
                  onChange={(e) =>
                    setCreateFormData({ ...createFormData, hireDate: e.target.value })
                  }
                />
              </div>
              <div className={styles.modalActions}>
                <button type="button" onClick={() => setShowCreateModal(false)}>
                  Cancel
                </button>
                <button type="submit" className={styles.submitButton}>
                  Add Employee
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Edit Employee Modal */}
      {showEditModal && selectedEmployee && (
        <div className={styles.modal} onClick={() => setShowEditModal(false)}>
          <div className={styles.modalContent} onClick={(e) => e.stopPropagation()}>
            <h2>Edit Employee: {selectedEmployee.employeeId}</h2>
            <form onSubmit={handleUpdateEmployee}>
              <div className={styles.formGroup}>
                <label>Department *</label>
                <input
                  type="text"
                  required
                  value={editFormData.department}
                  onChange={(e) =>
                    setEditFormData({ ...editFormData, department: e.target.value })
                  }
                />
              </div>
              <div className={styles.formGroup}>
                <label>Position *</label>
                <input
                  type="text"
                  required
                  value={editFormData.position}
                  onChange={(e) =>
                    setEditFormData({ ...editFormData, position: e.target.value })
                  }
                />
              </div>
              <div className={styles.formRow}>
                <div className={styles.formGroup}>
                  <label>Salary *</label>
                  <input
                    type="number"
                    required
                    min="0"
                    step="1000"
                    value={editFormData.salary}
                    onChange={(e) =>
                      setEditFormData({
                        ...editFormData,
                        salary: parseFloat(e.target.value),
                      })
                    }
                  />
                </div>
                <div className={styles.formGroup}>
                  <label>Employment Type *</label>
                  <select
                    required
                    value={editFormData.employmentType}
                    onChange={(e) =>
                      setEditFormData({
                        ...editFormData,
                        employmentType: e.target.value as EmploymentType,
                      })
                    }
                  >
                    <option value={EmploymentType.FULL_TIME}>Full Time</option>
                    <option value={EmploymentType.PART_TIME}>Part Time</option>
                    <option value={EmploymentType.CONTRACT}>Contract</option>
                    <option value={EmploymentType.INTERN}>Intern</option>
                  </select>
                </div>
              </div>
              <div className={styles.formGroup}>
                <label>Status *</label>
                <select
                  required
                  value={editFormData.status}
                  onChange={(e) =>
                    setEditFormData({
                      ...editFormData,
                      status: e.target.value as EmployeeStatus,
                    })
                  }
                >
                  <option value={EmployeeStatus.ACTIVE}>Active</option>
                  <option value={EmployeeStatus.ON_LEAVE}>On Leave</option>
                  <option value={EmployeeStatus.SUSPENDED}>Suspended</option>
                  <option value={EmployeeStatus.TERMINATED}>Terminated</option>
                </select>
              </div>
              <div className={styles.modalActions}>
                <button type="button" onClick={() => setShowEditModal(false)}>
                  Cancel
                </button>
                <button type="submit" className={styles.submitButton}>
                  Update Employee
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Terminate Employee Modal */}
      {showTerminateModal && selectedEmployee && (
        <div className={styles.modal} onClick={() => setShowTerminateModal(false)}>
          <div className={styles.modalContent} onClick={(e) => e.stopPropagation()}>
            <h2>Terminate Employee</h2>
            <p>
              Terminate employee <strong>{selectedEmployee.employeeId}</strong> (
              {selectedEmployee.user?.fullName || selectedEmployee.candidate?.name})?
            </p>
            <form onSubmit={handleTerminateEmployee}>
              <div className={styles.formGroup}>
                <label>Termination Date</label>
                <input
                  type="date"
                  value={terminationDate}
                  onChange={(e) => setTerminationDate(e.target.value)}
                />
              </div>
              <div className={styles.modalActions}>
                <button type="button" onClick={() => setShowTerminateModal(false)}>
                  Cancel
                </button>
                <button type="submit" className={styles.deleteConfirmButton}>
                  Terminate Employee
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {showDeleteModal && selectedEmployee && (
        <div className={styles.modal} onClick={() => setShowDeleteModal(false)}>
          <div className={styles.modalContent} onClick={(e) => e.stopPropagation()}>
            <h2>Delete Employee Record</h2>
            <p>
              Are you sure you want to delete employee{' '}
              <strong>{selectedEmployee.employeeId}</strong> (
              {selectedEmployee.user?.fullName || selectedEmployee.candidate?.name})?
            </p>
            <p className={styles.warningText}>This action cannot be undone.</p>
            <div className={styles.modalActions}>
              <button type="button" onClick={() => setShowDeleteModal(false)}>
                Cancel
              </button>
              <button
                type="button"
                onClick={handleDeleteEmployee}
                className={styles.deleteConfirmButton}
              >
                Delete Employee
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
