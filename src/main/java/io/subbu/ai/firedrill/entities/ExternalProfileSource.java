package io.subbu.ai.firedrill.entities;

/**
 * Enum representing the source of external candidate profile data.
 *
 * <p>To add a new source:
 * <ol>
 *   <li>Add a value below.</li>
 *   <li>Create a {@link io.subbu.ai.firedrill.services.enrichers.ProfileEnricher}
 *       {@code @Component} for it.</li>
 *   <li>No other changes needed — the enrichment service auto-discovers it.</li>
 * </ol>
 */
public enum ExternalProfileSource {
    GITHUB,
    LINKEDIN,
    TWITTER,
    INTERNET_SEARCH
}
