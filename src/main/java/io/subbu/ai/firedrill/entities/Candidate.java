package io.subbu.ai.firedrill.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a candidate whose resume has been analyzed.
 * Stores personal information, resume content, and AI-generated analysis results.
 */
@Entity
@Table(name = "candidates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Candidate {

    /**
     * Primary key - unique identifier for the candidate
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    /**
     * Candidate's full name extracted from resume
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Candidate's email address
     */
    @Column(name = "email")
    private String email;

    /**
     * Candidate's mobile/phone number
     */
    @Column(name = "mobile")
    private String mobile;

    /**
     * Original filename of the uploaded resume
     */
    @Column(name = "resume_filename")
    private String resumeFilename;

    /**
     * Full text content extracted from the resume
     */
    @Column(name = "resume_content", columnDefinition = "TEXT")
    private String resumeContent;

    /**
     * Binary data of the original resume file
     */
    @Lob
    @Column(name = "resume_file")
    private byte[] resumeFile;

    /**
     * AI-generated summary of the candidate's experience
     */
    @Column(name = "experience_summary", columnDefinition = "TEXT")
    private String experienceSummary;

    /**
     * Comma-separated list of skills identified in the resume
     */
    @Column(name = "skills", columnDefinition = "TEXT")
    private String skills;

    /**
     * Domain knowledge and industry experience
     */
    @Column(name = "domain_knowledge", columnDefinition = "TEXT")
    private String domainKnowledge;

    /**
     * Academic qualifications and educational background
     */
    @Column(name = "academic_background", columnDefinition = "TEXT")
    private String academicBackground;

    /**
     * Total years of professional experience
     */
    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    /**
     * Simplified experience field (alias for yearsOfExperience)
     */
    @Column(name = "experience")
    private Integer experience;

    /**
     * Education qualification
     */
    @Column(name = "education")
    private String education;

    /**
     * Current company name
     */
    @Column(name = "current_company")
    private String currentCompany;

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
     * One-to-many relationship with candidate matches
     */
    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CandidateMatch> matches;

    /**
     * One-to-many relationship with external profile enrichments
     */
    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CandidateExternalProfile> externalProfiles;
}
