package io.subbu.ai.firedrill.entities;

import com.pgvector.PGvector;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity storing vector embeddings for resume content.
 * Uses PostgreSQL pgvector extension for similarity search and semantic matching.
 */
@Entity
@Table(name = "resume_embeddings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeEmbedding {

    /**
     * Primary key - unique identifier for the embedding
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    /**
     * Foreign key to the candidate
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    /**
     * The text chunk that was embedded
     */
    @Column(name = "content_chunk", columnDefinition = "TEXT")
    private String contentChunk;

    /**
     * Vector embedding stored as pgvector type.
     * Dimension is 768 for nomic-embed-text model.
     */
    @Column(name = "embedding", columnDefinition = "vector(768)")
    private String embedding;

    /**
     * Section of resume this embedding represents (e.g., "experience", "education", "skills")
     */
    @Column(name = "section_type")
    private String sectionType;

    /**
     * Timestamp when the embedding was created
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}