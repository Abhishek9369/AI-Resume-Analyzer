package io.subbu.ai.firedrill.resolver;

import io.subbu.ai.firedrill.entities.ProcessTracker;
import io.subbu.ai.firedrill.repos.ProcessTrackerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * GraphQL resolver for ProcessTracker queries.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ProcessTrackerResolver {

    private final ProcessTrackerRepository trackerRepository;

    @QueryMapping
    public ProcessTracker processStatus(@Argument UUID trackerId) {
        log.info("Fetching process status: {}", trackerId);
        return trackerRepository.findById(trackerId)
                .orElseThrow(() -> new RuntimeException("Process tracker not found: " + trackerId));
    }

    @QueryMapping
    public List<ProcessTracker> recentProcessTrackers(@Argument int hours) {
        log.info("Fetching process trackers from the last {} hours", hours);
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hours);
        return trackerRepository.findByCreatedAtAfter(cutoffTime);
    }
}
