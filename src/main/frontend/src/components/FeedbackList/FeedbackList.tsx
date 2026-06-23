/**
 * Feedback List Component
 * Displays feedback for a candidate or job requirement
 */

import { useState, useEffect } from 'react'
import { useSelector } from 'react-redux'
import { graphqlClient } from '@/services/graphql'
import {
  FEEDBACK_FOR_CANDIDATE,
  FEEDBACK_FOR_JOB,
  AVERAGE_RATING_FOR_CANDIDATE,
  AVERAGE_RATING_FOR_JOB,
  DELETE_FEEDBACK,
  HIDE_FEEDBACK,
  SHOW_FEEDBACK,
} from '@/graphql/feedbackQueries'
import { selectUser } from '@store/selectors/authSelectors'
import { EntityType } from '@/components/FeedbackForm/FeedbackForm'
import styles from './FeedbackList.module.css'

interface User {
  id: string
  username: string
  fullName: string
}

interface Feedback {
  id: string
  entityId: string
  entityType: string
  user: User
  feedbackType: string
  rating: number | null
  comments: string | null
  isVisible: boolean
  createdAt: string
  updatedAt: string
}

interface FeedbackListProps {
  entityId: string
  entityType: EntityType
  refreshTrigger?: number
}

export default function FeedbackList({
  entityId,
  entityType,
  refreshTrigger = 0,
}: FeedbackListProps) {
  const [feedbackList, setFeedbackList] = useState<Feedback[]>([])
  const [averageRating, setAverageRating] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const currentUser = useSelector(selectUser)

  const fetchFeedback = async () => {
    setLoading(true)
    setError(null)

    try {
      const query =
        entityType === EntityType.CANDIDATE ? FEEDBACK_FOR_CANDIDATE : FEEDBACK_FOR_JOB
      const ratingQuery =
        entityType === EntityType.CANDIDATE
          ? AVERAGE_RATING_FOR_CANDIDATE
          : AVERAGE_RATING_FOR_JOB
      const variableKey =
        entityType === EntityType.CANDIDATE ? 'candidateId' : 'jobRequirementId'

      const [feedbackResult, ratingResult] = await Promise.all([
        graphqlClient.request<{
          feedbackForCandidate?: Feedback[]
          feedbackForJob?: Feedback[]
        }>(query, { [variableKey]: entityId }),
        graphqlClient.request<{
          averageRatingForCandidate?: number
          averageRatingForJob?: number
        }>(ratingQuery, { [variableKey]: entityId }),
      ])

      const feedback =
        entityType === EntityType.CANDIDATE
          ? feedbackResult.feedbackForCandidate || []
          : feedbackResult.feedbackForJob || []

      const rating =
        entityType === EntityType.CANDIDATE
          ? ratingResult.averageRatingForCandidate
          : ratingResult.averageRatingForJob

      setFeedbackList(feedback)
      setAverageRating(rating || null)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load feedback')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchFeedback()
  }, [entityId, entityType, refreshTrigger])

  const handleDelete = async (feedbackId: string) => {
    if (!confirm('Are you sure you want to delete this feedback?')) return

    try {
      await graphqlClient.request(DELETE_FEEDBACK, { id: feedbackId })
      fetchFeedback()
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to delete feedback')
    }
  }

  const handleToggleVisibility = async (feedbackId: string, isVisible: boolean) => {
    try {
      const mutation = isVisible ? HIDE_FEEDBACK : SHOW_FEEDBACK
      await graphqlClient.request(mutation, { id: feedbackId })
      fetchFeedback()
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to update visibility')
    }
  }

  const getFeedbackTypeBadgeClass = (type: string) => {
    switch (type) {
      case 'SHORTLIST':
        return styles.typeShortlist
      case 'REJECT':
        return styles.typeReject
      case 'INTERVIEW':
        return styles.typeInterview
      case 'OFFER':
        return styles.typeOffer
      case 'TECHNICAL':
        return styles.typeTechnical
      case 'CULTURAL_FIT':
        return styles.typeCulturalFit
      default:
        return styles.typeGeneral
    }
  }

  const renderStars = (rating: number) => {
    return (
      <div className={styles.stars}>
        {[1, 2, 3, 4, 5].map((star) => (
          <span key={star} className={star <= rating ? styles.activeStar : styles.inactiveStar}>
            ⭐
          </span>
        ))}
      </div>
    )
  }

  if (loading && feedbackList.length === 0) {
    return (
      <div className={styles.loading}>
        <div className={styles.spinner}></div>
        <p>Loading feedback...</p>
      </div>
    )
  }

  if (error) {
    return (
      <div className={styles.error}>
        <p>{error}</p>
        <button onClick={fetchFeedback} className={styles.retryButton}>
          Retry
        </button>
      </div>
    )
  }

  return (
    <div className={styles.feedbackList}>
      <div className={styles.header}>
        <h3>Feedback ({feedbackList.length})</h3>
        {averageRating !== null && (
          <div className={styles.averageRating}>
            <span className={styles.avgLabel}>Average Rating:</span>
            <span className={styles.avgValue}>{averageRating.toFixed(1)}/5</span>
            {renderStars(Math.round(averageRating))}
          </div>
        )}
      </div>

      {feedbackList.length === 0 ? (
        <div className={styles.noFeedback}>
          <p>No feedback yet. Be the first to provide feedback!</p>
        </div>
      ) : (
        <div className={styles.feedbackItems}>
          {feedbackList.map((feedback) => (
            <div
              key={feedback.id}
              className={`${styles.feedbackItem} ${
                !feedback.isVisible ? styles.hiddenFeedback : ''
              }`}
            >
              <div className={styles.feedbackHeader}>
                <div className={styles.userInfo}>
                  <span className={styles.userName}>{feedback.user.fullName}</span>
                  <span className={styles.timestamp}>
                    {new Date(feedback.createdAt).toLocaleDateString()}
                  </span>
                </div>
                <div className={styles.feedbackMeta}>
                  <span
                    className={`${styles.typeBadge} ${getFeedbackTypeBadgeClass(
                      feedback.feedbackType
                    )}`}
                  >
                    {feedback.feedbackType.replace('_', ' ')}
                  </span>
                  {!feedback.isVisible && (
                    <span className={styles.visibilityBadge}>Hidden</span>
                  )}
                </div>
              </div>

              {feedback.rating !== null && (
                <div className={styles.rating}>{renderStars(feedback.rating)}</div>
              )}

              {feedback.comments && (
                <p className={styles.comments}>{feedback.comments}</p>
              )}

              {currentUser && currentUser.id === feedback.user.id && (
                <div className={styles.actions}>
                  <button
                    onClick={() => handleToggleVisibility(feedback.id, feedback.isVisible)}
                    className={styles.actionButton}
                    title={feedback.isVisible ? 'Hide feedback' : 'Show feedback'}
                  >
                    {feedback.isVisible ? '👁️' : '🙈'}
                  </button>
                  <button
                    onClick={() => handleDelete(feedback.id)}
                    className={`${styles.actionButton} ${styles.deleteButton}`}
                    title="Delete feedback"
                  >
                    🗑️
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
