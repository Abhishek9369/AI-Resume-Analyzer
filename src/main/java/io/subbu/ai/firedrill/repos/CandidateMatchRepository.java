package io.subbu.ai.firedrill.repos;

import io.subbu.ai.firedrill.entities.Candidate;
import io.subbu.ai.firedrill.entities.CandidateMatch;
import io.subbu.ai.firedrill.entities.JobRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for CandidateMatch entity operations.
 * Manages matching scores between candidates and job requirements.
 */
@Repository
public interface CandidateMatchRepository extends JpaRepository<CandidateMatch, UUID> {

    /**
     * Find all matches for a specific candidate
     * 
     * @param candidate The candidate entity
     * @return List of matches for the candidate
     */
    List<CandidateMatch> findByCandidate(Candidate candidate);

    /**
     * Find all matches for a specific candidate by ID
     * 
     * @param candidateId The candidate UUID
     * @return List of matches for the candidate
     */
    @Query("SELECT cm FROM CandidateMatch cm WHERE cm.candidate.id = :candidateId")
    List<CandidateMatch> findByCandidateId(@Param("candidateId") UUID candidateId);

    /**
     * Find all matches for a specific job requirement
     * 
     * @param jobRequirement The job requirement entity
     * @return List of candidate matches for the job
     */
    List<CandidateMatch> findByJobRequirement(JobRequirement jobRequirement);

    /**
     * Find all matches for a specific job requirement by ID
     * 
     * @param jobRequirementId The job requirement UUID
     * @return List of candidate matches for the job
     */
    @Query("SELECT cm FROM CandidateMatch cm WHERE cm.jobRequirement.id = :jobRequirementId")
    List<CandidateMatch> findByJobRequirementId(@Param("jobRequirementId") UUID jobRequirementId);

    /**
     * Find matches ordered by match score descending
     * 
     * @param jobRequirement The job requirement entity
     * @return List of matches ordered by best score
     */
    List<CandidateMatch> findByJobRequirementOrderByMatchScoreDesc(JobRequirement jobRequirement);

    /**
     * Find matches for a job requirement ordered by match score descending using ID
     * 
     * @param jobRequirementId The job requirement UUID
     * @return List of matches ordered by best score
     */
    @Query("SELECT cm FROM CandidateMatch cm WHERE cm.jobRequirement.id = :jobRequirementId " +
           "ORDER BY cm.matchScore DESC")
    List<CandidateMatch> findByJobRequirementIdOrderByMatchScoreDesc(@Param("jobRequirementId") UUID jobRequirementId);

    /**
     * Find specific match between candidate and job
     * 
     * @param candidate The candidate entity
     * @param jobRequirement The job requirement entity
     * @return Optional containing the match if exists
     */
    Optional<CandidateMatch> findByCandidateAndJobRequirement(Candidate candidate, 
                                                               JobRequirement jobRequirement);

    /**
     * Find specific match between candidate and job by IDs
     * 
     * @param candidateId The candidate UUID
     * @param jobRequirementId The job requirement UUID
     * @return Optional containing the match if exists
     */
    @Query("SELECT cm FROM CandidateMatch cm WHERE cm.candidate.id = :candidateId " +
           "AND cm.jobRequirement.id = :jobRequirementId")
    Optional<CandidateMatch> findByCandidateIdAndJobRequirementId(
            @Param("candidateId") UUID candidateId,
            @Param("jobRequirementId") UUID jobRequirementId);

    /**
     * Find selected candidates for a job requirement
     * 
     * @param jobRequirement The job requirement entity
     * @param isSelected Whether to filter for selected candidates
     * @return List of selected candidate matches
     */
    List<CandidateMatch> findByJobRequirementAndIsSelected(JobRequirement jobRequirement, 
                                                            Boolean isSelected);

    /**
     * Find shortlisted candidates for a job requirement
     * 
     * @param jobRequirement The job requirement entity
     * @param isShortlisted Whether to filter for shortlisted candidates
     * @return List of shortlisted candidate matches
     */
    List<CandidateMatch> findByJobRequirementAndIsShortlisted(JobRequirement jobRequirement, 
                                                               Boolean isShortlisted);

    /**
     * Find top N matches for a job above a minimum score threshold
     * 
     * @param jobRequirement The job requirement entity
     * @param minScore Minimum match score threshold
     * @return List of top matching candidates
     */
    @Query("SELECT cm FROM CandidateMatch cm WHERE cm.jobRequirement = :jobRequirement " +
           "AND cm.matchScore >= :minScore ORDER BY cm.matchScore DESC")
    List<CandidateMatch> findTopMatchesAboveThreshold(@Param("jobRequirement") JobRequirement jobRequirement,
                                                       @Param("minScore") Double minScore);

    /**
     * Find top N matches for a job by ID above a minimum score threshold
     * 
     * @param jobRequirementId The job requirement UUID
     * @param minScore Minimum match score threshold
     * @param limit Maximum number of results
     * @return List of top matching candidates
     */
    @Query(value = "SELECT * FROM candidate_matches " +
                   "WHERE job_requirement_id = :jobRequirementId AND match_score >= :minScore " +
                   "ORDER BY match_score DESC LIMIT :limit", 
           nativeQuery = true)
    List<CandidateMatch> findTopMatchesByJobId(@Param("jobRequirementId") UUID jobRequirementId,
                                                @Param("minScore") Double minScore,
                                                @Param("limit") Integer limit);

    /**
     * Find top N matches for a job by ID (simplified version)
     * 
     * @param jobId The job requirement UUID
     * @param limit Maximum number of results
     * @return List of top matching candidates
     */
    @Query(value = "SELECT * FROM candidate_matches " +
                   "WHERE job_requirement_id = :jobId " +
                   "ORDER BY match_score DESC LIMIT :limit", 
           nativeQuery = true)
    List<CandidateMatch> topMatchesForJob(@Param("jobId") UUID jobId,
                                          @Param("limit") Integer limit);

    /**
     * Find shortlisted candidates for a job requirement by ID
     * 
     * @param jobId The job requirement UUID
     * @param isShortlisted Whether to filter for shortlisted candidates
     * @return List of shortlisted candidate matches
     */
    @Query("SELECT cm FROM CandidateMatch cm WHERE cm.jobRequirement.id = :jobId " +
           "AND cm.isShortlisted = :isShortlisted")
    List<CandidateMatch> findByJobRequirementIdAndIsShortlisted(@Param("jobId") UUID jobId, 
                                                                 @Param("isShortlisted") Boolean isShortlisted);

    /**
     * Find selected candidates for a job requirement by ID
     * 
     * @param jobId The job requirement UUID
     * @param isSelected Whether to filter for selected candidates
     * @return List of selected candidate matches
     */
    @Query("SELECT cm FROM CandidateMatch cm WHERE cm.jobRequirement.id = :jobId " +
           "AND cm.isSelected = :isSelected")
    List<CandidateMatch> findByJobRequirementIdAndIsSelected(@Param("jobId") UUID jobId, 
                                                              @Param("isSelected") Boolean isSelected);
}
