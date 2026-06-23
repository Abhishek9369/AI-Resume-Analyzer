package io.subbu.ai.firedrill.services.enrichers;

import io.subbu.ai.firedrill.entities.CandidateExternalProfile;
import io.subbu.ai.firedrill.repos.CandidateExternalProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * Abstract base for {@link ProfileEnricher} implementations.
 * Provides shared helpers for persisting status changes so each
 * enricher only needs to contain source-specific logic.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractProfileEnricher implements ProfileEnricher {

    protected final CandidateExternalProfileRepository externalProfileRepository;

    /** Marks the profile FAILED, records the error, and saves. */
    protected CandidateExternalProfile saveFailedProfile(CandidateExternalProfile profile, String errorMessage) {
        profile.setStatus("FAILED");
        profile.setErrorMessage(errorMessage);
        profile.setLastFetchedAt(LocalDateTime.now());
        log.warn("[{}] Enrichment failed — {}", getSource(), errorMessage);
        return externalProfileRepository.save(profile);
    }

    /** Marks the profile NOT_FOUND and saves. */
    protected CandidateExternalProfile saveNotFoundProfile(CandidateExternalProfile profile) {
        profile.setStatus("NOT_FOUND");
        profile.setLastFetchedAt(LocalDateTime.now());
        profile.setErrorMessage(null);
        log.info("[{}] Profile not found", getSource());
        return externalProfileRepository.save(profile);
    }

    /** Marks the profile NOT_AVAILABLE with an explanatory reason and saves. */
    protected CandidateExternalProfile saveNotAvailableProfile(CandidateExternalProfile profile, String reason) {
        profile.setStatus("NOT_AVAILABLE");
        profile.setErrorMessage(reason);
        profile.setLastFetchedAt(LocalDateTime.now());
        log.info("[{}] Source not available — {}", getSource(), reason);
        return externalProfileRepository.save(profile);
    }

    /**
     * Returns {@code null} if the value is blank or the literal "null";
     * otherwise returns the trimmed value.
     */
    protected static String nullIfBlank(String value) {
        return (value == null || value.isBlank() || "null".equalsIgnoreCase(value.trim()))
                ? null : value.trim();
    }
}
