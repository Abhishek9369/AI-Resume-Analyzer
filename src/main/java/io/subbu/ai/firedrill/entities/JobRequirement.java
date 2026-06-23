package io.subbu.ai.firedrill.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Entity representing a job requirement/position.
 * Used for matching candidates against specific role requirements.
 */
@Entity
@Table(name = "job_requirements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRequirement {

    /**
     * Primary key - unique identifier for the job requirement
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    /**
     * Job title or position name
     */
    @Column(name = "title", nullable = false)
    private String title;

    /**
     * Detailed job description
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Required skills (comma-separated)
     */
    @Column(name = "required_skills", columnDefinition = "TEXT")
    private String requiredSkills;

    /**
     * Minimum years of experience required
     */
    @Column(name = "min_experience_years")
    private Integer minExperienceYears;

    /**
     * Maximum years of experience
     */
    @Column(name = "max_experience_years")
    private Integer maxExperienceYears;

    /**
     * Simplified minimum experience field (alias for minExperienceYears)
     */
    @Column(name = "min_experience")
    private Integer minExperience;

    /**
     * Simplified maximum experience field (alias for maxExperienceYears)
     */
    @Column(name = "max_experience")
    private Integer maxExperience;

    /**
     * Domain field (alias for domainRequirements)
     */
    @Column(name = "domain", columnDefinition = "TEXT")
    private String domain;

    /**
     * Required academic qualifications
     */
    @Column(name = "required_education", columnDefinition = "TEXT")
    private String requiredEducation;

    /**
     * Domain or industry requirements
     */
    @Column(name = "domain_requirements", columnDefinition = "TEXT")
    private String domainRequirements;

    /**
     * Job location
     */
    @Column(name = "location")
    private String location;

    /**
     * Whether this requirement is currently active
     */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Timestamp when the record was created
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the record was last updated
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Many-to-many relationship with skills
     */
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "job_requirement_skills",
        joinColumns = @JoinColumn(name = "job_requirement_id"),
        inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    @Builder.Default
    private Set<Skill> skills = new HashSet<>();

    /**
     * One-to-many relationship with candidate matches
     */
    @OneToMany(mappedBy = "jobRequirement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CandidateMatch> matches;
}
