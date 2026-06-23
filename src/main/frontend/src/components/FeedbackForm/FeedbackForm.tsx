/**
 * Feedback Form Component
 * Allows users to submit feedback on candidates or job requirements
 */

import { useState, FormEvent } from 'react'
import { graphqlClient } from '@/services/graphql'
import { SUBMIT_FEEDBACK } from '@/graphql/feedbackQueries'
import styles from './FeedbackForm.module.css'

export enum FeedbackType {
  SHORTLIST = 'SHORTLIST',
  REJECT = 'REJECT',
  INTERVIEW = 'INTERVIEW',
  OFFER = 'OFFER',
  GENERAL = 'GENERAL',
  TECHNICAL = 'TECHNICAL',
  CULTURAL_FIT = 'CULTURAL_FIT',
}

export enum EntityType {
  CANDIDATE = 'CANDIDATE',
  JOB_REQUIREMENT = 'JOB_REQUIREMENT',
}

interface FeedbackFormProps {
  entityId: string
  entityType: EntityType
  onSuccess: () => void
  onCancel: () => void
}

export default function FeedbackForm({
  entityId,
  entityType,
  onSuccess,
  onCancel,
}: FeedbackFormProps) {
  const [feedbackType, setFeedbackType] = useState<FeedbackType>(FeedbackType.GENERAL)
  const [rating, setRating] = useState<number>(3)
  const [comments, setComments] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setIsSubmitting(true)
    setError(null)

    try {
      await graphqlClient.request(SUBMIT_FEEDBACK, {
        input: {
          entityId,
          entityType,
          feedbackType,
          rating,
          comments: comments.trim() || null,
        },
      })
      onSuccess()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to submit feedback')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className={styles.feedbackForm}>
      <h3>Provide Feedback</h3>
      
      {error && (
        <div className={styles.errorMessage}>
          {error}
        </div>
      )}
      
      <form onSubmit={handleSubmit}>
        <div className={styles.formGroup}>
          <label htmlFor="feedbackType">Feedback Type *</label>
          <select
            id="feedbackType"
            value={feedbackType}
            onChange={(e) => setFeedbackType(e.target.value as FeedbackType)}
            required
            disabled={isSubmitting}
          >
            <option value={FeedbackType.SHORTLIST}>Shortlist</option>
            <option value={FeedbackType.REJECT}>Reject</option>
            <option value={FeedbackType.INTERVIEW}>Interview</option>
            <option value={FeedbackType.OFFER}>Offer</option>
            <option value={FeedbackType.GENERAL}>General</option>
            <option value={FeedbackType.TECHNICAL}>Technical</option>
            <option value={FeedbackType.CULTURAL_FIT}>Cultural Fit</option>
          </select>
        </div>

        <div className={styles.formGroup}>
          <label htmlFor="rating">
            Rating: <span className={styles.ratingValue}>{rating}/5</span>
          </label>
          <div className={styles.ratingSlider}>
            {[1, 2, 3, 4, 5].map((value) => (
              <button
                key={value}
                type="button"
                className={`${styles.star} ${value <= rating ? styles.activeStar : ''}`}
                onClick={() => setRating(value)}
                disabled={isSubmitting}
              >
                ⭐
              </button>
            ))}
          </div>
        </div>

        <div className={styles.formGroup}>
          <label htmlFor="comments">Comments</label>
          <textarea
            id="comments"
            value={comments}
            onChange={(e) => setComments(e.target.value)}
            placeholder="Share your thoughts..."
            rows={4}
            disabled={isSubmitting}
          />
        </div>

        <div className={styles.formActions}>
          <button
            type="button"
            onClick={onCancel}
            className={styles.cancelButton}
            disabled={isSubmitting}
          >
            Cancel
          </button>
          <button
            type="submit"
            className={styles.submitButton}
            disabled={isSubmitting}
          >
            {isSubmitting ? 'Submitting...' : 'Submit Feedback'}
          </button>
        </div>
      </form>
    </div>
  )
}
