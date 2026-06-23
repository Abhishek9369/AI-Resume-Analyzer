import { useDispatch } from 'react-redux'
import { fetchProcessStatus } from '@/store/slices/uploadSlice'
import type { ProcessTracker } from '@/store/slices/uploadSlice'
import styles from './ProcessTrackerTable.module.css'

interface ProcessTrackerTableProps {
  trackers: ProcessTracker[]
  onRefresh: () => void
}

const ProcessTrackerTable = ({ trackers, onRefresh }: ProcessTrackerTableProps) => {
  const dispatch = useDispatch()

  const handleRefreshTracker = (trackerId: string) => {
    dispatch(fetchProcessStatus(trackerId))
  }

  const getStatusClass = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return styles.statusCompleted
      case 'FAILED':
        return styles.statusFailed
      case 'INITIATED':
        return styles.statusInitiated
      default:
        return styles.statusProcessing
    }
  }

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString()
  }

  const getProgress = (tracker: ProcessTracker) => {
    if (tracker.totalFiles === 0) return 0
    return Math.round((tracker.processedFiles / tracker.totalFiles) * 100)
  }

  return (
    <div className={styles.trackerTable}>
      <div className={styles.header}>
        <h3>Upload History</h3>
        <button onClick={onRefresh} className={styles.refreshAllButton}>
          🔄 Refresh All
        </button>
      </div>
      
      {trackers.length === 0 ? (
        <p className={styles.emptyState}>No recent uploads found</p>
      ) : (
        <div className={styles.tableWrapper}>
          <table>
            <thead>
              <tr>
                <th>Status</th>
                <th>Files</th>
                <th>Progress</th>
                <th>Started</th>
                <th>Completed</th>
                <th>Message</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {trackers.map((tracker) => (
                <tr key={tracker.id}>
                  <td>
                    <span className={`${styles.statusBadge} ${getStatusClass(tracker.status)}`}>
                      {tracker.status.replace(/_/g, ' ')}
                    </span>
                  </td>
                  <td>
                    <div className={styles.filesInfo}>
                      <span className={styles.fileCount}>{tracker.totalFiles} total</span>
                      {tracker.failedFiles > 0 && (
                        <span className={styles.failedCount}>{tracker.failedFiles} failed</span>
                      )}
                    </div>
                  </td>
                  <td>
                    <div className={styles.progressCell}>
                      <div className={styles.progressBar}>
                        <div 
                          className={styles.progressFill} 
                          style={{ width: `${getProgress(tracker)}%` }}
                        />
                      </div>
                      <span className={styles.progressText}>
                        {tracker.processedFiles}/{tracker.totalFiles}
                      </span>
                    </div>
                  </td>
                  <td className={styles.dateCell}>
                    {formatDate(tracker.createdAt || tracker.startTime || '')}
                  </td>
                  <td className={styles.dateCell}>
                    {tracker.completedAt ? formatDate(tracker.completedAt) : tracker.endTime ? formatDate(tracker.endTime) : '-'}
                  </td>
                  <td className={styles.messageCell}>
                    {tracker.message || '-'}
                  </td>
                  <td>
                    <button
                      onClick={() => handleRefreshTracker(tracker.id)}
                      className={styles.refreshButton}
                      disabled={tracker.status === 'COMPLETED' || tracker.status === 'FAILED'}
                      title="Refresh status"
                    >
                      🔄
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

export default ProcessTrackerTable
