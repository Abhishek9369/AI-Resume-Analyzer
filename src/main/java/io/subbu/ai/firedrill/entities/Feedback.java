package io.subbu.ai.firedrill.entities;

import io.subbu.ai.firedrill.models.EntityType;
import io.subbu.ai.firedrill.models.FeedbackType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Feedback entity for candidate and job requirement feedback
 */
@Entity
@Table(name = "feedback", indexes = {
    @Index(name = "idx_feedback_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_feedback_provided_by", columnList = "provided_by")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 20)
    private EntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_type", nullable = false, length = 20)
    private FeedbackType feedbackType;

    @Column
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provided_by", nullable = false)
    private User providedBy;

    @Column(name = "is_visible", nullable = false)
    private boolean isVisible = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Alias for providedBy to match the GraphQL schema field name 'user'
     */
    public User getUser() {
        return this.providedBy;
    }

    /**
     * Check if feedback has a rating
     */
    public boolean hasRating() {
        return rating != null && rating > 0;
    }

    /**
     * Check if rating is valid (1-5)
     */
    public boolean isValidRating() {
        return rating != null && rating >= 1 && rating <= 5;
    }

    /**
     * Hide feedback
     */
    public void hide() {
        this.isVisible = false;
    }

    /**
     * Show feedback
     */
    public void show() {
        this.isVisible = true;
    }

    /**
     * Set visibility
     */
    public void setIsVisible(boolean visible) {
        this.isVisible = visible;
    }
}
