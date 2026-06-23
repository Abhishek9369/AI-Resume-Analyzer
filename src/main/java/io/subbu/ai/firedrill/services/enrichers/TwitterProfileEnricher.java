package io.subbu.ai.firedrill.services.enrichers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.subbu.ai.firedrill.entities.Candidate;
import io.subbu.ai.firedrill.entities.CandidateExternalProfile;
import io.subbu.ai.firedrill.entities.ExternalProfileSource;
import io.subbu.ai.firedrill.repos.CandidateExternalProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDateTime;

/**
 * Enriches a candidate profile using the Twitter / X API v2.
 *
 * <h3>Authentication</h3>
 * Requires a Bearer Token from the
 * <a href="https://developer.twitter.com/en/portal/dashboard">Twitter Developer Portal</a>
 * (free Basic tier or above).  Configure via:
 * <pre>app.enrichment.twitter.bearer-token=${TWITTER_BEARER_TOKEN:}</pre>
 * Without a token this enricher stores {@code NOT_AVAILABLE}.
 *
 * <h3>URL detection</h3>
 * Matches any URL whose host contains {@code twitter.com} or {@code x.com}.
 * The username is extracted from the URL path so no search API call is needed.
 */
@Component
@Slf4j
public class TwitterProfileEnricher extends AbstractProfileEnricher {

    private static final String TWITTER_API_BASE = "https://api.twitter.com/2";

    @Value("${app.enrichment.twitter.bearer-token:}")
    private String bearerToken;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TwitterProfileEnricher(CandidateExternalProfileRepository externalProfileRepository) {
        super(externalProfileRepository);
    }

    @Override
    public ExternalProfileSource getSource() {
        return ExternalProfileSource.TWITTER;
    }

    @Override
    public boolean supportsUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        return lower.contains("twitter.com") || lower.contains("x.com");
    }

    @Override
    public CandidateExternalProfile enrich(CandidateExternalProfile profile, Candidate candidate) {
        log.info("[TWITTER] Enriching profile for candidate: {}", candidate.getName());

        if (bearerToken == null || bearerToken.isBlank()) {
            return saveNotAvailableProfile(profile,
                    "Twitter/X enrichment requires a Bearer Token. "
                    + "Set TWITTER_BEARER_TOKEN env var or app.enrichment.twitter.bearer-token.");
        }

        try {
            String username = resolveUsername(profile);
            if (username == null) {
                return saveNotFoundProfile(profile);
            }

            String url = TWITTER_API_BASE + "/users/by/username/" + username
                    + "?user.fields=name,description,location,public_metrics";
            JsonNode root = callApi(url);
            if (root == null || root.path("data").isMissingNode()) {
                return saveNotFoundProfile(profile);
            }

            JsonNode data = root.path("data");
            profile.setProfileUrl("https://x.com/" + username);
            profile.setDisplayName(nullIfBlank(data.path("name").asText()));
            profile.setBio(nullIfBlank(data.path("description").asText()));
            profile.setLocation(nullIfBlank(data.path("location").asText()));

            JsonNode metrics = data.path("public_metrics");
            if (!metrics.isMissingNode()) {
                profile.setFollowers(metrics.path("followers_count").asInt(0));
            }

            profile.setEnrichedSummary(buildSummary(username, data));
            profile.setStatus("SUCCESS");
            profile.setLastFetchedAt(LocalDateTime.now());
            profile.setErrorMessage(null);

            log.info("[TWITTER] Enriched profile for {} (@{})", candidate.getName(), username);
            return externalProfileRepository.save(profile);

        } catch (HttpClientErrorException.TooManyRequests e) {
            return saveFailedProfile(profile, "Twitter/X API rate limit exceeded — try again later.");
        } catch (HttpClientErrorException.Unauthorized e) {
            return saveFailedProfile(profile, "Twitter/X API authentication failed — check Bearer Token.");
        } catch (Exception e) {
            log.error("[TWITTER] Error for candidate {}: {}", candidate.getName(), e.getMessage(), e);
            return saveFailedProfile(profile, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------

    /**
     * Twitter API v2 free tier has no user-search endpoint.
     * We can only proceed when a URL (containing the username) is available.
     */
    private String resolveUsername(CandidateExternalProfile profile) {
        if (profile.getProfileUrl() != null) {
            return extractUsernameFromUrl(profile.getProfileUrl());
        }
        return null;
    }

    private JsonNode callApi(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(bearerToken);
            headers.set(HttpHeaders.USER_AGENT, "ResumeAnalyzer/1.0");
            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                return objectMapper.readTree(resp.getBody());
            }
        } catch (HttpClientErrorException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[TWITTER] API call failed for {}: {}", url, e.getMessage());
        }
        return null;
    }

    private String buildSummary(String username, JsonNode data) {
        var sb = new StringBuilder("Twitter/X: @").append(username).append(".");
        JsonNode m = data.path("public_metrics");
        if (!m.isMissingNode()) {
            sb.append(String.format(" %d followers, %d following, %d tweets.",
                    m.path("followers_count").asInt(0),
                    m.path("following_count").asInt(0),
                    m.path("tweet_count").asInt(0)));
        }
        String bio = nullIfBlank(data.path("description").asText());
        if (bio != null) sb.append(" Bio: ").append(bio).append(".");
        return sb.toString();
    }

    /** Extracts the Twitter/X username (first path segment, stripping @). */
    public static String extractUsernameFromUrl(String url) {
        if (url == null) return null;
        try {
            String path = new URI(url).getPath();
            if (path == null || path.isBlank()) return null;
            for (String part : path.split("/")) {
                String cleaned = part.replace("@", "").trim();
                if (!cleaned.isBlank()) return cleaned;
            }
        } catch (Exception ignored) { /* malformed URL */ }
        return null;
    }
}
