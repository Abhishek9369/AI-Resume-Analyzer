package io.subbu.ai.firedrill.services;

import io.subbu.ai.firedrill.entities.Candidate;
import io.subbu.ai.firedrill.entities.CandidateMatch;
import io.subbu.ai.firedrill.entities.JobRequirement;
import io.subbu.ai.firedrill.models.CandidateMatchResponse;
import io.subbu.ai.firedrill.repos.CandidateMatchRepository;
import io.subbu.ai.firedrill.repos.CandidateRepository;
import io.subbu.ai.firedrill.repos.JobRequirementRepository;
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
 * Unit tests for CandidateMatchingService.
 * Tests matching logic, scoring calculation, and repository interactions.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CandidateMatchingService Unit Tests")
class CandidateMatchingServiceTest {

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private JobRequirementRepository jobRequirementRepository;

    @Mock
    private CandidateMatchRepository matchRepository;

    @Mock
    private AIService aiService;

    @InjectMocks
    private CandidateMatchingService candidateMatchingService;

    private UUID candidateId;
    private UUID jobRequirementId;
    private Candidate mockCandidate;
    private JobRequirement mockJobRequirement;
    private CandidateMatchResponse mockMatchResponse;

    @BeforeEach
    void setUp() {
        candidateId = UUID.randomUUID();
        jobRequirementId = UUID.randomUUID();

        mockCandidate = Candidate.builder()
                .id(candidateId)
                .name("John Doe")
                .email("john.doe@email.com")
                .experienceSummary("Senior Software Engineer with 5 years of experience")
                .skills("Java, Spring Boot, Kubernetes, PostgreSQL")
                .domainKnowledge("Enterprise software, Cloud architecture")
                .academicBackground("Master of Science in Computer Science, MIT")
                .yearsOfExperience(5)
                .build();

        mockJobRequirement = JobRequirement.builder()
                .id(jobRequirementId)
                .title("Lead Software Engineer")
                .description("Looking for experienced engineer to lead microservices development")
                .requiredSkills("Java, Spring Boot, Kubernetes, Docker")
                .requiredEducation("Bachelor's or Master's in Computer Science")
                .domainRequirements("Enterprise software development")
                .minExperienceYears(4)
                .maxExperienceYears(8)
                .isActive(true)
                .build();

        mockMatchResponse = CandidateMatchResponse.builder()
                .matchScore(92.0)
                .skillsScore(95.0)
                .experienceScore(90.0)
                .educationScore(100.0)
                .domainScore(85.0)
                .explanation("Excellent match with strong technical skills")
                .strengths("Strong Java/Spring Boot expertise, proven leadership")
                .gaps("Could benefit from more Docker experience")
                .recommendation("Strong Match")
                .build();
    }

    @Test
    @DisplayName("Should successfully match candidate to job")
    void shouldMatchCandidateToJobSuccessfully() {
        // Given
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(mockCandidate));
        when(jobRequirementRepository.findById(jobRequirementId)).thenReturn(Optional.of(mockJobRequirement));
        when(matchRepository.findByCandidateIdAndJobRequirementId(candidateId, jobRequirementId))
                .thenReturn(Optional.empty());
        when(aiService.matchCandidate(any())).thenReturn(mockMatchResponse);
        when(matchRepository.save(any(CandidateMatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CandidateMatch result = candidateMatchingService.matchCandidateToJob(candidateId, jobRequirementId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMatchScore()).isEqualTo(92.0);
        assertThat(result.getSkillsScore()).isEqualTo(95.0);
        assertThat(result.getExperienceScore()).isEqualTo(90.0);
        assertThat(result.getIsShortlisted()).isTrue(); // Score >= 70

        verify(candidateRepository).findById(candidateId);
        verify(jobRequirementRepository).findById(jobRequirementId);
        verify(matchRepository).findByCandidateIdAndJobRequirementId(candidateId, jobRequirementId);
        verify(aiService).matchCandidate(any());
        verify(matchRepository).save(any(CandidateMatch.class));
    }

    @Test
    @DisplayName("Should update existing match with new scores")
    void shouldUpdateExistingMatch() {
        // Given
        CandidateMatch existingMatch = CandidateMatch.builder()
                .id(UUID.randomUUID())
                .candidate(mockCandidate)
                .jobRequirement(mockJobRequirement)
                .matchScore(75.0)
                .skillsScore(80.0)
                .experienceScore(70.0)
                .educationScore(75.0)
                .domainScore(75.0)
                .isSelected(false)
                .isShortlisted(true)
                .build();

        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(mockCandidate));
        when(jobRequirementRepository.findById(jobRequirementId)).thenReturn(Optional.of(mockJobRequirement));
        when(matchRepository.findByCandidateIdAndJobRequirementId(candidateId, jobRequirementId))
                .thenReturn(Optional.of(existingMatch));
        when(aiService.matchCandidate(any())).thenReturn(mockMatchResponse);
        when(matchRepository.save(any(CandidateMatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CandidateMatch result = candidateMatchingService.matchCandidateToJob(candidateId, jobRequirementId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMatchScore()).isEqualTo(92.0); // Updated score
        assertThat(result.getSkillsScore()).isEqualTo(95.0);

        verify(matchRepository).save(any(CandidateMatch.class));
    }

    @Test
    @DisplayName("Should throw exception when candidate not found")
    void shouldThrowExceptionWhenCandidateNotFound() {
        // Given
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> candidateMatchingService.matchCandidateToJob(candidateId, jobRequirementId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Candidate not found");

        verify(candidateRepository).findById(candidateId);
        verify(jobRequirementRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should throw exception when job requirement not found")
    void shouldThrowExceptionWhenJobRequirementNotFound() {
        // Given
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(mockCandidate));
        when(jobRequirementRepository.findById(jobRequirementId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> candidateMatchingService.matchCandidateToJob(candidateId, jobRequirementId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Job requirement not found");

        verify(jobRequirementRepository).findById(jobRequirementId);
    }

    @Test
    @DisplayName("Should match all candidates to a job")
    void shouldMatchAllCandidatesToJob() {
        // Given
        Candidate candidate2 = Candidate.builder()
                .id(UUID.randomUUID())
                .name("Jane Smith")
                .experienceSummary("Software Developer with 3 years")
                .skills("Python, Django, PostgreSQL")
                .yearsOfExperience(3)
                .build();

        List<Candidate> allCandidates = List.of(mockCandidate, candidate2);

        when(jobRequirementRepository.findById(jobRequirementId)).thenReturn(Optional.of(mockJobRequirement));
        when(candidateRepository.findAll()).thenReturn(allCandidates);
        when(matchRepository.findByCandidateIdAndJobRequirementId(any(), any()))
                .thenReturn(Optional.empty());
        when(aiService.matchCandidate(any())).thenReturn(mockMatchResponse);
        when(matchRepository.save(any(CandidateMatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<CandidateMatch> results = candidateMatchingService.matchAllCandidatesToJob(jobRequirementId);

        // Then
        assertThat(results).hasSize(2);
        verify(candidateRepository).findAll();
        verify(aiService, times(2)).matchCandidate(any());
        verify(matchRepository, times(2)).save(any(CandidateMatch.class));
    }

    @Test
    @DisplayName("Should match candidate to all active jobs")
    void shouldMatchCandidateToAllActiveJobs() {
        // Given
        JobRequirement job2 = JobRequirement.builder()
                .id(UUID.randomUUID())
                .title("Senior Backend Engineer")
                .requiredSkills("Java, Microservices")
                .isActive(true)
                .build();

        List<JobRequirement> activeJobs = List.of(mockJobRequirement, job2);

        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(mockCandidate));
        when(jobRequirementRepository.findByIsActive(true)).thenReturn(activeJobs);
        when(matchRepository.findByCandidateIdAndJobRequirementId(any(), any()))
                .thenReturn(Optional.empty());
        when(aiService.matchCandidate(any())).thenReturn(mockMatchResponse);
        when(matchRepository.save(any(CandidateMatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<CandidateMatch> results = candidateMatchingService.matchCandidateToAllJobs(candidateId);

        // Then
        assertThat(results).hasSize(2);
        verify(jobRequirementRepository).findByIsActive(true);
        verify(aiService, times(2)).matchCandidate(any());
    }

    @Test
    @DisplayName("Should auto-shortlist candidates with high scores")
    void shouldAutoShortlistHighScoringCandidates() {
        // Given
        CandidateMatchResponse highScoreResponse = CandidateMatchResponse.builder()
                .matchScore(85.0)
                .skillsScore(90.0)
                .experienceScore(80.0)
                .educationScore(85.0)
                .domainScore(85.0)
                .explanation("Very strong match")
                .strengths("Excellent technical skills")
                .gaps("Minor gaps in specific tools")
                .recommendation("Strong Match")
                .build();

        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(mockCandidate));
        when(jobRequirementRepository.findById(jobRequirementId)).thenReturn(Optional.of(mockJobRequirement));
        when(matchRepository.findByCandidateIdAndJobRequirementId(candidateId, jobRequirementId))
                .thenReturn(Optional.empty());
        when(aiService.matchCandidate(any())).thenReturn(highScoreResponse);
        when(matchRepository.save(any(CandidateMatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CandidateMatch result = candidateMatchingService.matchCandidateToJob(candidateId, jobRequirementId);

        // Then
        assertThat(result.getIsShortlisted()).isTrue(); // Score >= 70
        assertThat(result.getMatchScore()).isEqualTo(85.0);
    }

    @Test
    @DisplayName("Should not shortlist candidates with low scores")
    void shouldNotShortlistLowScoringCandidates() {
        // Given
        CandidateMatchResponse lowScoreResponse = CandidateMatchResponse.builder()
                .matchScore(45.0)
                .skillsScore(40.0)
                .experienceScore(50.0)
                .educationScore(45.0)
                .domainScore(40.0)
                .explanation("Limited match")
                .strengths("Some basic skills")
                .gaps("Significant skill gaps, insufficient experience")
                .recommendation("No Match")
                .build();

        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(mockCandidate));
        when(jobRequirementRepository.findById(jobRequirementId)).thenReturn(Optional.of(mockJobRequirement));
        when(matchRepository.findByCandidateIdAndJobRequirementId(candidateId, jobRequirementId))
                .thenReturn(Optional.empty());
        when(aiService.matchCandidate(any())).thenReturn(lowScoreResponse);
        when(matchRepository.save(any(CandidateMatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CandidateMatch result = candidateMatchingService.matchCandidateToJob(candidateId, jobRequirementId);

        // Then
        assertThat(result.getIsShortlisted()).isFalse(); // Score < 70
        assertThat(result.getMatchScore()).isLessThan(70.0);
    }

    @Test
    @DisplayName("Should handle matching errors gracefully when matching all candidates")
    void shouldHandleMatchingErrorsGracefully() {
        // Given
        Candidate candidate2 = Candidate.builder()
                .id(UUID.randomUUID())
                .name("Error Candidate")
                .build();

        List<Candidate> candidates = List.of(mockCandidate, candidate2);

        when(jobRequirementRepository.findById(jobRequirementId)).thenReturn(Optional.of(mockJobRequirement));
        when(candidateRepository.findAll()).thenReturn(candidates);
        when(matchRepository.findByCandidateIdAndJobRequirementId(mockCandidate.getId(), jobRequirementId))
                .thenReturn(Optional.empty());
        when(matchRepository.findByCandidateIdAndJobRequirementId(candidate2.getId(), jobRequirementId))
                .thenReturn(Optional.empty());
        when(aiService.matchCandidate(any()))
                .thenReturn(mockMatchResponse)
                .thenThrow(new RuntimeException("AI service error"));
        when(matchRepository.save(any(CandidateMatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<CandidateMatch> results = candidateMatchingService.matchAllCandidatesToJob(jobRequirementId);

        // Then - Should continue processing despite error
        assertThat(results).hasSize(1); // Only successful match
        verify(aiService, times(2)).matchCandidate(any());
    }
}
