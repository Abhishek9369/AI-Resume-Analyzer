package io.subbu.ai.firedrill.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entity representing a skill in the master skills database.
 * Used for autocomplete suggestions when creating job requirements.
 */
@Entity
@Table(name = "skills")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Skill {

    /**
     * Primary key - unique identifier for the skill
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    /**
     * Skill name (unique)
     */
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    /**
     * Category of the skill (e.g., Programming Language, Framework, Database, etc.)
     */
    @Column(name = "category")
    private String category;

    /**
     * Description of the skill
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Indicates if this skill is active/visible for selection
     */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Timestamp when the skill was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Timestamp when the skill was last updated
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Job requirements that include this skill
     */
    @ManyToMany(mappedBy = "skills")
    @Builder.Default
    private Set<JobRequirement> jobRequirements = new HashSet<>();
}
