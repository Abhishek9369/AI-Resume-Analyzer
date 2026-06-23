package io.subbu.ai.firedrill.repos;

import io.subbu.ai.firedrill.entities.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Skill entity operations.
 * Manages the master skills database.
 */
@Repository
public interface SkillRepository extends JpaRepository<Skill, UUID> {

    /**
     * Find all active skills
     * 
     * @param isActive Whether to filter for active skills
     * @return List of active skills
     */
    List<Skill> findByIsActive(Boolean isActive);

    /**
     * Find skill by exact name (case-insensitive)
     * 
     * @param name The skill name
     * @return Optional containing the skill if found
     */
    @Query("SELECT s FROM Skill s WHERE LOWER(s.name) = LOWER(:name)")
    Optional<Skill> findByNameIgnoreCase(@Param("name") String name);

    /**
     * Search skills by name pattern (case-insensitive)
     * Used for autocomplete functionality
     * 
     * @param name The search pattern
     * @return List of matching skills
     */
    @Query("SELECT s FROM Skill s WHERE s.isActive = true AND LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY s.name")
    List<Skill> searchByName(@Param("name") String name);

    /**
     * Find skills by category
     * 
     * @param category The skill category
     * @return List of skills in the category
     */
    @Query("SELECT s FROM Skill s WHERE s.isActive = true AND LOWER(s.category) = LOWER(:category) ORDER BY s.name")
    List<Skill> findByCategory(@Param("category") String category);

    /**
     * Get all skill categories
     * 
     * @return List of unique categories
     */
    @Query("SELECT DISTINCT s.category FROM Skill s WHERE s.category IS NOT NULL ORDER BY s.category")
    List<String> findAllCategories();
}
