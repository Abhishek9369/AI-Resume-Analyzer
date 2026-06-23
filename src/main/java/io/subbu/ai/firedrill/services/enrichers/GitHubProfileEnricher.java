package io.subbu.ai.firedrill.services.enrichers;

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
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Enriches a candidate profile using the GitHub REST API (v3).
 *
 * <h3>Authentication</h3>
 * Configure an optional personal access token via:
 * <pre>app.enrichment.github.token=${GITHUB_API_TOKEN:}</pre>
 * Authenticated: 5 000 req/hour. Anonymous: 60 req/hour.
 *
 * <h3>URL detection</h3>
 * Matches any URL whose host contains {@code github.com}.
 * When a GitHub URL is present on the profile the username is extracted
 * directly from the path — no name-search API call is needed.
 */
@Component
@Slf4j
public class GitHubProfileEnricher extends AbstractProfileEnricher {

    private static final String GITHUB_API_BASE = "https://api.github.com";
    private static final String USER_AGENT = "ResumeAnalyzer/1.0";

    @Value("${app.enrichment.github.token:}")
    private String githubToken;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GitHubProfileEnricher(CandidateExternalProfileRepository externalProfileRepository) {
        super(externalProfileRepository);
    }

    @Override
    public ExternalProfileSource getSource() {
        return ExternalProfileSource.GITHUB;
    }

    @Override
    public boolean supportsUrl(String url) {
        return url != null && url.toLowerCase().contains("github.com");
    }

    @Override
    public CandidateExternalProfile enrich(CandidateExternalProfile profile, Candidate candidate) {
        log.info("[GITHUB] Enriching profile for candidate: {}", candidate.getName());
        try {
            // 1 — Resolve login: prefer URL on profile, fall back to name search
            String login = resolveLogin(profile, candidate);
            if (login == null) {
                return saveNotFoundProfile(profile);
            }

            // 2 — Fetch user profile
            JsonNode user = callApi(GITHUB_API_BASE + "/users/" + login);
            if (user == null) {
                return saveNotFoundProfile(profile);
            }

            profile.setProfileUrl("https://github.com/" + login);
            profile.setDisplayName(nullIfBlank(user.path("name").asText()));
            profile.setBio(nullIfBlank(user.path("bio").asText()));
            profile.setLocation(nullIfBlank(user.path("location").asText()));
            profile.setCompany(nullIfBlank(user.path("company").asText()));
            profile.setPublicRepos(user.path("public_repos").asInt(0));
            profile.setFollowers(user.path("followers").asInt(0));

            // 3 — Fetch top repos by stars
            String reposUrl = UriComponentsBuilder.fromHttpUrl(GITHUB_API_BASE + "/users/" + login + "/repos")
                    .queryParam("sort", "stars")
                    .queryParam("per_page", 5)
                    .build().toUriString();
            JsonNode repos = callApi(reposUrl);
            List<String> repoSummaries = new ArrayList<>();
            if (repos != null && repos.isArray()) {
                for (JsonNode repo : repos) {
                    String name = repo.path("name").asText();
                    if (!name.isBlank()) {
                        String desc = nullIfBlank(repo.path("description").asText());
                        repoSummaries.add(String.format("%s (%s, %d⭐): %s",
                                name,
                                nullIfBlank(repo.path("language").asText()) != null
                                        ? repo.path("language").asText() : "unknown",
                                repo.path("stargazers_count").asInt(0),
                                desc != null ? desc : ""));
                    }
                }
            }
            if (!repoSummaries.isEmpty()) {
                profile.setRepositories(String.join("; ", repoSummaries));
            }

            profile.setEnrichedSummary(buildSummary(login, user, repoSummaries));
            profile.setStatus("SUCCESS");
            profile.setLastFetchedAt(LocalDateTime.now());
            profile.setErrorMessage(null);

            log.info("[GITHUB] Enriched profile for {} (login: {})", candidate.getName(), login);
            return externalProfileRepository.save(profile);

        } catch (HttpClientErrorException.TooManyRequests e) {
            return saveFailedProfile(profile, "GitHub API rate limit exceeded — try again later.");
        } catch (Exception e) {
            log.error("[GITHUB] Error for candidate {}: {}", candidate.getName(), e.getMessage(), e);
            return saveFailedProfile(profile, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------

    private String resolveLogin(CandidateExternalProfile profile, Candidate candidate) {
        // Try to extract from a stored or passed profile URL
        if (profile.getProfileUrl() != null) {
            String fromUrl = extractLoginFromUrl(profile.getProfileUrl());
            if (fromUrl != null) return fromUrl;
        }

        // Fall back to GitHub user-search by name
        String searchName = buildSearchName(candidate);
        String searchUrl = UriComponentsBuilder.fromHttpUrl(GITHUB_API_BASE + "/search/users")
                .queryParam("q", searchName + " in:name")
                .queryParam("per_page", 3)
                .build().toUriString();

        JsonNode result = callApi(searchUrl);
        if (result == null) return null;
        JsonNode items = result.path("items");
        if (!items.isArray() || items.isEmpty()) return null;
        return nullIfBlank(items.get(0).path("login").asText());
    }

    private JsonNode callApi(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, USER_AGENT);
            headers.set(HttpHeaders.ACCEPT, "application/vnd.github.v3+json");
            if (githubToken != null && !githubToken.isBlank()) {
                headers.setBearerAuth(githubToken);
            }
            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                return objectMapper.readTree(resp.getBody());
            }
        } catch (HttpClientErrorException e) {
            throw e; // let caller handle typed errors
        } catch (Exception e) {
            log.warn("[GITHUB] API call failed for {}: {}", url, e.getMessage());
        }
        return null;
    }

    private String buildSummary(String login, JsonNode user, List<String> repos) {
        var sb = new StringBuilder();
        sb.append(String.format("GitHub: @%s — %d public repos, %d followers.",
                login, user.path("public_repos").asInt(0), user.path("followers").asInt(0)));
        String blog = nullIfBlank(user.path("blog").asText());
        if (blog != null) sb.append(" Blog: ").append(blog).append(".");
        if (!repos.isEmpty()) sb.append(" Top projects: ").append(String.join(", ", repos)).append(".");
        return sb.toString();
    }

    private String buildSearchName(Candidate candidate) {
        String name = candidate.getName() == null ? "" : candidate.getName().trim();
        String[] parts = name.split("\\s+");
        return parts.length >= 2 ? parts[0] + " " + parts[parts.length - 1] : name;
    }

    /** Extracts the first path segment (= username) from a github.com URL. */
    public static String extractLoginFromUrl(String url) {
        if (url == null) return null;
        try {
            String path = new URI(url).getPath();
            if (path == null || path.isBlank()) return null;
            for (String part : path.split("/")) {
                if (!part.isBlank()) return part;
            }
        } catch (Exception ignored) { /* malformed URL */ }
        return null;
    }
}
