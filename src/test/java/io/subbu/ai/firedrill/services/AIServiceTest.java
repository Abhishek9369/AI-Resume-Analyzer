package io.subbu.ai.firedrill.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.subbu.ai.firedrill.config.AiPromptsProperties;
import io.subbu.ai.firedrill.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AIService.
 *
 * <p>AIService now uses the Spring AI {@link ChatClient} for LLM calls.
 * The ChatClient is mocked so tests run without a real LLM.
 * When the LLM is unavailable (mock throws), service methods return
 * graceful fallback responses — they do NOT propagate exceptions.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AIService Unit Tests")
class AIServiceTest {

    // Mock ChatClient.Builder + ChatClient using deep stubs so the fluent API chain works.
    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private ChatClient.Builder chatClientBuilder;

    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    private AIService aiService;
    private AiPromptsProperties prompts;

    private String sampleResumeContent;
    private ResumeAnalysisRequest resumeAnalysisRequest;
    private CandidateMatchRequest candidateMatchRequest;

    @BeforeEach
    void setUp() {
        // Wire builder to return our deep-stub ChatClient
        when(chatClientBuilder.build()).thenReturn(chatClient);

        // Build minimal prompt templates sufficient for rendering
        prompts = new AiPromptsProperties();
        AiPromptsProperties.PromptTemplate resumeTemplate = new AiPromptsProperties.PromptTemplate();
        resumeTemplate.setSystem("You are an HR analyst.");
        resumeTemplate.setUserTemplate("Analyse: {resumeContent}. Respond in JSON.");
        prompts.setResumeAnalysis(resumeTemplate);

        AiPromptsProperties.PromptTemplate matchTemplate = new AiPromptsProperties.PromptTemplate();
        matchTemplate.setSystem("You are a recruitment analyst.");
        matchTemplate.setUserTemplate(
                "Match candidate {experienceSummary} skills={skills} domain={domainKnowledge} " +
                "academic={academicBackground} yoe={yearsOfExperience} {enrichedSection} " +
                "job={jobTitle} desc={jobDescription} reqSkills={requiredSkills} " +
                "edu={requiredEducation} domainReq={domainRequirements} " +
                "exp={minExperience}-{maxExperience}. Respond in JSON.");
        prompts.setCandidateMatching(matchTemplate);

        aiService = new AIService(chatClientBuilder, new ObjectMapper(), prompts);

        sampleResumeContent = """
            John Doe
            john.doe@email.com | (555) 123-4567
            
            EXPERIENCE
            Senior Software Engineer at Tech Corp (2019-2024)
            - Led development of microservices architecture
            - Mentored team of 5 developers
            
            SKILLS
            Java, Spring Boot, Kubernetes, PostgreSQL, React
            
            EDUCATION
            Master of Science in Computer Science, MIT, 2019
            """;

        resumeAnalysisRequest = ResumeAnalysisRequest.builder()
                .filename("john-doe-resume.pdf")
                .resumeContent(sampleResumeContent)
                .build();

        candidateMatchRequest = CandidateMatchRequest.builder()
                .experienceSummary("Senior Software Engineer with 5 years of experience in microservices")
                .skills("Java, Spring Boot, Kubernetes, PostgreSQL, React")
                .domainKnowledge("Enterprise software, Cloud architecture")
                .academicBackground("Master of Science in Computer Science, MIT")
                .yearsOfExperience(5)
                .jobTitle("Lead Software Engineer")
                .jobDescription("Looking for experienced engineer to lead microservices development")
                .requiredSkills("Java, Spring Boot, Kubernetes, Docker")
                .requiredEducation("Bachelor's or Master's in Computer Science")
                .domainRequirements("Enterprise software development")
                .minExperienceYears(4)
                .maxExperienceYears(8)
                .build();
    }

    // ---- Helper: make the ChatClient chain return a given String ----
    private void stubChatClientContent(String jsonContent) {
        when(chatClient.prompt()
                .system(anyString())
                .user(anyString())
                .options(any())
                .call()
                .content())
                .thenReturn(jsonContent);
    }

    // ---- Helper: make the ChatClient chain throw an exception ----
    private void stubChatClientException(RuntimeException ex) {
        when(chatClient.prompt()
                .system(anyString())
                .user(anyString())
                .options(any())
                .call()
                .content())
                .thenThrow(ex);
    }

    @Test
    @DisplayName("Should return fallback response when LLM returns null content")
    void shouldHandleLlmUnavailableGracefully() {
        // RETURNS_DEEP_STUBS makes .content() return null by default.
        // AIService detects null → throws LlmServiceException → catches it → returns fallback.
        ResumeAnalysisResponse result = aiService.analyzeResume(resumeAnalysisRequest);

        assertThat(result).isNotNull();
        assertThat(result.getExperienceSummary()).isNotNull();
    }

    @Test
    @DisplayName("Should return fallback match response when LLM returns null content")
    void shouldHandleLlmUnavailableForMatchingGracefully() {
        CandidateMatchResponse result = aiService.matchCandidate(candidateMatchRequest);

        assertThat(result).isNotNull();
        assertThat(result.getRecommendation()).isNotNull();
    }

    @Test
    @DisplayName("Should not throw when resume content is empty")
    void shouldHandleEmptyResumeContent() {
        ResumeAnalysisRequest emptyRequest = ResumeAnalysisRequest.builder()
                .filename("empty.pdf")
                .resumeContent("")
                .build();

        // Service should handle empty content without throwing
        ResumeAnalysisResponse result = aiService.analyzeResume(emptyRequest);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should parse valid LLM JSON response into ResumeAnalysisResponse")
    void shouldParseValidResumeAnalysisResponse() {
        String validJson = """
                {
                  "name": "John Doe",
                  "email": "john.doe@email.com",
                  "mobile": "555-123-4567",
                  "experienceSummary": "Senior engineer with microservices expertise",
                  "skills": "Java, Spring Boot, Kubernetes",
                  "domainKnowledge": "Enterprise software",
                  "academicBackground": "MSc Computer Science, MIT",
                  "yearsOfExperience": 5,
                  "confidenceScore": 0.95
                }
                """;
        stubChatClientContent(validJson);

        ResumeAnalysisResponse result = aiService.analyzeResume(resumeAnalysisRequest);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("John Doe");
        assertThat(result.getEmail()).isEqualTo("john.doe@email.com");
        assertThat(result.getYearsOfExperience()).isEqualTo(5);
        assertThat(result.getConfidenceScore()).isEqualTo(0.95);
    }

    @Test
    @DisplayName("Should parse valid LLM JSON response into CandidateMatchResponse")
    void shouldParseValidCandidateMatchResponse() {
        String validJson = """
                {
                  "matchScore": 85,
                  "skillsScore": 90,
                  "experienceScore": 80,
                  "educationScore": 85,
                  "domainScore": 80,
                  "explanation": "Strong technical match",
                  "strengths": "Java expertise, microservices background",
                  "gaps": "Docker not explicitly mentioned",
                  "recommendation": "Strong Match"
                }
                """;
        stubChatClientContent(validJson);

        CandidateMatchResponse result = aiService.matchCandidate(candidateMatchRequest);

        assertThat(result).isNotNull();
        assertThat(result.getMatchScore()).isEqualTo(85.0);
        assertThat(result.getRecommendation()).isEqualTo("Strong Match");
    }

    @Test
    @DisplayName("Should return fallback when LLM throws runtime exception")
    void shouldHandleLlmRuntimeException() {
        stubChatClientException(new RuntimeException("Connection refused"));

        ResumeAnalysisResponse result = aiService.analyzeResume(resumeAnalysisRequest);

        assertThat(result).isNotNull();
        assertThat(result.getExperienceSummary())
                .contains("AI analysis unavailable");
    }

    @Test
    @DisplayName("Should strip markdown code fences from LLM response before parsing")
    void shouldStripMarkdownCodeFencesFromResponse() {
        String wrappedJson = """
                ```json
                {
                  "name": "Jane Smith",
                  "email": "jane@example.com",
                  "mobile": "",
                  "experienceSummary": "Full-stack developer",
                  "skills": "Python, Django",
                  "domainKnowledge": "Web",
                  "academicBackground": "BSc",
                  "yearsOfExperience": 3,
                  "confidenceScore": 0.8
                }
                ```
                """;
        stubChatClientContent(wrappedJson);

        ResumeAnalysisResponse result = aiService.analyzeResume(resumeAnalysisRequest);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Jane Smith");
    }
}
