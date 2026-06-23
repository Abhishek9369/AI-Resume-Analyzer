/**
 * Unauthorized Page
 * Displayed when user tries to access a route they don't have permission for
 */

import { useNavigate } from 'react-router-dom'
import { useSelector } from 'react-redux'
import { selectUserRole, selectUserFullName } from '@store/selectors/authSelectors'
import styles from './Unauthorized.module.css'

export default function Unauthorized() {
  const navigate = useNavigate()
  const userRole = useSelector(selectUserRole)
  const fullName = useSelector(selectUserFullName)

  return (
    <div className={styles.container}>
      <div className={styles.content}>
        <div className={styles.iconContainer}>
          <svg
            className={styles.icon}
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"
            />
          </svg>
        </div>
        <h1 className={styles.title}>Access Denied</h1>
        <p className={styles.message}>
          Sorry, {fullName || 'User'}, you don't have permission to access this page.
        </p>
        <p className={styles.roleInfo}>
          Your current role: <strong>{userRole}</strong>
        </p>
        <div className={styles.actions}>
          <button
            onClick={() => navigate(-1)}
            className={styles.backButton}
          >
            Go Back
          </button>
          <button
            onClick={() => navigate('/')}
            className={styles.homeButton}
          >
            Go to Dashboard
          </button>
        </div>
      </div>
    </div>
  )
}
