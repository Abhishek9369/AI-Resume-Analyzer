import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import { fetchCandidates } from '@/store/slices/candidatesSlice'
import { fetchJobs } from '@/store/slices/jobsSlice'
import { RootState } from '@/store'
import styles from './Dashboard.module.css'

const Dashboard = () => {
  const navigate = useNavigate()
  const dispatch = useDispatch()
  const candidates = useSelector((state: RootState) => state.candidates.candidates)
  const jobs = useSelector((state: RootState) => state.jobs.jobs)

  useEffect(() => {
    dispatch(fetchCandidates())
    dispatch(fetchJobs())
  }, [dispatch])

  const activeJobs = jobs.filter((job) => job.isActive).length

  return (
    <div className={styles.dashboard}>
      <h2>Dashboard</h2>

      <div className={styles.stats}>
        <div className={styles.statCard}>
          <h3>Total Candidates</h3>
          <p>{candidates.length}</p>
        </div>
        <div className={styles.statCard}>
          <h3>Active Jobs</h3>
          <p>{activeJobs}</p>
        </div>
        <div className={styles.statCard}>
          <h3>Total Jobs</h3>
          <p>{jobs.length}</p>
        </div>
      </div>

      <h2>Quick Actions</h2>
      <div className={styles.quickActions}>
        <div className={styles.actionCard}>
          <h3>📤 Upload Resumes</h3>
          <p>Upload single or multiple resumes for AI-powered analysis and candidate extraction.</p>
          <button className={styles.actionButton} onClick={() => navigate('/upload')}>
            Upload Now
          </button>
        </div>
        <div className={styles.actionCard}>
          <h3>👔 Create Job Posting</h3>
          <p>Create a new job requirement to match against existing candidates.</p>
          <button className={styles.actionButton} onClick={() => navigate('/jobs')}>
            Create Job
          </button>
        </div>
        <div className={styles.actionCard}>
          <h3>🎯 Match Candidates</h3>
          <p>Use AI to match candidates against job requirements with detailed scoring.</p>
          <button className={styles.actionButton} onClick={() => navigate('/matching')}>
            Start Matching
          </button>
        </div>
      </div>
    </div>
  )
}

export default Dashboard
