/**
 * GraphQL queries for Employee Management
 */

/**
 * Query all employees
 */
export const ALL_EMPLOYEES = `
  query {
    allEmployees {
      id
      employeeId
      candidate {
        id
        name
        email
      }
      user {
        id
        username
        fullName
      }
      department
      position
      salary
      employmentType
      hireDate
      manager {
        id
        employeeId
        user {
          fullName
        }
      }
      status
      terminationDate
      createdAt
      updatedAt
    }
  }
`

/**
 * Query employees by department
 */
export const EMPLOYEES_BY_DEPARTMENT = `
  query EmployeesByDepartment($department: String!) {
    employeesByDepartment(department: $department) {
      id
      employeeId
      candidate {
        name
      }
      user {
        fullName
      }
      department
      position
      employmentType
      status
    }
  }
`

/**
 * Query employees by status
 */
export const EMPLOYEES_BY_STATUS = `
  query EmployeesByStatus($status: EmployeeStatus!) {
    employeesByStatus(status: $status) {
      id
      employeeId
      candidate {
        name
      }
      user {
        fullName
      }
      department
      position
      employmentType
      status
    }
  }
`

/**
 * Query manager subordinates
 */
export const MANAGER_SUBORDINATES = `
  query ManagerSubordinates($managerId: UUID!) {
    managerSubordinates(managerId: $managerId) {
      id
      employeeId
      candidate {
        name
      }
      user {
        fullName
      }
      department
      position
    }
  }
`

/**
 * Query all departments
 */
export const ALL_DEPARTMENTS = `
  query {
    allDepartments
  }
`

/**
 * Query employee by ID
 */
export const GET_EMPLOYEE = `
  query GetEmployee($id: UUID!) {
    employee(id: $id) {
      id
      employeeId
      candidate {
        id
        name
        email
      }
      user {
        id
        username
        fullName
      }
      department
      position
      salary
      employmentType
      hireDate
      manager {
        id
        employeeId
        user {
          fullName
        }
      }
      status
      terminationDate
      createdAt
      updatedAt
    }
  }
`

/**
 * Create employee mutation
 */
export const CREATE_EMPLOYEE = `
  mutation CreateEmployee($input: EmployeeInput!) {
    createEmployee(input: $input) {
      id
      employeeId
      department
      position
      salary
      employmentType
      hireDate
      status
    }
  }
`

/**
 * Update employee mutation
 */
export const UPDATE_EMPLOYEE = `
  mutation UpdateEmployee($id: UUID!, $input: UpdateEmployeeInput!) {
    updateEmployee(id: $id, input: $input) {
      id
      employeeId
      department
      position
      salary
      employmentType
      status
      updatedAt
    }
  }
`

/**
 * Terminate employee mutation
 */
export const TERMINATE_EMPLOYEE = `
  mutation TerminateEmployee($id: UUID!, $terminationDate: LocalDateTime) {
    terminateEmployee(id: $id, terminationDate: $terminationDate) {
      id
      employeeId
      status
      terminationDate
    }
  }
`

/**
 * Delete employee mutation
 */
export const DELETE_EMPLOYEE = `
  mutation DeleteEmployee($id: UUID!) {
    deleteEmployee(id: $id)
  }
`
