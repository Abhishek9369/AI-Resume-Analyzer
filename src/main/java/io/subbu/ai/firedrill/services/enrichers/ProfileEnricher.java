package io.subbu.ai.firedrill.services.enrichers;

import io.subbu.ai.firedrill.entities.Candidate;
import io.subbu.ai.firedrill.entities.CandidateExternalProfile;
import io.subbu.ai.firedrill.entities.ExternalProfileSource;

/**
 * Strategy interface for fetching and enriching candidate profile data
 * from a specific external source (e.g. GitHub, LinkedIn, Twitter).
 *
 * <p>Implement this interface for each external source and annotate the
 * implementation with {@code @Component} (or any Spring stereotype). The
 * {@link io.subbu.ai.firedrill.services.CandidateProfileEnrichmentService}
 * auto-discovers all registered enrichers via dependency injection and
 * routes each enrichment request to the correct implementation.</p>
 *
 * <h3>Adding a new source</h3>
 * <ol>
 *   <li>Add a value to {@link ExternalProfileSource}.</li>
 *   <li>Create a class implementing this interface and annotate it with
 *       {@code @Component}.</li>
 *   <li>No other code changes are required — the service will pick it up
 *       automatically.</li>
 * </ol>
 */
public interface ProfileEnricher {

    /**
     * Returns the {@link ExternalProfileSource} this enricher handles.
     * Used to route enrichment requests by source enum value.
     */
    ExternalProfileSource getSource();

    /**
     * Returns {@code true} if this enricher can handle the given profile URL.
     * This enables URL-based auto-detection: given a URL extracted from a
     * resume, the service can find the correct enricher without the caller
     * knowing the source type in advance.
     *
     * @param url a social / profile URL (may be {@code null} or empty)
     * @return true if this enricher recognises the URL's host / pattern
     */
    boolean supportsUrl(String url);

    /**
     * Fetches data from the external source and populates {@code profile}.
     *
     * <p>Implementations should:</p>
     * <ul>
     *   <li>Set all available fields on {@code profile}.</li>
     *   <li>Set {@code profile.status} to {@code "SUCCESS"}, {@code "NOT_FOUND"},
     *       {@code "NOT_AVAILABLE"}, or {@code "FAILED"}.</li>
     *   <li>Set {@code profile.lastFetchedAt} to {@link java.time.LocalDateTime#now()}.</li>
     *   <li>Persist and return the updated profile via the repository.</li>
     *   <li>Never let an unchecked exception propagate — catch it, call the
     *       base-class helper, and return the failed profile.</li>
     * </ul>
     *
     * @param profile   the (possibly new or previously persisted) profile entity
     *                  to populate; never {@code null}
     * @param candidate the candidate whose external profile is being enriched;
     *                  never {@code null}
     * @return the saved {@link CandidateExternalProfile} entity
     */
    CandidateExternalProfile enrich(CandidateExternalProfile profile, Candidate candidate);
}
