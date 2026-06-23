package io.subbu.ai.firedrill.repositories;

import io.subbu.ai.firedrill.entities.Feedback;
import io.subbu.ai.firedrill.models.EntityType;
import io.subbu.ai.firedrill.models.FeedbackType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Feedback entity
 */
@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {

    /**
     * Find all feedback for a specific entity
     */
    List<Feedback> findByEntityTypeAndEntityIdAndIsVisibleOrderByCreatedAtDesc(
        EntityType entityType, UUID entityId, boolean isVisible
    );

    /**
     * Find all feedback for a specific entity (including hidden)
     */
    List<Feedback> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
        EntityType entityType, UUID entityId
    );

    /**
     * Find all feedback for a specific entity (without sorting)
     */
    List<Feedback> findByEntityTypeAndEntityId(EntityType entityType, UUID entityId);

    /**
     * Find all visible feedback for a specific entity
     */
    List<Feedback> findByEntityTypeAndEntityIdAndIsVisibleTrue(EntityType entityType, UUID entityId);

    /**
     * Find all feedback by user
     */
    List<Feedback> findByProvidedByIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find all feedback by user (without sorting)
     */
    List<Feedback> findByProvidedById(UUID userId);

    /**
     * Find feedback by type for an entity
     */
    List<Feedback> findByEntityTypeAndEntityIdAndFeedbackTypeAndIsVisible(
        EntityType entityType, UUID entityId, FeedbackType feedbackType, boolean isVisible
    );

    /**
     * Count feedback for an entity
     */
    long countByEntityTypeAndEntityIdAndIsVisible(EntityType entityType, UUID entityId, boolean isVisible);

    /**
     * Count feedback by type for an entity
     */
    long countByEntityTypeAndEntityIdAndFeedbackType(EntityType entityType, UUID entityId, FeedbackType feedbackType);

    /**
     * Get average rating for an entity
     */
    @Query("SELECT AVG(f.rating) FROM Feedback f WHERE " +
           "f.entityType = :entityType AND f.entityId = :entityId AND " +
           "f.rating IS NOT NULL AND f.isVisible = true")
    Double getAverageRating(@Param("entityType") EntityType entityType, @Param("entityId") UUID entityId);

    /**
     * Find recent feedback (last N entries)
     */
    @Query("SELECT f FROM Feedback f WHERE f.isVisible = true ORDER BY f.createdAt DESC")
    List<Feedback> findRecentFeedback();
}
