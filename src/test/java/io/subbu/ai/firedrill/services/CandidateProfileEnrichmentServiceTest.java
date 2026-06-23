package io.subbu.ai.firedrill.services;

import io.subbu.ai.firedrill.entities.Candidate;
import io.subbu.ai.firedrill.entities.CandidateExternalProfile;
import io.subbu.ai.firedrill.entities.ExternalProfileSource;
import io.subbu.ai.firedrill.repos.CandidateExternalProfileRepository;
import io.subbu.ai.firedrill.repos.CandidateRepository;
import io.subbu.ai.firedrill.services.enrichers.GitHubProfileEnricher;
import io.subbu.ai.firedrill.services.enrichers.InternetSearchProfileEnricher;
import io.subbu.ai.firedrill.services.enrichers.LinkedInProfileEnricher;
import io.subbu.ai.firedrill.services.enrichers.ProfileEnricher;
import io.subbu.ai.firedrill.services.enrichers.TwitterProfileEnricher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CandidateProfileEnrichmentService}.
 *
 * All enrichers are mocked — these tests verify the orchestration logic only:
 * routing, URL-based detection, error handling, and context building.
 */
@ExtendWith(MockitoExtension.class)
class CandidateProfileEnrichmentServiceTest {

    @Mock CandidateRepository candidateRepository;
    @Mock CandidateExternalProfileRepository externalProfileRepository;
    @Mock GitHubProfileEnricher githubEnricher;
    @Mock LinkedInProfileEnricher linkedInEnricher;
    @Mock TwitterProfileEnricher twitterEnricher;
    @Mock InternetSearchProfileEnricher internetSearchEnricher;

    CandidateProfileEnrichmentService service;

    Candidate candidate;
    UUID candidateId;

    @BeforeEach
    void setUp() {
        // Wire mock enrichers to their source enum values
        when(githubEnricher.getSource()).thenReturn(ExternalProfileSource.GITHUB);
        when(linkedInEnricher.getSource()).thenReturn(ExternalProfileSource.LINKEDIN);
        when(twitterEnricher.getSource()).thenReturn(ExternalProfileSource.TWITTER);
        when(internetSearchEnricher.getSource()).thenReturn(ExternalProfileSource.INTERNET_SEARCH);

        List<ProfileEnricher> enrichers = List.of(
                githubEnricher, linkedInEnricher, twitterEnricher, internetSearchEnricher);

        service = new CandidateProfileEnrichmentService(
                externalProfileRepository, candidateRepository, enrichers);

        // Clear the getSource() invocations made during service construction
        clearInvocations(githubEnricher, linkedInEnricher, twitterEnricher, internetSearchEnricher);

        candidateId = UUID.randomUUID();
        candidate = Candidate.builder().id(candidateId).name("Jane Doe").email("jane@example.com").build();
    }

    // =========================================================================
    // enrichProfile — routing by source
    // =========================================================================

    @Nested
    @DisplayName("enrichProfile — routing by source enum")
    class EnrichProfileRouting {

        @Test
        @DisplayName("routes GITHUB to GitHubProfileEnricher")
        void routesGitHub() {
            CandidateExternalProfile expected = profile(ExternalProfileSource.GITHUB, "SUCCESS");
            when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
            when(externalProfileRepository.findByCandidateIdAndSource(candidateId, ExternalProfileSource.GITHUB))
                    .thenReturn(Optional.of(expected));
            when(githubEnricher.enrich(any(), any())).thenReturn(expected);

            CandidateExternalProfile result = service.enrichProfile(candidateId, ExternalProfileSource.GITHUB);

            assertThat(result).isSameAs(expected);
            verify(githubEnricher).enrich(expected, candidate);
            verifyNoInteractions(linkedInEnricher, twitterEnricher, internetSearchEnricher);
        }

        @Test
        @DisplayName("routes LINKEDIN to LinkedInProfileEnricher")
        void routesLinkedIn() {
            CandidateExternalProfile expected = profile(ExternalProfileSource.LINKEDIN, "NOT_AVAILABLE");
            when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
            when(externalProfileRepository.findByCandidateIdAndSource(candidateId, ExternalProfileSource.LINKEDIN))
                    .thenReturn(Optional.of(expected));
            when(linkedInEnricher.enrich(any(), any())).thenReturn(expected);

            service.enrichProfile(candidateId, ExternalProfileSource.LINKEDIN);

            verify(linkedInEnricher).enrich(expected, candidate);
            verifyNoInteractions(githubEnricher, twitterEnricher, internetSearchEnricher);
        }

        @Test
        @DisplayName("routes TWITTER to TwitterProfileEnricher")
        void routesTwitter() {
            CandidateExternalProfile expected = profile(ExternalProfileSource.TWITTER, "SUCCESS");
            when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
            when(externalProfileRepository.findByCandidateIdAndSource(candidateId, ExternalProfileSource.TWITTER))
                    .thenReturn(Optional.of(expected));
            when(twitterEnricher.enrich(any(), any())).thenReturn(expected);

            service.enrichProfile(candidateId, ExternalProfileSource.TWITTER);

            verify(twitterEnricher).enrich(expected, candidate);
            verifyNoInteractions(githubEnricher, linkedInEnricher, internetSearchEnricher);
        }

        @Test
        @DisplayName("routes INTERNET_SEARCH to InternetSearchProfileEnricher")
        void routesInternetSearch() {
            CandidateExternalProfile expected = profile(ExternalProfileSource.INTERNET_SEARCH, "SUCCESS");
            when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
            when(externalProfileRepository.findByCandidateIdAndSource(candidateId, ExternalProfileSource.INTERNET_SEARCH))
                    .thenReturn(Optional.of(expected));
            when(internetSearchEnricher.enrich(any(), any())).thenReturn(expected);

            service.enrichProfile(candidateId, ExternalProfileSource.INTERNET_SEARCH);

            verify(internetSearchEnricher).enrich(expected, candidate);
            verifyNoInteractions(githubEnricher, linkedInEnricher, twitterEnricher);
        }

        @Test
        @DisplayName("throws when candidate not found")
        void throwsIfCandidateNotFound() {
            when(candidateRepository.findById(candidateId)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.enrichProfile(candidateId, ExternalProfileSource.GITHUB))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Candidate not found");
        }

        @Test
        @DisplayName("creates new profile entity when none exists yet")
        void createsNewProfileWhenAbsent() {
            when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
            when(externalProfileRepository.findByCandidateIdAndSource(candidateId, ExternalProfileSource.GITHUB))
                    .thenReturn(Optional.empty());
            CandidateExternalProfile saved = profile(ExternalProfileSource.GITHUB, "SUCCESS");
            when(githubEnricher.enrich(any(), eq(candidate))).thenReturn(saved);

            CandidateExternalProfile result = service.enrichProfile(candidateId, ExternalProfileSource.GITHUB);

            assertThat(result.getStatus()).isEqualTo("SUCCESS");
            // enricher called with a freshly-built PENDING profile
            verify(githubEnricher).enrich(argThat(p -> "PENDING".equals(p.getStatus())), eq(candidate));
        }
    }

    // =========================================================================
    // enrichFromUrl — URL-based auto-detection
    // =========================================================================

    @Nested
    @DisplayName("enrichFromUrl — URL-based source detection")
    class EnrichFromUrl {

        @Test
        @DisplayName("detects GitHub URL and routes to GitHubProfileEnricher")
        void detectsGitHub() {
            when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
            when(githubEnricher.supportsUrl("https://github.com/jdoe")).thenReturn(true);
            when(externalProfileRepository.findByCandidateIdAndSource(any(), eq(ExternalProfileSource.GITHUB)))
                    .thenReturn(Optional.empty());
            CandidateExternalProfile expected = profile(ExternalProfileSource.GITHUB, "SUCCESS");
            when(githubEnricher.enrich(any(), any())).thenReturn(expected);

            CandidateExternalProfile result = service.enrichFromUrl(candidateId, "https://github.com/jdoe");

            assertThat(result).isSameAs(expected);
            verify(githubEnricher).enrich(any(), eq(candidate));
        }

        @Test
        @DisplayName("returns null when no enricher recognises the URL")
        void returnsNullForUnknownUrl() {
            when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));

            CandidateExternalProfile result = service.enrichFromUrl(candidateId, "https://unknown-site.example");

            assertThat(result).isNull();
            verify(githubEnricher, never()).enrich(any(), any());
            verify(linkedInEnricher, never()).enrich(any(), any());
            verify(twitterEnricher, never()).enrich(any(), any());
            verify(internetSearchEnricher, never()).enrich(any(), any());
        }

        @Test
        @DisplayName("sets the profile URL before calling the enricher")
        void setsProfileUrlBeforeEnriching() {
            String url = "https://github.com/jdoe";
            when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
            when(githubEnricher.supportsUrl(url)).thenReturn(true);
            when(externalProfileRepository.findByCandidateIdAndSource(any(), any())).thenReturn(Optional.empty());
            when(githubEnricher.enrich(any(), any())).thenAnswer(inv -> inv.getArgument(0));

            service.enrichFromUrl(candidateId, url);

            verify(githubEnricher).enrich(argThat(p -> url.equals(p.getProfileUrl())), eq(candidate));
        }
    }

    // =========================================================================
    // refreshProfile
    // =========================================================================

    @Nested
    @DisplayName("refreshProfile")
    class RefreshProfile {

        @Test
        @DisplayName("routes to the correct enricher based on stored source")
        void routesByStoredSource() {
            UUID profileId = UUID.randomUUID();
            CandidateExternalProfile existing = profile(ExternalProfileSource.GITHUB, "SUCCESS");
            existing.setCandidate(candidate);
            when(externalProfileRepository.findById(profileId)).thenReturn(Optional.of(existing));
            when(githubEnricher.enrich(any(), any())).thenReturn(existing);

            service.refreshProfile(profileId);

            verify(githubEnricher).enrich(existing, candidate);
        }

        @Test
        @DisplayName("throws when profile not found")
        void throwsIfProfileNotFound() {
            UUID profileId = UUID.randomUUID();
            when(externalProfileRepository.findById(profileId)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.refreshProfile(profileId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("External profile not found");
        }
    }

    // =========================================================================
    // buildEnrichmentContext
    // =========================================================================

    @Nested
    @DisplayName("buildEnrichmentContext")
    class BuildEnrichmentContext {

        @Test
        @DisplayName("returns null when no SUCCESS profiles exist")
        void returnsNullWhenEmpty() {
            when(externalProfileRepository.findByCandidateIdAndStatus(candidateId, "SUCCESS"))
                    .thenReturn(List.of());
            assertThat(service.buildEnrichmentContext(candidateId)).isNull();
        }

        @Test
        @DisplayName("includes profile data in context string")
        void includesProfileData() {
            CandidateExternalProfile p = profile(ExternalProfileSource.GITHUB, "SUCCESS");
            p.setBio("Open source contributor");
            p.setProfileUrl("https://github.com/jdoe");
            p.setPublicRepos(42);
            p.setFollowers(100);

            when(externalProfileRepository.findByCandidateIdAndStatus(candidateId, "SUCCESS"))
                    .thenReturn(List.of(p));

            String context = service.buildEnrichmentContext(candidateId);

            assertThat(context)
                    .contains("GITHUB")
                    .contains("https://github.com/jdoe")
                    .contains("Open source contributor")
                    .contains("42")
                    .contains("100");
        }
    }

    // =========================================================================
    // URL extraction utilities
    // =========================================================================

    @Nested
    @DisplayName("GitHubProfileEnricher.extractLoginFromUrl")
    class GitHubExtractLogin {
        @Test void extractsFromCleanUrl()     { assertThat(GitHubProfileEnricher.extractLoginFromUrl("https://github.com/jdoe")).isEqualTo("jdoe"); }
        @Test void extractsFromRepoUrl()      { assertThat(GitHubProfileEnricher.extractLoginFromUrl("https://github.com/jdoe/myrepo")).isEqualTo("jdoe"); }
        @Test void returnsNullForNull()       { assertThat(GitHubProfileEnricher.extractLoginFromUrl(null)).isNull(); }
        @Test void returnsNullForNonGitHub()  { assertThat(GitHubProfileEnricher.extractLoginFromUrl("https://example.com")).isNull(); }
    }

    @Nested
    @DisplayName("TwitterProfileEnricher.extractUsernameFromUrl")
    class TwitterExtractUsername {
        @Test void extractsFromTwitterUrl()   { assertThat(TwitterProfileEnricher.extractUsernameFromUrl("https://twitter.com/jdoe")).isEqualTo("jdoe"); }
        @Test void extractsFromXUrl()         { assertThat(TwitterProfileEnricher.extractUsernameFromUrl("https://x.com/jdoe")).isEqualTo("jdoe"); }
        @Test void stripsAtSign()             { assertThat(TwitterProfileEnricher.extractUsernameFromUrl("https://x.com/@jdoe")).isEqualTo("jdoe"); }
        @Test void returnsNullForNull()       { assertThat(TwitterProfileEnricher.extractUsernameFromUrl(null)).isNull(); }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private CandidateExternalProfile profile(ExternalProfileSource source, String status) {
        return CandidateExternalProfile.builder()
                .id(UUID.randomUUID())
                .candidate(candidate)
                .source(source)
                .status(status)
                .build();
    }
}
