/**
 * GraphQL queries for Admin Dashboard
 */

/**
 * Query system health report
 */
export const SYSTEM_HEALTH_REPORT = `
  query {
    systemHealthReport {
      id
      serviceName
      status
      responseTimeMs
      message
      lastCheckedAt
      lastSuccessAt
      lastFailureAt
      failureCount
    }
    overallSystemStatus
  }
`

/**
 * Query user statistics
 */
export const USER_STATISTICS = `
  query {
    userStatistics {
      total
      active
      admins
      recruiters
      hr
      hiringManagers
    }
  }
`

/**
 * Query employee statistics
 */
export const EMPLOYEE_STATISTICS = `
  query {
    employeeStatistics {
      total
      active
      onLeave
      suspended
      terminated
      byDepartment {
        department
        count
      }
      byEmploymentType {
        employmentType
        count
      }
    }
  }
`

/**
 * Combined admin dashboard query
 */
export const ADMIN_DASHBOARD_DATA = `
  query {
    systemHealthReport {
      id
      serviceName
      status
      responseTimeMs
      message
      lastCheckedAt
      lastSuccessAt
      lastFailureAt
      failureCount
    }
    overallSystemStatus
    userStatistics {
      total
      active
      admins
      recruiters
      hr
      hiringManagers
    }
    employeeStatistics {
      total
      active
      onLeave
      suspended
      terminated
      byDepartment {
        department
        count
      }
      byEmploymentType {
        employmentType
        count
      }
    }
  }
`

/**
 * Single combined query — fetches all AdminDashboard data in one round trip.
 * Replaces the separate ADMIN_DASHBOARD_DATA + MATCH_AUDITS_QUERY calls to
 * eliminate the flicker / double-render that occurred when two requests
 * completed at different times.
 */
export const ADMIN_DASHBOARD_ALL = `
  query AdminDashboardAll {
    systemHealthReport {
      id
      serviceName
      status
      responseTimeMs
      message
      lastCheckedAt
      lastSuccessAt
      lastFailureAt
      failureCount
    }
    overallSystemStatus
    userStatistics {
      total
      active
      admins
      recruiters
      hr
      hiringManagers
    }
    employeeStatistics {
      total
      active
      onLeave
      suspended
      terminated
      byDepartment {
        department
        count
      }
      byEmploymentType {
        employmentType
        count
      }
    }
    matchAudits(limit: 30) {
      id
      jobRequirementId
      jobTitle
      totalCandidates
      successfulMatches
      shortlistedCount
      averageMatchScore
      highestMatchScore
      durationMs
      estimatedTokensUsed
      status
      initiatedBy
      errorMessage
      initiatedAt
      completedAt
    }
  }
`

/**
 * Query match audits for the admin panel
 */
export const MATCH_AUDITS_QUERY = `
  query {
    matchAudits(limit: 30) {
      id
      jobRequirementId
      jobTitle
      totalCandidates
      successfulMatches
      shortlistedCount
      averageMatchScore
      highestMatchScore
      durationMs
      estimatedTokensUsed
      status
      initiatedBy
      errorMessage
      initiatedAt
      completedAt
    }
  }
`

/**
 * Query active (in-progress) match runs
 */
export const ACTIVE_MATCH_RUNS_QUERY = `
  query {
    activeMatchRuns {
      id
      jobTitle
      initiatedBy
      initiatedAt
      status
    }
  }
`
