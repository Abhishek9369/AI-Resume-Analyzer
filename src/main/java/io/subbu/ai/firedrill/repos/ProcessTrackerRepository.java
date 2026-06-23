package io.subbu.ai.firedrill.repos;

import io.subbu.ai.firedrill.entities.ProcessTracker;
import io.subbu.ai.firedrill.models.ProcessStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for ProcessTracker entity operations.
 * Manages tracking records for async resume processing jobs.
 */
@Repository
public interface ProcessTrackerRepository extends JpaRepository<ProcessTracker, UUID> {

    /**
     * Find process tracker by status
     * 
     * @param status The process status to filter by
     * @return List of process trackers with the specified status
     */
    List<ProcessTracker> findByStatus(ProcessStatus status);

    /**
     * Find process trackers created after a specific date
     * 
     * @param dateTime The cutoff datetime
     * @return List of recent process trackers
     */
    List<ProcessTracker> findByCreatedAtAfter(LocalDateTime dateTime);

    /**
     * Find the most recent process tracker for a file
     * 
     * @param filename The uploaded filename
     * @return Optional containing the latest tracker for the file
     */
    Optional<ProcessTracker> findFirstByUploadedFilenameOrderByCreatedAtDesc(String filename);

    /**
     * Find incomplete (not completed or failed) process trackers
     * 
     * @param statuses List of statuses to exclude
     * @return List of in-progress trackers
     */
    List<ProcessTracker> findByStatusNotIn(List<ProcessStatus> statuses);

    /**
     * Find all process trackers ordered by creation date descending
     * 
     * @return List of all process trackers sorted by most recent first
     */
    @Query("SELECT pt FROM ProcessTracker pt ORDER BY pt.createdAt DESC")
    List<ProcessTracker> findAllOrderByCreatedAtDesc();
}
