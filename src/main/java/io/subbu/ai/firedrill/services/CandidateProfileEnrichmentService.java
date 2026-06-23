package io.subbu.ai.firedrill.services;

import io.subbu.ai.firedrill.entities.Candidate;
import io.subbu.ai.firedrill.entities.CandidateExternalProfile;
import io.subbu.ai.firedrill.entities.ExternalProfileSource;
import io.subbu.ai.firedrill.entities.JobRequirement;
import io.subbu.ai.firedrill.repos.CandidateExternalProfileRepository;
import io.subbu.ai.firedrill.repos.CandidateRepository;
import io.subbu.ai.firedrill.services.enrichers.ProfileEnricher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Orchestrates candidate profile enrichment from multiple external sources.
 *
 * <h3>Design — Strategy Pattern</h3>
 * <p>All {@link ProfileEnricher} implementations registered as Spring beans are
 * discovered automatically at startup and keyed by their
 * {@link ExternalProfileSource}.  The service routes each request to the correct
 * enricher without containing any source-specific logic itself.</p>
 *
 * <h3>Adding a new source</h3>
 * <ol>
 *   <li>Add a value to {@link ExternalProfileSource}.</li>
 *   <li>Create an {@code @Component} implementing {@link ProfileEnricher}.</li>
 *   <li>No changes required here.</li>
 * </ol>
 */
@Service
@Slf4j
public class CandidateProfileEnrichmentService {

    private final CandidateExternalProfileRepository externalProfileRepository;
    private final CandidateRepository candidateRepository;
    private final Map<ExternalProfileSource, ProfileEnricher> enricherMap;
    private final List<ProfileEnricher> enricherList;

    @Value("${app.enrichment.staleness-ttl-days:7}")
    private int stalenessTtlDays;

    public CandidateProfileEnrichmentService(
            CandidateExternalProfileRepository externalProfileRepository,
            CandidateRepository candidateRepository,
            List<ProfileEnricher> enrichers) {

        this.externalProfileRepository = externalProfileRepository;
        this.candidateRepository = candidateRepository;
        this.enricherList = List.copyOf(enrichers);
        this.enricherMap = enrichers.stream()
                .collect(Collectors.toMap(ProfileEnricher::getSource, Function.identity()));

        log.info("CandidateProfileEnrichmentService ready with {} enricher(s): {}",
                enrichers.size(),
                enrichers.stream().map(e -> e.getSource().name()).collect(Collectors.joining(", ")));
    }

    // =========================================================================
    // Queries
    // =========================================================================

    /** Returns all external profiles stored for the given candidate. */
    public List<CandidateExternalProfile> getExternalProfiles(UUID candidateId) {
        return externalProfileRepository.findByCandidateId(candidateId);
    }

    // =========================================================================
    // Enrichment operations
    // =========================================================================

    /**
     * Enriches a candidate profile from the specified source.
     * If a profile for this source already exists it is refreshed.
     */
    @Transactional
    public CandidateExternalProfile enrichProfile(UUID candidateId, ExternalProfileSource source) {
        Candidate candidate = requireCandidate(candidateId);
        ProfileEnricher enricher = requireEnricher(source);
        CandidateExternalProfile profile = getOrCreate(candidate, source);
        log.info("Enriching {} profile for candidate: {}", source, candidate.getName());
        return enricher.enrich(profile, candidate);
    }

    /**
     * Auto-detects the source from a social profile URL, then enriches.
     * Returns {@code null} if no enricher recognises the URL.
     *
     * @param candidateId UUID of the candidate
     * @param profileUrl  URL parsed from the candidate's resume (GitHub, LinkedIn, X…)
     */
    @Transactional
    public CandidateExternalProfile enrichFromUrl(UUID candidateId, String profileUrl) {
        Candidate candidate = requireCandidate(candidateId);
        Optional<ProfileEnricher> match = enricherList.stream()
                .filter(e -> e.supportsUrl(profileUrl))
                .findFirst();

        if (match.isEmpty()) {
            log.warn("No enricher found for URL: {}", profileUrl);
            return null;
        }

        ProfileEnricher enricher = match.get();
        CandidateExternalProfile profile = getOrCreate(candidate, enricher.getSource());
        profile.setProfileUrl(profileUrl);
        log.info("Enriching {} profile from URL {} for candidate: {}",
                enricher.getSource(), profileUrl, candidate.getName());
        return enricher.enrich(profile, candidate);
    }

    /**
     * Refreshes an existing external profile by its own ID.
     */
    @Transactional
    public CandidateExternalProfile refreshProfile(UUID profileId) {
        CandidateExternalProfile profile = externalProfileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("External profile not found: " + profileId));
        ProfileEnricher enricher = requireEnricher(profile.getSource());
        log.info("Refreshing {} profile (id: {}) for candidate: {}",
                profile.getSource(), profileId, profile.getCandidate().getName());
        return enricher.enrich(profile, profile.getCandidate());
    }

    // =========================================================================
    // Context building for AI matching
    // =========================================================================

    /**
     * Aggregates all SUCCESS profiles into a formatted context string for AI prompts.
     *
     * @return context string, or {@code null} if no successful profiles exist
     */
    public String buildEnrichmentContext(UUID candidateId) {
        List<CandidateExternalProfile> profiles =
                externalProfileRepository.findByCandidateIdAndStatus(candidateId, "SUCCESS");
        if (profiles.isEmpty()) return null;

        var sb = new StringBuilder("--- External Profile Information ---\n");
        for (CandidateExternalProfile p : profiles) {
            appendProfile(sb, p);
        }
        return sb.toString();
    }

    /**
     * Job-aware variant: profiles are ranked by their relevance to the given job
     * requirement before being assembled into the context string, so the LLM sees
     * the most useful evidence first.
     *
     * <p>Relevance scoring:
     * <ul>
     *   <li>GITHUB scores highest for engineering/coding roles</li>
     *   <li>TWITTER scores highest for community/advocacy roles</li>
     *   <li>LINKEDIN always provides professional context</li>
     *   <li>INTERNET_SEARCH provides baseline context</li>
     * </ul>
     */
    public String buildEnrichmentContext(UUID candidateId, JobRequirement job) {
        List<CandidateExternalProfile> profiles =
                externalProfileRepository.findByCandidateIdAndStatus(candidateId, "SUCCESS");
        if (profiles.isEmpty()) return null;

        String jobText = String.join(" ",
                nullSafe(job.getTitle()), nullSafe(job.getDescription()),
                nullSafe(job.getRequiredSkills()), nullSafe(job.getDomainRequirements())
        ).toLowerCase();

        var sb = new StringBuilder("--- External Profile Information (ranked by job relevance) ---\n");
        profiles.stream()
                .sorted(Comparator.comparingInt(
                        (CandidateExternalProfile p) -> profileRelevanceScore(p.getSource(), jobText))
                        .reversed())
                .forEach(p -> appendProfile(sb, p));
        return sb.toString();
    }

    // =========================================================================
    // Agentic enrichment helpers
    // =========================================================================

    /**
     * Guarantees an up-to-date INTERNET_SEARCH profile exists.
     * This is always fast (no external API) and acts as a baseline for the agentic loop.
     */
    @Transactional
    public void ensureInternetSearchFresh(Candidate candidate) {
        Optional<CandidateExternalProfile> existing =
                externalProfileRepository.findByCandidateIdAndSource(
                        candidate.getId(), ExternalProfileSource.INTERNET_SEARCH);
        boolean needsRefresh = existing.isEmpty()
                || !"SUCCESS".equals(existing.get().getStatus())
                || isStale(existing.get());

        if (needsRefresh) {
            String reason = existing.isEmpty() ? "missing" : isStale(existing.get()) ? "stale" : "failed";
            log.info("[AUTO-ENRICH] Running INTERNET_SEARCH for {} ({})", candidate.getName(), reason);
            ProfileEnricher enricher = enricherMap.get(ExternalProfileSource.INTERNET_SEARCH);
            if (enricher != null) {
                CandidateExternalProfile profile = existing.orElseGet(() ->
                        CandidateExternalProfile.builder()
                                .candidate(candidate)
                                .source(ExternalProfileSource.INTERNET_SEARCH)
                                .status("PENDING")
                                .build());
                enricher.enrich(profile, candidate);
            }
        }
    }

    /**
     * Refreshes all SUCCESS profiles for the given candidate that are older than
     * the configured staleness TTL.  Stale profiles give the LLM outdated context.
     */
    @Transactional
    public void refreshStaleProfiles(Candidate candidate) {
        List<CandidateExternalProfile> profiles =
                externalProfileRepository.findByCandidateId(candidate.getId());
        for (CandidateExternalProfile profile : profiles) {
            if ("SUCCESS".equals(profile.getStatus()) && isStale(profile)) {
                ProfileEnricher enricher = enricherMap.get(profile.getSource());
                if (enricher != null) {
                    log.info("[STALENESS] Refreshing {} for {} (last fetched: {})",
                            profile.getSource(), candidate.getName(), profile.getLastFetchedAt());
                    try {
                        enricher.enrich(profile, candidate);
                    } catch (Exception e) {
                        log.warn("[STALENESS] Refresh failed for {} {}: {}",
                                profile.getSource(), candidate.getName(), e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Fetches external profiles for each recommended source, skipping those
     * that already have a fresh SUCCESS record.
     * Called after the LLM source selector has recommended specific sources.
     */
    @Transactional
    public void autoEnrich(Candidate candidate, List<ExternalProfileSource> sources) {
        for (ExternalProfileSource source : sources) {
            Optional<CandidateExternalProfile> existing =
                    externalProfileRepository.findByCandidateIdAndSource(candidate.getId(), source);
            boolean needsFetch = existing.isEmpty()
                    || !"SUCCESS".equals(existing.get().getStatus())
                    || isStale(existing.get());

            if (needsFetch) {
                ProfileEnricher enricher = enricherMap.get(source);
                if (enricher != null) {
                    log.info("[AUTO-ENRICH] Fetching {} for {}", source, candidate.getName());
                    CandidateExternalProfile profile = existing.orElseGet(() ->
                            CandidateExternalProfile.builder()
                                    .candidate(candidate)
                                    .source(source)
                                    .status("PENDING")
                                    .build());
                    try {
                        enricher.enrich(profile, candidate);
                    } catch (Exception e) {
                        log.warn("[AUTO-ENRICH] {} failed for {}: {}", source, candidate.getName(), e.getMessage());
                    }
                }
            }
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private boolean isStale(CandidateExternalProfile profile) {
        if (profile.getLastFetchedAt() == null) return true;
        return profile.getLastFetchedAt().isBefore(LocalDateTime.now().minusDays(stalenessTtlDays));
    }

    private static int profileRelevanceScore(ExternalProfileSource source, String jobText) {
        return switch (source) {
            case GITHUB -> containsAny(jobText,
                    "developer", "engineer", "software", "coding", "code", "github",
                    "open source", "backend", "frontend", "fullstack",
                    "java", "python", "javascript", "typescript", "golang", "rust") ? 3 : 1;
            case TWITTER -> containsAny(jobText,
                    "social", "community", "advocate", "evangelist", "content",
                    "marketing", "brand", "speaker", "influencer", "developer relations") ? 3 : 0;
            case LINKEDIN -> 2;         // always professionally relevant
            case INTERNET_SEARCH -> 1;  // always provide baseline context
        };
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private void appendProfile(StringBuilder sb, CandidateExternalProfile p) {
        sb.append(String.format("[Source: %s]\n", p.getSource().name()));
        if (p.getProfileUrl() != null)       sb.append("Profile URL: ").append(p.getProfileUrl()).append('\n');
        if (present(p.getBio()))             sb.append("Bio: ").append(p.getBio()).append('\n');
        if (p.getCompany() != null)          sb.append("Company: ").append(p.getCompany()).append('\n');
        if (p.getLocation() != null)         sb.append("Location: ").append(p.getLocation()).append('\n');
        if (p.getPublicRepos() != null)      sb.append("Public Repos: ").append(p.getPublicRepos()).append('\n');
        if (p.getFollowers() != null)        sb.append("Followers: ").append(p.getFollowers()).append('\n');
        if (present(p.getEnrichedSummary())) sb.append("Summary: ").append(p.getEnrichedSummary()).append('\n');
        if (present(p.getRepositories()))    sb.append("Top Projects: ").append(p.getRepositories()).append('\n');
        sb.append('\n');
    }

    private Candidate requireCandidate(UUID id) {
        return candidateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found: " + id));
    }

    private ProfileEnricher requireEnricher(ExternalProfileSource source) {
        ProfileEnricher e = enricherMap.get(source);
        if (e == null) throw new IllegalArgumentException(
                "No ProfileEnricher registered for: " + source + ". Available: " + enricherMap.keySet());
        return e;
    }

    private CandidateExternalProfile getOrCreate(Candidate candidate, ExternalProfileSource source) {
        return externalProfileRepository
                .findByCandidateIdAndSource(candidate.getId(), source)
                .orElseGet(() -> CandidateExternalProfile.builder()
                        .candidate(candidate)
                        .source(source)
                        .status("PENDING")
                        .build());
    }

    private static boolean present(String s) { return s != null && !s.isBlank(); }
    private static String nullSafe(String s) { return s != null ? s : ""; }
}
