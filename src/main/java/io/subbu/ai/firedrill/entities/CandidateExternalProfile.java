package io.subbu.ai.firedrill.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity storing candidate profile information fetched from external sources
 * such as GitHub, LinkedIn (public web), or general internet search.
 * This enriched data provides additional context for candidate matching.
 */
@Entity
@Table(
    name = "candidate_external_profiles",
    uniqueConstraints = @UniqueConstraint(columnNames = {"candidate_id", "source"})
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateExternalProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    /**
     * The candidate this external profile belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    /**
     * Source of the external profile data.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private ExternalProfileSource source;

    /**
     * URL to the candidate's profile page on the external source.
     */
    @Column(name = "profile_url")
    private String profileUrl;

    /**
     * Display name on the external platform.
     */
    @Column(name = "display_name")
    private String displayName;

    /**
     * Bio or headline from the external profile.
     */
    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    /**
     * Location listed on the external profile.
     */
    @Column(name = "location")
    private String location;

    /**
     * Company/organization from the external profile.
     */
    @Column(name = "company")
    private String company;

    /**
     * Number of public repositories (GitHub-specific).
     */
    @Column(name = "public_repos")
    private Integer publicRepos;

    /**
     * Number of followers on the platform.
     */
    @Column(name = "followers")
    private Integer followers;

    /**
     * JSON array of notable repositories or public contributions.
     */
    @Column(name = "repositories", columnDefinition = "TEXT")
    private String repositories;

    /**
     * AI-generated summary of the enriched profile data for use in matching.
     */
    @Column(name = "enriched_summary", columnDefinition = "TEXT")
    private String enrichedSummary;

    /**
     * Status of enrichment: PENDING, SUCCESS, FAILED, NOT_FOUND.
     */
    @Column(name = "status")
    private String status;

    /**
     * Error message if enrichment failed.
     */
    @Column(name = "error_message")
    private String errorMessage;

    /**
     * When the external data was last fetched.
     */
    @Column(name = "last_fetched_at")
    private LocalDateTime lastFetchedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
