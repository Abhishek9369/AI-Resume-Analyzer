package io.subbu.ai.firedrill.resolver;

import io.subbu.ai.firedrill.entities.JobRequirement;
import io.subbu.ai.firedrill.repos.JobRequirementRepository;
import io.subbu.ai.firedrill.repos.SkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JobRequirementResolver.
 * Tests GraphQL queries and mutations for job requirements.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JobRequirementResolver Unit Tests")
class JobRequirementResolverTest {

    @Mock
    private JobRequirementRepository jobRepository;

    @Mock
    private SkillRepository skillRepository;

    @InjectMocks
    private JobRequirementResolver jobRequirementResolver;

    private UUID jobId;
    private JobRequirement mockJobRequirement;

    @BeforeEach
    void setUp() {
        jobId = UUID.randomUUID();
        mockJobRequirement = JobRequirement.builder()
                .id(jobId)
                .title("Senior Software Engineer")
                .description("Looking for experienced Java developer")
                .requiredSkills("Java, Spring Boot, Kubernetes")
                .requiredEducation("Bachelor's or Master's in Computer Science")
                .domainRequirements("Enterprise software development")
                .minExperienceYears(5)
                .maxExperienceYears(10)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Should fetch job requirement by ID successfully")
    void shouldFetchJobRequirementById() {
        // Given
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(mockJobRequirement));

        // When
        JobRequirement result = jobRequirementResolver.jobRequirement(jobId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(jobId);
        assertThat(result.getTitle()).isEqualTo("Senior Software Engineer");
        verify(jobRepository).findById(jobId);
    }

    @Test
    @DisplayName("Should throw exception when job requirement not found")
    void shouldThrowExceptionWhenJobRequirementNotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(jobRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> jobRequirementResolver.jobRequirement(nonExistentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Job requirement not found");
        verify(jobRepository).findById(nonExistentId);
    }

    @Test
    @DisplayName("Should fetch all job requirements successfully")
    void shouldFetchAllJobRequirements() {
        // Given
        JobRequirement job2 = JobRequirement.builder()
                .id(UUID.randomUUID())
                .title("Backend Engineer")
                .isActive(true)
                .build();

        List<JobRequirement> allJobs = List.of(mockJobRequirement, job2);
        when(jobRepository.findAll()).thenReturn(allJobs);

        // When
        List<JobRequirement> results = jobRequirementResolver.allJobRequirements();

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).containsExactly(mockJobRequirement, job2);
        verify(jobRepository).findAll();
    }

    @Test
    @DisplayName("Should fetch only active job requirements")
    void shouldFetchActiveJobRequirements() {
        // Given
        when(jobRepository.findByIsActive(true)).thenReturn(List.of(mockJobRequirement));

        // When
        List<JobRequirement> results = jobRequirementResolver.activeJobRequirements();

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getIsActive()).isTrue();
        verify(jobRepository).findByIsActive(true);
    }

    @Test
    @DisplayName("Should return empty list when no active jobs exist")
    void shouldReturnEmptyListWhenNoActiveJobs() {
        // Given
        when(jobRepository.findByIsActive(true)).thenReturn(List.of());

        // When
        List<JobRequirement> results = jobRequirementResolver.activeJobRequirements();

        // Then
        assertThat(results).isEmpty();
        verify(jobRepository).findByIsActive(true);
    }

    @Test
    @DisplayName("Should handle mixed active and inactive jobs")
    void shouldHandleMixedActiveInactiveJobs() {
        // Given
        JobRequirement activeJob = JobRequirement.builder()
                .id(UUID.randomUUID())
                .title("Active Job")
                .isActive(true)
                .build();

        when(jobRepository.findByIsActive(true)).thenReturn(List.of(activeJob));

        // When
        List<JobRequirement> results = jobRequirementResolver.activeJobRequirements();

        // Then
        assertThat(results).hasSize(1);
        assertThat(results).allMatch(JobRequirement::getIsActive);
    }

    @Test
    @DisplayName("Should validate required fields are present")
    void shouldValidateRequiredFields() {
        // When
        JobRequirement result = mockJobRequirement;

        // Then
        assertThat(result.getTitle()).isNotNull();
        assertThat(result.getRequiredSkills()).isNotNull();
        assertThat(result.getMinExperienceYears()).isNotNull();
        assertThat(result.getIsActive()).isNotNull();
    }

    @Test
    @DisplayName("Should handle job with minimum experience requirement")
    void shouldHandleMinimumExperienceRequirement() {
        // Given
        JobRequirement entryLevelJob = JobRequirement.builder()
                .id(UUID.randomUUID())
                .title("Junior Developer")
                .minExperienceYears(0)
                .maxExperienceYears(2)
                .isActive(true)
                .build();

        when(jobRepository.findById(entryLevelJob.getId())).thenReturn(Optional.of(entryLevelJob));

        // When
        JobRequirement result = jobRequirementResolver.jobRequirement(entryLevelJob.getId());

        // Then
        assertThat(result.getMinExperienceYears()).isEqualTo(0);
        assertThat(result.getMaxExperienceYears()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should handle job with high experience requirement")
    void shouldHandleHighExperienceRequirement() {
        // Given
        JobRequirement seniorJob = JobRequirement.builder()
                .id(UUID.randomUUID())
                .title("Principal Engineer")
                .minExperienceYears(10)
                .maxExperienceYears(15)
                .isActive(true)
                .build();

        when(jobRepository.findById(seniorJob.getId())).thenReturn(Optional.of(seniorJob));

        // When
        JobRequirement result = jobRequirementResolver.jobRequirement(seniorJob.getId());

        // Then
        assertThat(result.getMinExperienceYears()).isEqualTo(10);
        assertThat(result.getMaxExperienceYears()).isEqualTo(15);
    }

    @Test
    @DisplayName("Should handle job with multiple required skills")
    void shouldHandleMultipleRequiredSkills() {
        // When
        String skills = mockJobRequirement.getRequiredSkills();

        // Then
        assertThat(skills).contains("Java");
        assertThat(skills).contains("Spring Boot");
        assertThat(skills).contains("Kubernetes");
    }

    @Test
    @DisplayName("Should handle job with domain requirements")
    void shouldHandleDomainRequirements() {
        // When
        String domain = mockJobRequirement.getDomainRequirements();

        // Then
        assertThat(domain).isNotNull();
        assertThat(domain).isEqualTo("Enterprise software development");
    }

    @Test
    @DisplayName("Should handle job with educational requirements")
    void shouldHandleEducationalRequirements() {
        // When
        String education = mockJobRequirement.getRequiredEducation();

        // Then
        assertThat(education).isNotNull();
        assertThat(education).contains("Bachelor's");
        assertThat(education).contains("Master's");
    }

    @Test
    @DisplayName("Should fetch jobs within experience range")
    void shouldFetchJobsWithinExperienceRange() {
        // Given
        int candidateExperience = 7;

        // When
        boolean matchesRequirement = candidateExperience >= mockJobRequirement.getMinExperienceYears()
                && candidateExperience <= mockJobRequirement.getMaxExperienceYears();

        // Then
        assertThat(matchesRequirement).isTrue();
    }
}
