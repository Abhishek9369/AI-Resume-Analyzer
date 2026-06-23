package io.subbu.ai.firedrill.services.enrichers;

import io.subbu.ai.firedrill.entities.Candidate;
import io.subbu.ai.firedrill.entities.CandidateExternalProfile;
import io.subbu.ai.firedrill.entities.ExternalProfileSource;
import io.subbu.ai.firedrill.repos.CandidateExternalProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * LinkedIn profile enricher.
 *
 * <p><strong>Current status — NOT_AVAILABLE.</strong>  LinkedIn's REST API
 * requires per-member OAuth 2.0 consent; there is no server-side lookup by
 * name or URL without that consent.  This enricher stores a friendly
 * {@code NOT_AVAILABLE} status so the UI can surface an appropriate message.
 * When a profile URL is available it is preserved so the UI can show a
 * deep-link directly to the candidate's LinkedIn page.</p>
 *
 * <h3>Future integration path</h3>
 * <ol>
 *   <li>Register an app at <a href="https://developer.linkedin.com/">developer.linkedin.com</a>.</li>
 *   <li>Obtain {@code r_liteprofile} / {@code r_emailaddress} OAuth scopes.</li>
 *   <li>Replace the body of {@link #enrich} with OAuth token flow + API calls.</li>
 * </ol>
 */
@Component
@Slf4j
public class LinkedInProfileEnricher extends AbstractProfileEnricher {

    private static final String NOT_AVAILABLE_REASON =
            "LinkedIn integration requires OAuth 2.0 from the candidate. "
            + "Planned for a future release.";

    public LinkedInProfileEnricher(CandidateExternalProfileRepository externalProfileRepository) {
        super(externalProfileRepository);
    }

    @Override
    public ExternalProfileSource getSource() {
        return ExternalProfileSource.LINKEDIN;
    }

    @Override
    public boolean supportsUrl(String url) {
        return url != null && url.toLowerCase().contains("linkedin.com");
    }

    @Override
    public CandidateExternalProfile enrich(CandidateExternalProfile profile, Candidate candidate) {
        log.info("[LINKEDIN] OAuth required — storing placeholder for: {}", candidate.getName());

        // Preserve what we can so the UI can show a useful link
        if (profile.getProfileUrl() == null && candidate.getName() != null) {
            profile.setProfileUrl("https://www.linkedin.com/search/results/people/?keywords="
                    + URLEncoder.encode(candidate.getName(), StandardCharsets.UTF_8));
        }
        profile.setDisplayName(candidate.getName());

        return saveNotAvailableProfile(profile, NOT_AVAILABLE_REASON);
    }
}
