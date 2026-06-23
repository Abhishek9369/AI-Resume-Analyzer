/**
 * Role-Based Route Component
 * Redirects to unauthorized page if user doesn't have required role
 */

import { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useSelector } from 'react-redux'
import {
  selectIsAuthenticated,
  selectUserRole,
} from '@store/selectors/authSelectors'
import { UserRole } from '@/types/auth'

interface RoleBasedRouteProps {
  children: ReactNode
  allowedRoles: UserRole[]
}

export default function RoleBasedRoute({
  children,
  allowedRoles,
}: RoleBasedRouteProps) {
  const isAuthenticated = useSelector(selectIsAuthenticated)
  const userRole = useSelector(selectUserRole)

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  if (!userRole || !allowedRoles.includes(userRole)) {
    return <Navigate to="/unauthorized" replace />
  }

  return <>{children}</>
}
