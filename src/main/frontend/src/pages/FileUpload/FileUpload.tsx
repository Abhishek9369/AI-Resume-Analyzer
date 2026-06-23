import { useState, useRef, useEffect } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { uploadFiles, fetchProcessStatus, clearTracker, fetchRecentTrackers } from '@/store/slices/uploadSlice'
import { RootState } from '@/store'
import ProcessTrackerTable from '@/components/ProcessTrackerTable/ProcessTrackerTable'
import styles from './FileUpload.module.css'

const FileUpload = () => {
  const dispatch = useDispatch()
  const { uploading, tracker, trackers, error } = useSelector((state: RootState) => state.upload)
  const [files, setFiles] = useState<File[]>([])
  const [dragging, setDragging] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const pollIntervalRef = useRef<NodeJS.Timeout>()

  // Fetch recent trackers on mount
  useEffect(() => {
    dispatch(fetchRecentTrackers(24)) // Fetch trackers from last 24 hours
  }, [dispatch])

  // Poll tracker status if processing
  useEffect(() => {
    if (tracker && tracker.status !== 'COMPLETED' && tracker.status !== 'FAILED') {
      pollIntervalRef.current = setInterval(() => {
        dispatch(fetchProcessStatus(tracker.id))
      }, 2000)
    }
    return () => {
      if (pollIntervalRef.current) {
        clearInterval(pollIntervalRef.current)
      }
    }
  }, [tracker, dispatch])

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault()
    setDragging(true)
  }

  const handleDragLeave = () => {
    setDragging(false)
  }

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault()
    setDragging(false)
    const droppedFiles = Array.from(e.dataTransfer.files).filter((file) =>
      file.name.match(/\.(pdf|doc|docx|zip)$/i)
    )
    setFiles((prev) => [...prev, ...droppedFiles])
  }

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      const selectedFiles = Array.from(e.target.files)
      setFiles((prev) => [...prev, ...selectedFiles])
    }
  }

  const removeFile = (index: number) => {
    setFiles((prev) => prev.filter((_, i) => i !== index))
  }

  const handleUpload = () => {
    if (files.length > 0) {
      dispatch(uploadFiles(files))
      setFiles([])
    }
  }

  const handleRefreshAll = () => {
    dispatch(fetchRecentTrackers(24))
  }

  const handleNewUpload = () => {
    dispatch(clearTracker())
  }

  const formatBytes = (bytes: number) => {
    if (bytes === 0) return '0 Bytes'
    const k = 1024
    const sizes = ['Bytes', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
  }

  const getStatusClass = (status: string) => {
    switch (status) {
      case 'INITIATED':
        return styles.initiated
      case 'EMBED_GENERATED':
      case 'VECTOR_DB_UPDATED':
      case 'RESUME_ANALYZED':
        return styles.processing
      case 'COMPLETED':
        return styles.completed
      case 'FAILED':
        return styles.failed
      default:
        return styles.initiated
    }
  }

  return (
    <div className={styles.upload}>
      <h2>Upload Resumes</h2>

      {/* Show current processing status if a tracker exists and is active */}
      {tracker && (
        <div className={styles.currentTracker}>
          <h3>Current Upload Status</h3>
          <div className={styles.tracker}>
            <div className={styles.progressInfo}>
              <span className={`${styles.status} ${getStatusClass(tracker.status)}`}>
                {tracker.status.replace(/_/g, ' ')}
              </span>
              <div className={styles.progressBar}>
                <div className={styles.progressFill} style={{ 
                  width: `${tracker.totalFiles > 0 ? Math.round((tracker.processedFiles / tracker.totalFiles) * 100) : 0}%` 
                }}>
                  {tracker.totalFiles > 0 ? Math.round((tracker.processedFiles / tracker.totalFiles) * 100) : 0}%
                </div>
              </div>
              <div className={styles.stats}>
                <div className={styles.stat}>
                  <strong>{tracker.totalFiles}</strong>
                  <span>Total Files</span>
                </div>
                <div className={styles.stat}>
                  <strong>{tracker.processedFiles}</strong>
                  <span>Processed</span>
                </div>
                <div className={styles.stat}>
                  <strong>{tracker.failedFiles}</strong>
                  <span>Failed</span>
                </div>
              </div>
            </div>
            {tracker.message && (
              <p style={{ color: '#f56565', marginTop: '1rem' }}>
                {tracker.message}
              </p>
            )}
            {(tracker.status === 'COMPLETED' || tracker.status === 'FAILED') && (
              <button className={styles.newUploadButton} onClick={handleNewUpload}>
                Clear & Upload More
              </button>
            )}
          </div>
        </div>
      )}
      
      {/* File Upload Section */}
      <div className={styles.uploadSection}>
        <div
          className={`${styles.dropzone} ${dragging ? styles.dragging : ''}`}
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          onClick={() => fileInputRef.current?.click()}
        >
          <p>📁 Drag & drop resume files here</p>
          <small>or click to browse (PDF, DOC, DOCX, ZIP)</small>
          <input
            ref={fileInputRef}
            type="file"
            multiple
            accept=".pdf,.doc,.docx,.zip"
            onChange={handleFileSelect}
            className={styles.fileInput}
            aria-label="Resume file upload"
          />
        </div>

        {files.length > 0 && (
          <div className={styles.selectedFiles}>
            <h3>Selected Files ({files.length})</h3>
            <ul className={styles.fileList}>
              {files.map((file, index) => (
                <li key={index} className={styles.fileItem}>
                  <div>
                    <div className={styles.fileName}>{file.name}</div>
                    <div className={styles.fileSize}>{formatBytes(file.size)}</div>
                  </div>
                  <button
                    className={styles.removeButton}
                    onClick={() => removeFile(index)}
                  >
                    Remove
                  </button>
                </li>
              ))}
            </ul>
            <button
              className={styles.uploadButton}
              onClick={handleUpload}
              disabled={uploading}
            >
              {uploading ? 'Uploading...' : `Upload ${files.length} file(s)`}
            </button>
          </div>
        )}

        {error && <p style={{ color: '#f56565', marginTop: '1rem' }}>{error}</p>}
      </div>

      {/* Process Tracker Table */}
      <ProcessTrackerTable trackers={trackers} onRefresh={handleRefreshAll} />
    </div>
  )
}

export default FileUpload
