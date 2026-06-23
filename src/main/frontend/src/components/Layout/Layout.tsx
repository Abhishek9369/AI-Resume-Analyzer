import { useState } from 'react'
import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import { logout } from '@store/slices/authSlice'
import {
  selectUser,
  selectUserFullName,
  selectUserRole,
  selectIsAdmin,
  selectCanManageEmployees,
  selectCanUploadResumes,
  selectCanManageJobs,
} from '@store/selectors/authSelectors'
import {
  LayoutDashboard,
  ShieldCheck,
  Users,
  Building2,
  Upload,
  UserCircle,
  Briefcase,
  Target,
  Wrench,
  LogOut,
  ChevronLeft,
  ChevronRight,
  Menu,
} from 'lucide-react'
import styles from './Layout.module.css'

const SIDEBAR_KEY = 'sidebarCollapsed'

const Layout = () => {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const user = useSelector(selectUser)
  const fullName = useSelector(selectUserFullName)
  const userRole = useSelector(selectUserRole)
  const isAdmin = useSelector(selectIsAdmin)
  const canManageEmployees = useSelector(selectCanManageEmployees)
  const canUploadResumes = useSelector(selectCanUploadResumes)
  const canManageJobs = useSelector(selectCanManageJobs)

  const [collapsed, setCollapsed] = useState<boolean>(() => {
    try {
      return localStorage.getItem(SIDEBAR_KEY) === 'true'
    } catch {
      return false
    }
  })

  const toggleSidebar = () => {
    setCollapsed((prev) => {
      const next = !prev
      try { localStorage.setItem(SIDEBAR_KEY, String(next)) } catch { /* noop */ }
      return next
    })
  }

  const handleLogout = () => {
    dispatch(logout())
    navigate('/login')
  }

  const navLinkClass = ({ isActive }: { isActive: boolean }) =>
    isActive
      ? `${styles.navItem} ${styles.navItemActive}`
      : styles.navItem

  return (
    <div className={`${styles.layout} ${collapsed ? styles.layoutCollapsed : ''}`}>

      {/* ── Sidebar ─────────────────────────────────────────────── */}
      <aside className={`${styles.sidebar} ${collapsed ? styles.sidebarCollapsed : ''}`}>

        {/* Sidebar header */}
        <div className={styles.sidebarHeader}>
          {!collapsed && (
            <span className={styles.brandName}>🎯 Resume Analyzer</span>
          )}
          <button
            className={styles.toggleBtn}
            onClick={toggleSidebar}
            title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
            aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          >
            {collapsed ? <ChevronRight size={20} /> : <ChevronLeft size={20} />}
          </button>
        </div>

        {/* User info */}
        {user && (
          <div className={`${styles.userInfo} ${collapsed ? styles.userInfoCollapsed : ''}`}>
            <div className={styles.userAvatar}>
              {fullName ? fullName.charAt(0).toUpperCase() : 'U'}
            </div>
            {!collapsed && (
              <div className={styles.userDetails}>
                <span className={styles.userName}>{fullName}</span>
                <span className={styles.userRole}>{userRole}</span>
              </div>
            )}
          </div>
        )}

        {/* Navigation */}
        <nav className={styles.nav}>
          <NavLink to="/" className={navLinkClass} end title={collapsed ? 'Dashboard' : undefined}>
            <LayoutDashboard size={20} className={styles.navIcon} />
            {!collapsed && <span>Dashboard</span>}
          </NavLink>

          {isAdmin && (
            <NavLink to="/admin" className={navLinkClass} title={collapsed ? 'Admin Dashboard' : undefined}>
              <ShieldCheck size={20} className={styles.navIcon} />
              {!collapsed && <span>Admin Dashboard</span>}
            </NavLink>
          )}

          {isAdmin && (
            <NavLink to="/users" className={navLinkClass} title={collapsed ? 'User Management' : undefined}>
              <Users size={20} className={styles.navIcon} />
              {!collapsed && <span>User Management</span>}
            </NavLink>
          )}

          {canManageEmployees && (
            <NavLink to="/employees" className={navLinkClass} title={collapsed ? 'Employee Management' : undefined}>
              <Building2 size={20} className={styles.navIcon} />
              {!collapsed && <span>Employees</span>}
            </NavLink>
          )}

          {canUploadResumes && (
            <NavLink to="/upload" className={navLinkClass} title={collapsed ? 'Upload Resumes' : undefined}>
              <Upload size={20} className={styles.navIcon} />
              {!collapsed && <span>Upload Resumes</span>}
            </NavLink>
          )}

          <NavLink to="/candidates" className={navLinkClass} title={collapsed ? 'Candidates' : undefined}>
            <UserCircle size={20} className={styles.navIcon} />
            {!collapsed && <span>Candidates</span>}
          </NavLink>

          {canManageJobs && (
            <NavLink to="/jobs" className={navLinkClass} title={collapsed ? 'Job Requirements' : undefined}>
              <Briefcase size={20} className={styles.navIcon} />
              {!collapsed && <span>Job Requirements</span>}
            </NavLink>
          )}

          <NavLink to="/matching" className={navLinkClass} title={collapsed ? 'Candidate Matching' : undefined}>
            <Target size={20} className={styles.navIcon} />
            {!collapsed && <span>Candidate Matching</span>}
          </NavLink>

          {isAdmin && (
            <NavLink to="/skills" className={navLinkClass} title={collapsed ? 'Skills Master' : undefined}>
              <Wrench size={20} className={styles.navIcon} />
              {!collapsed && <span>Skills Master</span>}
            </NavLink>
          )}
        </nav>

        {/* Logout at bottom */}
        <div className={styles.sidebarFooter}>
          <button
            onClick={handleLogout}
            className={styles.logoutBtn}
            title={collapsed ? 'Logout' : undefined}
          >
            <LogOut size={20} className={styles.navIcon} />
            {!collapsed && <span>Logout</span>}
          </button>
        </div>
      </aside>

      {/* ── Main area ────────────────────────────────────────────── */}
      <div className={styles.mainWrapper}>
        {/* Top bar (mobile toggle + breadcrumb placeholder) */}
        <header className={styles.topBar}>
          <button
            className={styles.mobileToggle}
            onClick={toggleSidebar}
            aria-label="Toggle sidebar"
          >
            <Menu size={22} />
          </button>
          {user && (
            <div className={styles.topBarRight}>
              <span className={styles.topBarUser}>{fullName}</span>
              <span className={styles.topBarRole}>{userRole}</span>
            </div>
          )}
        </header>

        <main className={styles.main}>
          <Outlet />
        </main>

        <footer className={styles.footer}>
          <p>Resume Analyzer — AI-Powered Candidate Matching © 2025</p>
        </footer>
      </div>
    </div>
  )
}

export default Layout

