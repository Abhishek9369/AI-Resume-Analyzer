package io.subbu.ai.firedrill.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing an audit record of a candidate matching operation.
 * Stored asynchronously after each matching run for admin monitoring.
 */
@Entity
@Table(name = "match_audits")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "job_requirement_id")
    private UUID jobRequirementId;

    @Column(name = "job_title")
    private String jobTitle;

    /** Total number of candidates processed in this match run */
    @Column(name = "total_candidates")
    @Builder.Default
    private int totalCandidates = 0;

    /** Number of candidates successfully matched (no error) */
    @Column(name = "successful_matches")
    @Builder.Default
    private int successfulMatches = 0;

    /** Number of candidates auto-shortlisted (score >= 70) */
    @Column(name = "shortlisted_count")
    @Builder.Default
    private int shortlistedCount = 0;

    /** Average match score across all candidates */
    @Column(name = "average_match_score")
    private Double averageMatchScore;

    /** Highest match score among all candidates */
    @Column(name = "highest_match_score")
    private Double highestMatchScore;

    /** Duration of the matching operation in milliseconds */
    @Column(name = "duration_ms")
    private Long durationMs;

    /** Rough estimate of LLM tokens used (~1500 per candidate) */
    @Column(name = "estimated_tokens_used")
    private Integer estimatedTokensUsed;

    /** Status: IN_PROGRESS, COMPLETED, FAILED */
    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "IN_PROGRESS";

    /** Username of the user who triggered this match */
    @Column(name = "initiated_by")
    private String initiatedBy;

    /** JSON array of per-candidate match summaries */
    @Column(name = "match_summaries", columnDefinition = "TEXT")
    private String matchSummaries;

    /** Error message if the run failed */
    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "initiated_at")
    private LocalDateTime initiatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
