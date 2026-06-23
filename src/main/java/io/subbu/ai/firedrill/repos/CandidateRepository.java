package io.subbu.ai.firedrill.repos;

import io.subbu.ai.firedrill.entities.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Candidate entity operations.
 * Provides CRUD operations and custom queries for candidate management.
 */
@Repository
public interface CandidateRepository extends JpaRepository<Candidate, UUID> {

    /**
     * Find candidate by email address
     * 
     * @param email The candidate's email
     * @return Optional containing the candidate if found
     */
    Optional<Candidate> findByEmail(String email);

    /**
     * Find candidates by name (case-insensitive search)
     * 
     * @param name The candidate name to search for
     * @return List of matching candidates
     */
    @Query("SELECT c FROM Candidate c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Candidate> searchByName(@Param("name") String name);

    /**
     * Find candidates with specific skills
     * 
     * @param skill The skill to search for
     * @return List of candidates with the specified skill
     */
    @Query("SELECT c FROM Candidate c WHERE LOWER(c.skills) LIKE LOWER(CONCAT('%', :skill, '%'))")
    List<Candidate> findBySkillsContaining(@Param("skill") String skill);

    /**
     * Find candidates by years of experience range
     * 
     * @param minYears Minimum years of experience
     * @param maxYears Maximum years of experience
     * @return List of matching candidates
     */
    @Query("SELECT c FROM Candidate c WHERE c.yearsOfExperience BETWEEN :minYears AND :maxYears")
    List<Candidate> findByExperienceRange(@Param("minYears") Integer minYears, 
                                          @Param("maxYears") Integer maxYears);

    /**
     * Count total candidates in the system
     * 
     * @return Total count of candidates
     */
    @Query("SELECT COUNT(c) FROM Candidate c")
    Long countAllCandidates();
}
