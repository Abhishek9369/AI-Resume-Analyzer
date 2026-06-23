package io.subbu.ai.firedrill.repos;

import io.subbu.ai.firedrill.entities.MatchAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for MatchAudit entities used for admin monitoring of matching operations.
 */
@Repository
public interface MatchAuditRepository extends JpaRepository<MatchAudit, UUID> {

    /** Get all audits ordered by most recent first */
    List<MatchAudit> findAllByOrderByInitiatedAtDesc();

    /** Get recent audits limited to top N */
    List<MatchAudit> findTop50ByOrderByInitiatedAtDesc();

    /** Get audits for a specific job requirement */
    List<MatchAudit> findByJobRequirementIdOrderByInitiatedAtDesc(UUID jobRequirementId);

    /** Get audits by status */
    List<MatchAudit> findByStatusOrderByInitiatedAtDesc(String status);
}
