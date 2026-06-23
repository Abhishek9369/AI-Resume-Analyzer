package io.subbu.ai.firedrill.resolver;

import io.subbu.ai.firedrill.entities.CandidateMatch;
import io.subbu.ai.firedrill.models.UpdateCandidateMatchInput;
import io.subbu.ai.firedrill.repos.CandidateMatchRepository;
import io.subbu.ai.firedrill.services.CandidateMatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

/**
 * GraphQL resolver for CandidateMatch queries and mutations.
 * Argument names match the GraphQL schema exactly.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class CandidateMatchResolver {

    private final CandidateMatchRepository matchRepository;
    private final CandidateMatchingService matchingService;

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER', 'HIRING_MANAGER')")
    public List<CandidateMatch> matchesForJob(@Argument UUID jobRequirementId, @Argument Integer limit) {
        log.info("Fetching matches for job: {}", jobRequirementId);
        if (limit == null) limit = 50;
        return matchRepository.topMatchesForJob(jobRequirementId, limit);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER', 'HIRING_MANAGER')")
    public List<CandidateMatch> matchesForCandidate(@Argument UUID candidateId) {
        log.info("Fetching matches for candidate: {}", candidateId);
        return matchRepository.findByCandidateId(candidateId);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER', 'HIRING_MANAGER')")
    public List<CandidateMatch> shortlistedCandidatesForJob(@Argument UUID jobRequirementId) {
        log.info("Fetching shortlisted candidates for job: {}", jobRequirementId);
        return matchRepository.findByJobRequirementIdAndIsShortlisted(jobRequirementId, true);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER', 'HIRING_MANAGER')")
    public List<CandidateMatch> selectedCandidatesForJob(@Argument UUID jobRequirementId) {
        log.info("Fetching selected candidates for job: {}", jobRequirementId);
        return matchRepository.findByJobRequirementIdAndIsSelected(jobRequirementId, true);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER', 'HIRING_MANAGER')")
    public List<CandidateMatch> topMatchesForJob(@Argument UUID jobRequirementId, @Argument Integer limit, @Argument Double minScore) {
        log.info("Fetching top matches for job: {} limit: {} minScore: {}", jobRequirementId, limit, minScore);
        if (limit == null) limit = 10;
        List<CandidateMatch> matches = matchRepository.topMatchesForJob(jobRequirementId, limit);
        if (minScore != null) {
            return matches.stream()
                    .filter(m -> m.getMatchScore() != null && m.getMatchScore() >= minScore)
                    .toList();
        }
        return matches;
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    public CandidateMatch matchCandidateToJob(@Argument UUID candidateId, @Argument UUID jobRequirementId) {
        log.info("Matching candidate {} to job {}", candidateId, jobRequirementId);
        return matchingService.matchCandidateToJob(candidateId, jobRequirementId);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    public List<CandidateMatch> matchAllCandidatesToJob(@Argument UUID jobRequirementId) {
        log.info("Matching all candidates to job: {}", jobRequirementId);
        return matchingService.matchAllCandidatesToJob(jobRequirementId);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    public List<CandidateMatch> matchCandidateToAllJobs(@Argument UUID candidateId) {
        log.info("Matching candidate to all jobs: {}", candidateId);
        return matchingService.matchCandidateToAllJobs(candidateId);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER', 'HIRING_MANAGER')")
    public CandidateMatch updateCandidateMatch(@Argument UUID matchId, @Argument UpdateCandidateMatchInput input) {
        log.info("Updating candidate match: {}", matchId);
        CandidateMatch match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found: " + matchId));

        if (input.isSelected() != null) match.setIsSelected(input.isSelected());
        if (input.isShortlisted() != null) match.setIsShortlisted(input.isShortlisted());
        if (input.recruiterNotes() != null) match.setRecruiterNotes(input.recruiterNotes());

        return matchRepository.save(match);
    }
}
