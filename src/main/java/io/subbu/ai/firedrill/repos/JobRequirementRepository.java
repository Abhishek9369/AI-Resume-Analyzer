package io.subbu.ai.firedrill.repos;

import io.subbu.ai.firedrill.entities.JobRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for JobRequirement entity operations.
 * Manages job postings and requirements for candidate matching.
 */
@Repository
public interface JobRequirementRepository extends JpaRepository<JobRequirement, UUID> {

    /**
     * Find all active job requirements
     * 
     * @param isActive Whether to filter for active jobs
     * @return List of active job requirements
     */
    List<JobRequirement> findByIsActive(Boolean isActive);

    /**
     * Find job requirements by title (case-insensitive search)
     * 
     * @param title The title to search for
     * @return List of matching job requirements
     */
    @Query("SELECT j FROM JobRequirement j WHERE LOWER(j.title) LIKE LOWER(CONCAT('%', :title, '%'))")
    List<JobRequirement> searchByTitle(@Param("title") String title);

    /**
     * Find job requirements by experience range
     * 
     * @param years Years of experience to match
     * @return List of job requirements matching the experience level
     */
    @Query("SELECT j FROM JobRequirement j WHERE " +
           "j.minExperienceYears <= :years AND " +
           "(j.maxExperienceYears >= :years OR j.maxExperienceYears IS NULL)")
    List<JobRequirement> findByExperienceYears(@Param("years") Integer years);

    /**
     * Find active job requirements with specific skills
     * 
     * @param skill The skill to search for
     * @return List of job requirements requiring the specified skill
     */
    @Query("SELECT j FROM JobRequirement j WHERE j.isActive = true AND " +
           "LOWER(j.requiredSkills) LIKE LOWER(CONCAT('%', :skill, '%'))")
    List<JobRequirement> findActiveBySkill(@Param("skill") String skill);

    /**
     * Find all active job requirements ordered by creation date descending
     * 
     * @return List of active jobs sorted by most recent first
     */
    @Query("SELECT j FROM JobRequirement j WHERE j.isActive = true ORDER BY j.createdAt DESC")
    List<JobRequirement> findAllActiveOrderByCreatedAtDesc();
}
