package io.subbu.ai.firedrill.services;

import io.subbu.ai.firedrill.config.EnrichmentProperties;
import io.subbu.ai.firedrill.config.SecurityUtils;
import io.subbu.ai.firedrill.entities.Candidate;
import io.subbu.ai.firedrill.entities.ExternalProfileSource;
import io.subbu.ai.firedrill.entities.CandidateMatch;
import io.subbu.ai.firedrill.entities.JobRequirement;
import io.subbu.ai.firedrill.entities.MatchAudit;
import io.subbu.ai.firedrill.models.CandidateMatchRequest;
import io.subbu.ai.firedrill.models.CandidateMatchResponse;
import io.subbu.ai.firedrill.repos.CandidateMatchRepository;
import io.subbu.ai.firedrill.repos.CandidateRepository;
import io.subbu.ai.firedrill.repos.JobRequirementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for matching candidates against job requirements using AI.
 * Generates matching scores and stores results in the database.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateMatchingService {

    private final CandidateRepository candidateRepository;
    private final JobRequirementRepository jobRequirementRepository;
    private final CandidateMatchRepository matchRepository;
    private final AIService aiService;
    private final MatchAuditService matchAuditService;
    private final CandidateProfileEnrichmentService enrichmentService;
    private final EnrichmentProperties enrichmentProps;

    /**
     * Match a single candidate against a job requirement.
     * 
     * @param candidateId Candidate UUID
     * @param jobRequirementId Job requirement UUID
     * @return Created or updated candidate match
     */
    @Transactional
    public CandidateMatch matchCandidateToJob(UUID candidateId, UUID jobRequirementId) {
        log.info("Matching candidate {} to job {}", candidateId, jobRequirementId);

        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found: " + candidateId));

        JobRequirement job = jobRequirementRepository.findById(jobRequirementId)
                .orElseThrow(() -> new IllegalArgumentException("Job requirement not found: " + jobRequirementId));

        // Check if match already exists
        return matchRepository.findByCandidateIdAndJobRequirementId(candidateId, jobRequirementId)
                .map(existingMatch -> updateMatch(existingMatch, candidate, job))
                .orElseGet(() -> createNewMatch(candidate, job));
    }

    /**
     * Match all candidates against a specific job requirement.
     * 
     * @param jobRequirementId Job requirement UUID
     * @return List of candidate matches
     */
    @Transactional
    public List<CandidateMatch> matchAllCandidatesToJob(UUID jobRequirementId) {
        log.info("Matching all candidates to job {}", jobRequirementId);

        JobRequirement job = jobRequirementRepository.findById(jobRequirementId)
                .orElseThrow(() -> new IllegalArgumentException("Job requirement not found: " + jobRequirementId));

        String initiatedBy = SecurityUtils.getCurrentUsername().orElse("system");
        MatchAudit audit = matchAuditService.createAudit(jobRequirementId, job.getTitle(), initiatedBy);
        long startTime = System.currentTimeMillis();

        List<Candidate> allCandidates = candidateRepository.findAll();
        List<CandidateMatch> matches = new ArrayList<>();

        try {
            for (Candidate candidate : allCandidates) {
                try {
                    CandidateMatch match = matchRepository.findByCandidateIdAndJobRequirementId(
                            candidate.getId(), jobRequirementId)
                            .map(existingMatch -> updateMatch(existingMatch, candidate, job))
                            .orElseGet(() -> createNewMatch(candidate, job));
                    matches.add(match);
                } catch (Exception e) {
                    log.error("Error matching candidate {} to job {}",
                             candidate.getId(), jobRequirementId, e);
                }
            }

            log.info("Matched {} candidates to job {}", matches.size(), jobRequirementId);
            long durationMs = System.currentTimeMillis() - startTime;
            matchAuditService.completeAudit(audit.getId(), matches, durationMs);
        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startTime;
            matchAuditService.failAudit(audit.getId(), e.getMessage(), durationMs);
            throw e;
        }

        return matches;
    }

    /**
     * Match a single candidate against all active job requirements.
     * 
     * @param candidateId Candidate UUID
     * @return List of candidate matches
     */
    @Transactional
    public List<CandidateMatch> matchCandidateToAllJobs(UUID candidateId) {
        log.info("Matching candidate {} to all active jobs", candidateId);

        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found: " + candidateId));

        List<JobRequirement> activeJobs = jobRequirementRepository.findByIsActive(true);
        List<CandidateMatch> matches = new ArrayList<>();

        for (JobRequirement job : activeJobs) {
            try {
                CandidateMatch match = matchRepository.findByCandidateIdAndJobRequirementId(
                        candidateId, job.getId())
                        .map(existingMatch -> updateMatch(existingMatch, candidate, job))
                        .orElseGet(() -> createNewMatch(candidate, job));
                matches.add(match);
            } catch (Exception e) {
                log.error("Error matching candidate {} to job {}", 
                         candidateId, job.getId(), e);
            }
        }

        log.info("Matched candidate {} to {} jobs", candidateId, matches.size());
        return matches;
    }

    /**
     * Create a new candidate match using AI.
     * 
     * @param candidate Candidate entity
     * @param job Job requirement entity
     * @return Created candidate match
     */
    private CandidateMatch createNewMatch(Candidate candidate, JobRequirement job) {
        CandidateMatchResponse matchResponse = performAIMatching(candidate, job);

        CandidateMatch match = CandidateMatch.builder()
                .candidate(candidate)
                .jobRequirement(job)
                .matchScore(matchResponse.getMatchScore())
                .skillsScore(matchResponse.getSkillsScore())
                .experienceScore(matchResponse.getExperienceScore())
                .educationScore(matchResponse.getEducationScore())
                .domainScore(matchResponse.getDomainScore())
                .matchExplanation(buildExplanation(matchResponse))
                .isSelected(false)
                .isShortlisted(matchResponse.getMatchScore() >= 70.0)
                .build();

        return matchRepository.save(match);
    }

    /**
     * Update an existing candidate match with fresh AI analysis.
     * 
     * @param existingMatch Existing match entity
     * @param candidate Candidate entity
     * @param job Job requirement entity
     * @return Updated candidate match
     */
    private CandidateMatch updateMatch(CandidateMatch existingMatch, Candidate candidate, JobRequirement job) {
        CandidateMatchResponse matchResponse = performAIMatching(candidate, job);

        existingMatch.setMatchScore(matchResponse.getMatchScore());
        existingMatch.setSkillsScore(matchResponse.getSkillsScore());
        existingMatch.setExperienceScore(matchResponse.getExperienceScore());
        existingMatch.setEducationScore(matchResponse.getEducationScore());
        existingMatch.setDomainScore(matchResponse.getDomainScore());
        existingMatch.setMatchExplanation(buildExplanation(matchResponse));

        // Auto-shortlist if score is high
        if (matchResponse.getMatchScore() >= 70.0 && !existingMatch.getIsSelected()) {
            existingMatch.setIsShortlisted(true);
        }

        return matchRepository.save(existingMatch);
    }

    /**
     * Full agentic RAG matching pipeline:
     * <ol>
     *   <li>Refresh profiles that are past the staleness TTL</li>
     *   <li>Ensure a baseline INTERNET_SEARCH profile exists and is fresh</li>
     *   <li>Optionally run LLM source-selector to fetch targeted profiles</li>
     *   <li>Build a job-aware, relevance-ranked enrichment context</li>
     *   <li>First-pass AI match</li>
     *   <li>Multi-pass re-match for borderline candidates using freshly fetched data</li>
     * </ol>
     */
    private CandidateMatchResponse performAIMatching(Candidate candidate, JobRequirement job) {

        // Step 1 — TTL-based staleness refresh
        try {
            enrichmentService.refreshStaleProfiles(candidate);
        } catch (Exception e) {
            log.warn("[AGENTIC] Staleness refresh failed for {}: {}", candidate.getName(), e.getMessage());
        }

        // Step 2 — Always ensure a baseline INTERNET_SEARCH profile is present
        try {
            enrichmentService.ensureInternetSearchFresh(candidate);
        } catch (Exception e) {
            log.warn("[AGENTIC] ensureInternetSearchFresh failed for {}: {}", candidate.getName(), e.getMessage());
        }

        // Step 3 — LLM source selector (opt-in)
        if (enrichmentProps.isSourceSelectionEnabled()) {
            try {
                List<ExternalProfileSource> sources = aiService.selectEnrichmentSources(candidate, job);
                log.info("[AGENTIC] LLM selected sources for {}: {}", candidate.getName(), sources);
                enrichmentService.autoEnrich(candidate, sources);
            } catch (Exception e) {
                log.warn("[AGENTIC] Source selection/auto-enrich failed for {}: {}", candidate.getName(), e.getMessage());
            }
        }

        // Step 4 — Job-aware ranked enrichment context
        String enrichedContext = null;
        try {
            enrichedContext = enrichmentService.buildEnrichmentContext(candidate.getId(), job);
        } catch (Exception e) {
            log.warn("[AGENTIC] buildEnrichmentContext failed for {}: {}", candidate.getId(), e.getMessage());
        }

        // Step 5 — First-pass match
        CandidateMatchResponse firstPass = doMatch(candidate, job, enrichedContext);
        log.info("[AGENTIC] First-pass score for {}: {}", candidate.getName(), firstPass.getMatchScore());

        // Step 6 — Multi-pass for borderline candidates when we had no context the first time
        if (enrichmentProps.getMultiPass().isEnabled()
                && enrichedContext == null
                && firstPass.getMatchScore() >= enrichmentProps.getMultiPass().getBorderlineMin()
                && firstPass.getMatchScore() <= enrichmentProps.getMultiPass().getBorderlineMax()) {

            log.info("[AGENTIC] Borderline score {:.0f} for {} — running second pass with enrichment",
                    firstPass.getMatchScore(), candidate.getName());
            try {
                String enrichedContext2 = enrichmentService.buildEnrichmentContext(candidate.getId(), job);
                if (enrichedContext2 != null) {
                    CandidateMatchResponse secondPass = doMatch(candidate, job, enrichedContext2);
                    log.info("[AGENTIC] Multi-pass score: {:.0f} → {:.0f} for {}",
                            firstPass.getMatchScore(), secondPass.getMatchScore(), candidate.getName());
                    return secondPass;
                }
            } catch (Exception e) {
                log.warn("[AGENTIC] Multi-pass failed for {}: {}", candidate.getName(), e.getMessage());
            }
        }

        return firstPass;
    }

    /**
     * Single AI match call — shared by first-pass and multi-pass.
     */
    private CandidateMatchResponse doMatch(Candidate candidate, JobRequirement job, String enrichedContext) {
        CandidateMatchRequest matchRequest = CandidateMatchRequest.builder()
                .experienceSummary(candidate.getExperienceSummary())
                .skills(candidate.getSkills())
                .domainKnowledge(candidate.getDomainKnowledge())
                .academicBackground(candidate.getAcademicBackground())
                .yearsOfExperience(candidate.getYearsOfExperience())
                .jobTitle(job.getTitle())
                .jobDescription(job.getDescription())
                .requiredSkills(job.getRequiredSkills())
                .requiredEducation(job.getRequiredEducation())
                .domainRequirements(job.getDomainRequirements())
                .minExperienceYears(job.getMinExperienceYears())
                .maxExperienceYears(job.getMaxExperienceYears())
                .enrichedProfileContext(enrichedContext)
                .build();
        return aiService.matchCandidate(matchRequest);
    }

    /**
     * Build a comprehensive explanation from AI match response.
     * 
     * @param response AI matching response
     * @return Formatted explanation text
     */
    private String buildExplanation(CandidateMatchResponse response) {
        return String.format("""
                Recommendation: %s
                
                %s
                
                Strengths:
                %s
                
                Gaps:
                %s
                """,
                response.getRecommendation(),
                response.getExplanation(),
                response.getStrengths(),
                response.getGaps()
        );
    }
}
