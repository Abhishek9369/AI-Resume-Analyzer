import { Routes, Route } from 'react-router-dom'
import ProtectedRoute from './components/ProtectedRoute/ProtectedRoute'
import RoleBasedRoute from './components/RoleBasedRoute/RoleBasedRoute'
import Layout from './components/Layout/Layout'
import Login from './pages/Login/Login'
import Unauthorized from './pages/Unauthorized/Unauthorized'
import Dashboard from './pages/Dashboard/Dashboard'
import AdminDashboard from './pages/AdminDashboard/AdminDashboard'
import UserManagement from './pages/UserManagement/UserManagement'
import EmployeeManagement from './pages/EmployeeManagement/EmployeeManagement'
import CandidateList from './pages/CandidateList/CandidateList'
import JobRequirements from './pages/JobRequirements/JobRequirements'
import CandidateMatching from './pages/CandidateMatching/CandidateMatching'
import FileUpload from './pages/FileUpload/FileUpload'
import SkillsManager from './pages/SkillsManager/SkillsManager'
import { UserRole } from './types/auth'

function App() {
  return (
    <Routes>
      {/* Public routes */}
      <Route path="/login" element={<Login />} />
      <Route path="/unauthorized" element={<Unauthorized />} />

      {/* Protected routes */}
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Dashboard />} />
        
        {/* Admin Dashboard - Admin only */}
        <Route
          path="admin"
          element={
            <RoleBasedRoute allowedRoles={[UserRole.ADMIN]}>
              <AdminDashboard />
            </RoleBasedRoute>
          }
        />
        
        {/* User Management - Admin only */}
        <Route
          path="users"
          element={
            <RoleBasedRoute allowedRoles={[UserRole.ADMIN]}>
              <UserManagement />
            </RoleBasedRoute>
          }
        />
        
        {/* Employee Management - Admin and HR */}
        <Route
          path="employees"
          element={
            <RoleBasedRoute allowedRoles={[UserRole.ADMIN, UserRole.HR]}>
              <EmployeeManagement />
            </RoleBasedRoute>
          }
        />
        
        {/* Upload resumes - Admin and Recruiter only */}
        <Route
          path="upload"
          element={
            <RoleBasedRoute allowedRoles={[UserRole.ADMIN, UserRole.RECRUITER]}>
              <FileUpload />
            </RoleBasedRoute>
          }
        />
        
        {/* Candidates - All authenticated users */}
        <Route path="candidates" element={<CandidateList />} />
        
        {/* Jobs - Admin, Recruiter, and Hiring Manager */}
        <Route
          path="jobs"
          element={
            <RoleBasedRoute
              allowedRoles={[
                UserRole.ADMIN,
                UserRole.RECRUITER,
                UserRole.HIRING_MANAGER,
              ]}
            >
              <JobRequirements />
            </RoleBasedRoute>
          }
        />
        
        {/* Matching - All authenticated users */}
        <Route path="matching" element={<CandidateMatching />} />
        
        {/* Skills - Admin only */}
        <Route
          path="skills"
          element={
            <RoleBasedRoute allowedRoles={[UserRole.ADMIN]}>
              <SkillsManager />
            </RoleBasedRoute>
          }
        />
      </Route>
    </Routes>
  )
}

export default App
