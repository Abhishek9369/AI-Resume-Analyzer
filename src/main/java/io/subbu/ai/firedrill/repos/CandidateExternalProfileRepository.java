package io.subbu.ai.firedrill.repos;

import io.subbu.ai.firedrill.entities.CandidateExternalProfile;
import io.subbu.ai.firedrill.entities.ExternalProfileSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for candidate external profiles.
 */
@Repository
public interface CandidateExternalProfileRepository extends JpaRepository<CandidateExternalProfile, UUID> {

    List<CandidateExternalProfile> findByCandidateId(UUID candidateId);

    Optional<CandidateExternalProfile> findByCandidateIdAndSource(UUID candidateId, ExternalProfileSource source);

    List<CandidateExternalProfile> findByCandidateIdAndStatus(UUID candidateId, String status);
}
