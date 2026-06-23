package io.subbu.ai.firedrill.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.subbu.ai.firedrill.entities.CandidateMatch;
import io.subbu.ai.firedrill.entities.MatchAudit;
import io.subbu.ai.firedrill.repos.MatchAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for creating and managing match audit records.
 * Audit completion is done asynchronously to avoid impacting match performance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchAuditService {

    private final MatchAuditRepository matchAuditRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Create an IN_PROGRESS audit record at the start of a matching run.
     * This is synchronous so the audit ID is available immediately.
     */
    @Transactional
    public MatchAudit createAudit(UUID jobRequirementId, String jobTitle, String initiatedBy) {
        MatchAudit audit = MatchAudit.builder()
                .jobRequirementId(jobRequirementId)
                .jobTitle(jobTitle)
                .status("IN_PROGRESS")
                .initiatedBy(initiatedBy)
                .initiatedAt(LocalDateTime.now())
                .build();
        MatchAudit saved = matchAuditRepository.save(audit);
        log.info("Created match audit {} for job '{}'", saved.getId(), jobTitle);
        return saved;
    }

    /**
     * Complete an audit record asynchronously after a successful matching run.
     */
    @Async
    @Transactional
    public void completeAudit(UUID auditId, List<CandidateMatch> matches, long durationMs) {
        try {
            matchAuditRepository.findById(auditId).ifPresent(audit -> {
                int total = matches.size();
                int shortlisted = (int) matches.stream()
                        .filter(m -> Boolean.TRUE.equals(m.getIsShortlisted()))
                        .count();

                OptionalStats stats = computeStats(matches);

                // Estimate tokens: ~1500 per candidate (prompt + completion)
                int estimatedTokens = total * 1500;

                audit.setTotalCandidates(total);
                audit.setSuccessfulMatches(total);
                audit.setShortlistedCount(shortlisted);
                audit.setAverageMatchScore(stats.avg);
                audit.setHighestMatchScore(stats.max);
                audit.setDurationMs(durationMs);
                audit.setEstimatedTokensUsed(estimatedTokens);
                audit.setStatus("COMPLETED");
                audit.setCompletedAt(LocalDateTime.now());
                audit.setMatchSummaries(buildMatchSummaries(matches));

                matchAuditRepository.save(audit);
                log.info("Completed match audit {} — {} candidates, avg score {:.1f}%, {}ms",
                        auditId, total, String.format("%.1f", stats.avg), durationMs);
            });
        } catch (Exception e) {
            log.error("Failed to complete match audit {}: {}", auditId, e.getMessage(), e);
        }
    }

    /**
     * Mark an audit record as FAILED asynchronously.
     */
    @Async
    @Transactional
    public void failAudit(UUID auditId, String errorMessage, long durationMs) {
        try {
            matchAuditRepository.findById(auditId).ifPresent(audit -> {
                audit.setStatus("FAILED");
                audit.setErrorMessage(errorMessage);
                audit.setDurationMs(durationMs);
                audit.setCompletedAt(LocalDateTime.now());
                matchAuditRepository.save(audit);
                log.info("Failed match audit {} after {}ms: {}", auditId, durationMs, errorMessage);
            });
        } catch (Exception e) {
            log.error("Failed to record audit failure {}: {}", auditId, e.getMessage(), e);
        }
    }

    // ----- helpers -----

    private record OptionalStats(double avg, double max) {}

    private OptionalStats computeStats(List<CandidateMatch> matches) {
        double avg = matches.stream()
                .filter(m -> m.getMatchScore() != null)
                .mapToDouble(CandidateMatch::getMatchScore)
                .average()
                .orElse(0.0);
        double max = matches.stream()
                .filter(m -> m.getMatchScore() != null)
                .mapToDouble(CandidateMatch::getMatchScore)
                .max()
                .orElse(0.0);
        return new OptionalStats(avg, max);
    }

    private String buildMatchSummaries(List<CandidateMatch> matches) {
        try {
            List<Map<String, Object>> summaries = matches.stream()
                    .map(m -> Map.<String, Object>of(
                            "candidateId",   m.getCandidate() != null ? m.getCandidate().getId().toString() : "",
                            "candidateName", m.getCandidate() != null ? m.getCandidate().getName() : "Unknown",
                            "matchScore",    m.getMatchScore() != null ? m.getMatchScore() : 0.0,
                            "skillsScore",   m.getSkillsScore() != null ? m.getSkillsScore() : 0.0,
                            "isShortlisted", Boolean.TRUE.equals(m.getIsShortlisted())
                    ))
                    .toList();
            return objectMapper.writeValueAsString(summaries);
        } catch (JsonProcessingException e) {
            log.warn("Could not serialize match summaries: {}", e.getMessage());
            return "[]";
        }
    }
}
