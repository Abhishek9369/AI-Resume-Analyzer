package io.subbu.ai.firedrill.resolver;

import io.subbu.ai.firedrill.entities.Candidate;
import io.subbu.ai.firedrill.repos.CandidateRepository;
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
 * Unit tests for CandidateResolver.
 * Tests GraphQL queries and mutations for candidates.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CandidateResolver Unit Tests")
class CandidateResolverTest {

    @Mock
    private CandidateRepository candidateRepository;

    @InjectMocks
    private CandidateResolver candidateResolver;

    private UUID candidateId;
    private Candidate mockCandidate;

    @BeforeEach
    void setUp() {
        candidateId = UUID.randomUUID();
        mockCandidate = Candidate.builder()
                .id(candidateId)
                .name("John Doe")
                .email("john.doe@email.com")
                .mobile("555-1234")
                .skills("Java, Spring Boot, Kubernetes")
                .experience(5)
                .education("Master of Science in Computer Science")
                .currentCompany("Tech Corp")
                .build();
    }

    @Test
    @DisplayName("Should fetch candidate by ID successfully")
    void shouldFetchCandidateById() {
        // Given
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(mockCandidate));

        // When
        Candidate result = candidateResolver.candidate(candidateId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(candidateId);
        assertThat(result.getName()).isEqualTo("John Doe");
        verify(candidateRepository).findById(candidateId);
    }

    @Test
    @DisplayName("Should throw exception when candidate not found")
    void shouldThrowExceptionWhenCandidateNotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(candidateRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> candidateResolver.candidate(nonExistentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Candidate not found");
        verify(candidateRepository).findById(nonExistentId);
    }

    @Test
    @DisplayName("Should fetch all candidates successfully")
    void shouldFetchAllCandidates() {
        // Given
        Candidate candidate2 = Candidate.builder()
                .id(UUID.randomUUID())
                .name("Jane Smith")
                .email("jane@email.com")
                .build();

        List<Candidate> allCandidates = List.of(mockCandidate, candidate2);
        when(candidateRepository.findAll()).thenReturn(allCandidates);

        // When
        List<Candidate> results = candidateResolver.allCandidates();

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).containsExactly(mockCandidate, candidate2);
        verify(candidateRepository).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no candidates exist")
    void shouldReturnEmptyListWhenNoCandidates() {
        // Given
        when(candidateRepository.findAll()).thenReturn(List.of());

        // When
        List<Candidate> results = candidateResolver.allCandidates();

        // Then
        assertThat(results).isEmpty();
        verify(candidateRepository).findAll();
    }

    @Test
    @DisplayName("Should search candidates by name successfully")
    void shouldSearchCandidatesByName() {
        // Given
        String searchName = "John";
        when(candidateRepository.searchByName(searchName)).thenReturn(List.of(mockCandidate));

        // When
        List<Candidate> results = candidateResolver.searchCandidatesByName(searchName);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).contains("John");
        verify(candidateRepository).searchByName(searchName);
    }

    @Test
    @DisplayName("Should search candidates by skill successfully")
    void shouldSearchCandidatesBySkill() {
        // Given
        String skill = "Java";
        when(candidateRepository.findBySkillsContaining(skill)).thenReturn(List.of(mockCandidate));

        // When
        List<Candidate> results = candidateResolver.searchCandidatesBySkill(skill);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSkills()).contains("Java");
        verify(candidateRepository).findBySkillsContaining(skill);
    }

    @Test
    @DisplayName("Should return empty list when no candidates match skill search")
    void shouldReturnEmptyListWhenNoSkillMatch() {
        // Given
        String skill = "Rust";
        when(candidateRepository.findBySkillsContaining(skill)).thenReturn(List.of());

        // When
        List<Candidate> results = candidateResolver.searchCandidatesBySkill(skill);

        // Then
        assertThat(results).isEmpty();
        verify(candidateRepository).findBySkillsContaining(skill);
    }

    @Test
    @DisplayName("Should update candidate successfully")
    void shouldUpdateCandidateSuccessfully() {
        // Given
        String newName = "John Updated";
        String newEmail = "john.updated@email.com";
        String newSkills = "Java, Python, Kubernetes";

        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(mockCandidate));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Candidate result = candidateResolver.updateCandidate(
                candidateId, newName, newEmail, null, newSkills, 6, null, null);

        // Then
        assertThat(result.getName()).isEqualTo(newName);
        assertThat(result.getEmail()).isEqualTo(newEmail);
        assertThat(result.getSkills()).isEqualTo(newSkills);
        assertThat(result.getExperience()).isEqualTo(6);
        verify(candidateRepository).findById(candidateId);
        verify(candidateRepository).save(any(Candidate.class));
    }

    @Test
    @DisplayName("Should update only provided fields")
    void shouldUpdateOnlyProvidedFields() {
        // Given
        String originalEmail = mockCandidate.getEmail();
        String originalMobile = mockCandidate.getMobile();
        String newName = "John Updated";

        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(mockCandidate));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Candidate result = candidateResolver.updateCandidate(
                candidateId, newName, null, null, null, null, null, null);

        // Then
        assertThat(result.getName()).isEqualTo(newName);
        assertThat(result.getEmail()).isEqualTo(originalEmail); // Unchanged
        assertThat(result.getMobile()).isEqualTo(originalMobile); // Unchanged
        verify(candidateRepository).save(any(Candidate.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent candidate")
    void shouldThrowExceptionWhenUpdatingNonExistentCandidate() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(candidateRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> candidateResolver.updateCandidate(
                nonExistentId, "New Name", null, null, null, null, null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Candidate not found");
        verify(candidateRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete candidate successfully")
    void shouldDeleteCandidateSuccessfully() {
        // Given
        doNothing().when(candidateRepository).deleteById(candidateId);

        // When
        Boolean result = candidateResolver.deleteCandidate(candidateId);

        // Then
        assertThat(result).isTrue();
        verify(candidateRepository).deleteById(candidateId);
    }

    @Test
    @DisplayName("Should handle delete of non-existent candidate")
    void shouldHandleDeleteOfNonExistentCandidate() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        doNothing().when(candidateRepository).deleteById(nonExistentId);

        // When
        Boolean result = candidateResolver.deleteCandidate(nonExistentId);

        // Then
        assertThat(result).isTrue();
        verify(candidateRepository).deleteById(nonExistentId);
    }

    @Test
    @DisplayName("Should update all candidate fields when provided")
    void shouldUpdateAllFieldsWhenProvided() {
        // Given
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(mockCandidate));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Candidate result = candidateResolver.updateCandidate(
                candidateId,
                "New Name",
                "new@email.com",
                "555-9999",
                "Python, Go",
                10,
                "PhD in AI",
                "New Company"
        );

        // Then
        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getEmail()).isEqualTo("new@email.com");
        assertThat(result.getMobile()).isEqualTo("555-9999");
        assertThat(result.getSkills()).isEqualTo("Python, Go");
        assertThat(result.getExperience()).isEqualTo(10);
        assertThat(result.getEducation()).isEqualTo("PhD in AI");
        assertThat(result.getCurrentCompany()).isEqualTo("New Company");
        verify(candidateRepository).save(any(Candidate.class));
    }
}
