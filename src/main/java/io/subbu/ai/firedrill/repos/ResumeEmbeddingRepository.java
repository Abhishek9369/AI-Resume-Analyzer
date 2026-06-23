package io.subbu.ai.firedrill.repos;

import io.subbu.ai.firedrill.entities.Candidate;
import io.subbu.ai.firedrill.entities.ResumeEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for ResumeEmbedding entity operations.
 * Provides vector similarity search capabilities using PostgreSQL pgvector extension.
 */
@Repository
public interface ResumeEmbeddingRepository extends JpaRepository<ResumeEmbedding, UUID> {

    /**
     * Find all embeddings for a specific candidate
     * 
     * @param candidate The candidate entity
     * @return List of embeddings for the candidate
     */
    List<ResumeEmbedding> findByCandidate(Candidate candidate);

    /**
     * Find all embeddings for a specific candidate ID
     * 
     * @param candidateId The candidate UUID
     * @return List of embeddings for the candidate
     */
    @Query("SELECT re FROM ResumeEmbedding re WHERE re.candidate.id = :candidateId")
    List<ResumeEmbedding> findByCandidateId(@Param("candidateId") UUID candidateId);

    /**
     * Find embeddings by section type
     * 
     * @param sectionType The section type (e.g., "experience", "education")
     * @return List of embeddings for the specified section
     */
    List<ResumeEmbedding> findBySectionType(String sectionType);

    /**
     * Find similar resumes using cosine similarity on vector embeddings.
     * Uses pgvector extension for efficient similarity search.
     * The <=> operator computes cosine distance (1 - cosine similarity).
     * 
     * @param embedding The query embedding vector
     * @param limit Maximum number of results
     * @return List of similar resume embeddings ordered by similarity (most similar first)
     */
    @Query(value = "SELECT * FROM resume_embeddings " +
                   "ORDER BY embedding <=> CAST(:embedding AS vector) " +
                   "LIMIT :limit", 
           nativeQuery = true)
    List<ResumeEmbedding> findSimilarResumes(@Param("embedding") String embedding, 
                                             @Param("limit") Integer limit);

    /**
     * Find similar resumes excluding a specific candidate.
     * Useful for finding candidates similar to a given candidate.
     * 
     * @param embedding The query embedding vector
     * @param excludeCandidateId Candidate ID to exclude from results
     * @param limit Maximum number of results
     * @return List of similar resume embeddings
     */
    @Query(value = "SELECT * FROM resume_embeddings " +
                   "WHERE candidate_id != :excludeCandidateId " +
                   "ORDER BY embedding <=> CAST(:embedding AS vector) " +
                   "LIMIT :limit", 
           nativeQuery = true)
    List<ResumeEmbedding> findSimilarResumesExcludingCandidate(
            @Param("embedding") String embedding,
            @Param("excludeCandidateId") UUID excludeCandidateId,
            @Param("limit") Integer limit);

    /**
     * Delete all embeddings for a candidate
     * 
     * @param candidate The candidate whose embeddings should be deleted
     */
    void deleteByCandidate(Candidate candidate);

    /**
     * Delete all embeddings for a candidate by ID
     * 
     * @param candidateId The candidate UUID
     */
    @Query("DELETE FROM ResumeEmbedding re WHERE re.candidate.id = :candidateId")
    void deleteByCandidateId(@Param("candidateId") UUID candidateId);

    /**
     * Native SQL insert to properly handle pgvector type casting.
     * JPA cannot automatically cast String to vector type, so we use native SQL with explicit CAST.
     *
     * @param id UUID for the embedding
     * @param candidateId Candidate UUID
     * @param contentChunk The text chunk
     * @param embedding The vector as string (e.g., "[0.1, 0.2, ...]")
     * @param sectionType The section type
     */
    @jakarta.transaction.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "INSERT INTO resume_embeddings (id, candidate_id, content_chunk, embedding, section_type, created_at) " +
                   "VALUES (CAST(:id AS uuid), CAST(:candidateId AS uuid), :contentChunk, CAST(:embedding AS vector), :sectionType, NOW())",
           nativeQuery = true)
    void insertEmbeddingNative(@Param("id") String id,
                               @Param("candidateId") String candidateId,
                               @Param("contentChunk") String contentChunk,
                               @Param("embedding") String embedding,
                               @Param("sectionType") String sectionType);
}
