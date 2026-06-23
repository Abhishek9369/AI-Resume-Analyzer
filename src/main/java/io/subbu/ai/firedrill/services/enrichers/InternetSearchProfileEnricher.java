package io.subbu.ai.firedrill.services.enrichers;

import io.subbu.ai.firedrill.config.EnrichmentProperties;
import io.subbu.ai.firedrill.entities.Candidate;
import io.subbu.ai.firedrill.entities.CandidateExternalProfile;
import io.subbu.ai.firedrill.entities.ExternalProfileSource;
import io.subbu.ai.firedrill.repos.CandidateExternalProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Builds an enrichment context from the candidate's existing resume data.
 *
 * <p>This enricher does <em>not</em> call any external API. It synthesises a
 * structured text block from fields already in the database, giving downstream
 * AI components extra context during candidate matching.  Real web-search
 * (e.g. via Tavily API) can be plugged in here without changing anything else.</p>
 *
 * <h3>URL detection</h3>
 * This enricher does not match any URL pattern — it is invoked explicitly via
 * the {@link ExternalProfileSource#INTERNET_SEARCH} enum value.
 */
@Component
@Slf4j
public class InternetSearchProfileEnricher extends AbstractProfileEnricher {

    private static final String TAVILY_SEARCH_URL = "https://api.tavily.com/search";

    private final EnrichmentProperties enrichmentProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    public InternetSearchProfileEnricher(CandidateExternalProfileRepository externalProfileRepository,
                                         EnrichmentProperties enrichmentProperties) {
        super(externalProfileRepository);
        this.enrichmentProperties = enrichmentProperties;
    }

    @Override
    public ExternalProfileSource getSource() {
        return ExternalProfileSource.INTERNET_SEARCH;
    }

    @Override
    public boolean supportsUrl(String url) {
        return false; // not URL-driven; always explicit
    }

    @Override
    public CandidateExternalProfile enrich(CandidateExternalProfile profile, Candidate candidate) {
        log.info("[INTERNET_SEARCH] Enriching profile for: {}", candidate.getName());
        try {
            profile.setDisplayName(candidate.getName());
            String apiKey = enrichmentProperties.getTavily().getApiKey();
            if (apiKey != null && !apiKey.isBlank()) {
                profile.setEnrichedSummary(enrichWithTavily(candidate, apiKey));
            } else {
                log.debug("[INTERNET_SEARCH] No Tavily API key — using synthesised context");
                profile.setEnrichedSummary(buildSynthesisedContext(candidate));
            }
            profile.setStatus("SUCCESS");
            profile.setLastFetchedAt(LocalDateTime.now());
            profile.setErrorMessage(null);
            return externalProfileRepository.save(profile);
        } catch (Exception e) {
            log.error("[INTERNET_SEARCH] Error for {}: {}", candidate.getName(), e.getMessage(), e);
            return saveFailedProfile(profile, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Tavily real web search
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private String enrichWithTavily(Candidate candidate, String apiKey) {
        String primarySkill = "";
        if (candidate.getSkills() != null && !candidate.getSkills().isBlank()) {
            primarySkill = candidate.getSkills().split(",")[0].trim();
        }
        String query = candidate.getName() + " " + primarySkill + " software developer professional profile";

        Map<String, Object> requestBody = Map.of(
                "api_key", apiKey,
                "query", query,
                "max_results", 5,
                "include_answer", true,
                "search_depth", "basic"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        log.debug("[INTERNET_SEARCH] Tavily query: {}", query);
        Map<String, Object> response = restTemplate.postForObject(TAVILY_SEARCH_URL, request, Map.class);

        if (response == null) {
            log.warn("[INTERNET_SEARCH] Tavily returned null response — falling back to synthesised context");
            return buildSynthesisedContext(candidate);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== Web Search Results for ").append(candidate.getName()).append(" ===\n\n");

        // Include Tavily's AI-generated answer if present
        Object answer = response.get("answer");
        if (answer instanceof String s && !s.isBlank()) {
            sb.append("Summary: ").append(s).append("\n\n");
        }

        // Include top snippet from each search result
        Object results = response.get("results");
        if (results instanceof List<?> resultList) {
            int count = 0;
            for (Object item : resultList) {
                if (count >= 3) break; // top 3 results sufficient
                if (item instanceof Map<?, ?> r) {
                    Object title = r.get("title");
                    Object content = r.get("content");
                    Object url = r.get("url");
                    sb.append("Source: ").append(title).append(" (").append(url).append(")\n");
                    if (content instanceof String c && !c.isBlank()) {
                        // Trim to ~300 chars per result to avoid token bloat
                        String snippet = c.length() > 300 ? c.substring(0, 300) + "..." : c;
                        sb.append(snippet).append("\n\n");
                    }
                    count++;
                }
            }
        }

        if (sb.length() < 100) {
            // Tavily returned nothing useful
            return buildSynthesisedContext(candidate);
        }

        log.info("[INTERNET_SEARCH] Tavily search succeeded for {}", candidate.getName());
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Fallback: synthesise context from existing DB fields
    // -------------------------------------------------------------------------

    private String buildSynthesisedContext(Candidate candidate) {
        var sb = new StringBuilder();
        sb.append("Candidate: ").append(candidate.getName()).append(".");
        if (candidate.getEmail() != null) sb.append(" Email: ").append(candidate.getEmail()).append(".");
        if (candidate.getCurrentCompany() != null) sb.append(" Company: ").append(candidate.getCurrentCompany()).append(".");
        if (candidate.getYearsOfExperience() != null) sb.append(" Experience: ").append(candidate.getYearsOfExperience()).append(" years.");
        if (candidate.getSkills() != null && !candidate.getSkills().isBlank()) sb.append(" Skills: ").append(candidate.getSkills()).append(".");
        if (candidate.getAcademicBackground() != null && !candidate.getAcademicBackground().isBlank()) sb.append(" Education: ").append(candidate.getAcademicBackground()).append(".");
        sb.append(" (Synthesised from resume data.)");
        return sb.toString();
    }
}
