package io.subbu.ai.firedrill.resolver;

import io.subbu.ai.firedrill.entities.MatchAudit;
import io.subbu.ai.firedrill.repos.MatchAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

/**
 * GraphQL resolver for MatchAudit queries (admin monitoring).
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class MatchAuditResolver {

    private final MatchAuditRepository matchAuditRepository;

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<MatchAudit> matchAudits(@Argument Integer limit) {
        log.info("Fetching match audits, limit={}", limit);
        if (limit != null && limit > 0) {
            return matchAuditRepository.findTop50ByOrderByInitiatedAtDesc()
                    .stream().limit(limit).toList();
        }
        return matchAuditRepository.findTop50ByOrderByInitiatedAtDesc();
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<MatchAudit> matchAuditsForJob(@Argument UUID jobRequirementId) {
        log.info("Fetching match audits for job: {}", jobRequirementId);
        return matchAuditRepository.findByJobRequirementIdOrderByInitiatedAtDesc(jobRequirementId);
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<MatchAudit> activeMatchRuns() {
        log.info("Fetching active match runs");
        return matchAuditRepository.findByStatusOrderByInitiatedAtDesc("IN_PROGRESS");
    }
}
