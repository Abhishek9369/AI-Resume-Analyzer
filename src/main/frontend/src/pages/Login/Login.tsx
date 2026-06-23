/**
 * Login Page Component
 * Handles user authentication with username/password
 */

import { useState, useEffect, FormEvent } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { useNavigate, useLocation } from 'react-router-dom'
import { loginRequest, clearError } from '@store/slices/authSlice'
import {
  selectIsAuthenticated,
  selectIsLoading,
  selectAuthError,
} from '@store/selectors/authSelectors'
import styles from './Login.module.css'

interface LocationState {
  from?: {
    pathname: string
  }
}

export default function Login() {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const location = useLocation()
  const state = location.state as LocationState

  const isAuthenticated = useSelector(selectIsAuthenticated)
  const isLoading = useSelector(selectIsLoading)
  const error = useSelector(selectAuthError)

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')

  // Redirect to intended destination after successful login
  useEffect(() => {
    if (isAuthenticated) {
      const from = state?.from?.pathname || '/'
      navigate(from, { replace: true })
    }
  }, [isAuthenticated, navigate, state])

  // Clear error when component unmounts
  useEffect(() => {
    return () => {
      dispatch(clearError())
    }
  }, [dispatch])

  const handleSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()

    if (!username.trim() || !password.trim()) {
      return
    }

    dispatch(loginRequest({ username, password }))
  }

  return (
    <div className={styles.loginContainer}>
      <div className={styles.loginBox}>
        <div className={styles.loginHeader}>
          <h1>Resume Analyzer</h1>
          <p>Sign in to your account</p>
        </div>

        <form onSubmit={handleSubmit} className={styles.loginForm}>
          {error && (
            <div className={styles.errorMessage} role="alert">
              {error}
            </div>
          )}

          <div className={styles.formGroup}>
            <label htmlFor="username">Username</label>
            <input
              id="username"
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="Enter your username"
              disabled={isLoading}
              required
              autoComplete="username"
              autoFocus
            />
          </div>

          <div className={styles.formGroup}>
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Enter your password"
              disabled={isLoading}
              required
              autoComplete="current-password"
            />
          </div>

          <button
            type="submit"
            className={styles.loginButton}
            disabled={isLoading || !username.trim() || !password.trim()}
          >
            {isLoading ? 'Signing in...' : 'Sign In'}
          </button>
        </form>

        <div className={styles.loginFooter}>
          <p className={styles.testAccounts}>Test Accounts:</p>
          <ul className={styles.accountList}>
            <li><strong>Admin:</strong> admin / Admin@123</li>
            <li><strong>Recruiter:</strong> recruiter / Recruiter@123</li>
            <li><strong>HR:</strong> hr / HR@123</li>
            <li><strong>Manager:</strong> hiring_manager / Manager@123</li>
          </ul>
        </div>
      </div>
    </div>
  )
}
