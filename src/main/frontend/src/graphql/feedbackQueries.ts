/**
 * GraphQL Queries and Mutations for Feedback
 */

export const FEEDBACK_FOR_CANDIDATE = `
  query FeedbackForCandidate($candidateId: UUID!) {
    feedbackForCandidate(candidateId: $candidateId) {
      id
      entityId
      entityType
      user {
        id
        username
        fullName
      }
      feedbackType
      rating
      comments
      isVisible
      createdAt
      updatedAt
    }
  }
`

export const FEEDBACK_FOR_JOB = `
  query FeedbackForJob($jobRequirementId: UUID!) {
    feedbackForJob(jobRequirementId: $jobRequirementId) {
      id
      entityId
      entityType
      user {
        id
        username
        fullName
      }
      feedbackType
      rating
      comments
      isVisible
      createdAt
      updatedAt
    }
  }
`

export const FEEDBACK_BY_USER = `
  query FeedbackByUser($userId: UUID!) {
    feedbackByUser(userId: $userId) {
      id
      entityId
      entityType
      user {
        id
        username
        fullName
      }
      feedbackType
      rating
      comments
      isVisible
      createdAt
      updatedAt
    }
  }
`

export const AVERAGE_RATING_FOR_CANDIDATE = `
  query AverageRatingForCandidate($candidateId: UUID!) {
    averageRatingForCandidate(candidateId: $candidateId)
  }
`

export const AVERAGE_RATING_FOR_JOB = `
  query AverageRatingForJob($jobRequirementId: UUID!) {
    averageRatingForJob(jobRequirementId: $jobRequirementId)
  }
`

export const FEEDBACK_STATISTICS = `
  query FeedbackStatistics($entityId: UUID!, $entityType: EntityType!) {
    feedbackStatistics(entityId: $entityId, entityType: $entityType) {
      total
      averageRating
      byType {
        feedbackType
        count
      }
    }
  }
`

export const SUBMIT_FEEDBACK = `
  mutation SubmitFeedback($input: FeedbackInput!) {
    submitFeedback(input: $input) {
      id
      entityId
      entityType
      user {
        id
        username
        fullName
      }
      feedbackType
      rating
      comments
      isVisible
      createdAt
      updatedAt
    }
  }
`

export const UPDATE_FEEDBACK = `
  mutation UpdateFeedback($id: UUID!, $input: UpdateFeedbackInput!) {
    updateFeedback(id: $id, input: $input) {
      id
      entityId
      entityType
      user {
        id
        username
        fullName
      }
      feedbackType
      rating
      comments
      isVisible
      createdAt
      updatedAt
    }
  }
`

export const DELETE_FEEDBACK = `
  mutation DeleteFeedback($id: UUID!) {
    deleteFeedback(id: $id)
  }
`

export const HIDE_FEEDBACK = `
  mutation HideFeedback($id: UUID!) {
    hideFeedback(id: $id) {
      id
      isVisible
    }
  }
`

export const SHOW_FEEDBACK = `
  mutation ShowFeedback($id: UUID!) {
    showFeedback(id: $id) {
      id
      isVisible
    }
  }
`
