package io.subbu.ai.firedrill.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.subbu.ai.firedrill.config.AiPromptsProperties;
import io.subbu.ai.firedrill.entities.Candidate;
import io.subbu.ai.firedrill.entities.ExternalProfileSource;
import io.subbu.ai.firedrill.entities.JobRequirement;
import io.subbu.ai.firedrill.exceptions.LlmServiceException;
import io.subbu.ai.firedrill.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for AI/LLM interactions using the Spring AI {@link ChatClient}.
 *
 * <p>Prompt templates are externalised in {@code ai-prompts.yml} and injected via
 * {@link AiPromptsProperties}, so prompts can be updated without touching Java source code.</p>
 *
 * <p>All LLM calls request a structured JSON response. Jackson ({@link ObjectMapper})
 * deserialises the JSON into strongly-typed Java objects, keeping the rest of the codebase
 * free of raw JSON string manipulation.</p>
 *
 * <p>Error-handling strategy:
 * <ul>
 *   <li>Network / API failures are caught and wrapped in {@link LlmServiceException}.</li>
 *   <li>JSON parse failures fall back to a safe default response so the caller can
 *       continue without crashing the upload / match pipeline.</li>
 *   <li>All errors are logged with enough context for operational debugging.</li>
 * </ul>
 * </p>
 */
@Service
@Slf4j
public class AIService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final AiPromptsProperties prompts;

    public AIService(ChatClient.Builder chatClientBuilder,
                     ObjectMapper objectMapper,
                     AiPromptsProperties prompts) {
        // Build a default ChatClient; per-call options (temperature, max-tokens) are
        // applied inline so that each LLM task uses its own tuning parameters.
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
        this.prompts = prompts;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Analyse resume content using the LLM to extract candidate information.
     *
     * @param request Resume analysis request containing file name and raw text.
     * @return Populated {@link ResumeAnalysisResponse}; a safe fallback is returned
     *         when the LLM is unavailable or returns unparseable content.
     */
    public ResumeAnalysisResponse analyzeResume(ResumeAnalysisRequest request) {
        log.info("Analysing resume: {}", request.getFilename());
        try {
            String userPrompt = prompts.getResumeAnalysis().render(
                    "resumeContent", request.getResumeContent()
            );

            String rawResponse = callLlm(
                    prompts.getResumeAnalysis().getSystem(),
                    userPrompt,
                    /* temperature */ 0.3f,
                    /* maxTokens  */ 3000
            );

            log.info("LLM resume-analysis response length: {}", rawResponse != null ? rawResponse.length() : "null");
            return parseResumeAnalysisResponse(rawResponse);
        } catch (LlmServiceException e) {
            log.warn("LLM unavailable during resume analysis — returning fallback. Reason: {}", e.getMessage());
            return ResumeAnalysisResponse.builder()
                    .name("Unknown")
                    .email("")
                    .mobile("")
                    .experienceSummary("Resume processed (AI analysis unavailable)")
                    .skills("")
                    .domainKnowledge("")
                    .academicBackground("")
                    .yearsOfExperience(0)
                    .confidenceScore(0.0)
                    .build();
        }
    }

    /**
     * Generate a match score between a candidate and a job requirement using the LLM.
     *
     * @param request Candidate vs. job match request.
     * @return Populated {@link CandidateMatchResponse}; a safe fallback is returned
     *         when the LLM is unavailable or returns unparseable content.
     */
    public CandidateMatchResponse matchCandidate(CandidateMatchRequest request) {
        log.info("Matching candidate for job: {}", request.getJobTitle());
        try {
            String enrichedSection = buildEnrichedSection(request.getEnrichedProfileContext());

            String userPrompt = prompts.getCandidateMatching().render(
                    "experienceSummary",  nullSafe(request.getExperienceSummary()),
                    "skills",             nullSafe(request.getSkills()),
                    "domainKnowledge",    nullSafe(request.getDomainKnowledge()),
                    "academicBackground", nullSafe(request.getAcademicBackground()),
                    "yearsOfExperience",  String.valueOf(request.getYearsOfExperience() != null ? request.getYearsOfExperience() : 0),
                    "enrichedSection",    enrichedSection,
                    "jobTitle",           nullSafe(request.getJobTitle()),
                    "jobDescription",     nullSafe(request.getJobDescription()),
                    "requiredSkills",     nullSafe(request.getRequiredSkills()),
                    "requiredEducation",  nullSafe(request.getRequiredEducation()),
                    "domainRequirements", nullSafe(request.getDomainRequirements()),
                    "minExperience",      String.valueOf(request.getMinExperienceYears() != null ? request.getMinExperienceYears() : 0),
                    "maxExperience",      String.valueOf(request.getMaxExperienceYears() != null ? request.getMaxExperienceYears() : 100)
            );

            String rawResponse = callLlm(
                    prompts.getCandidateMatching().getSystem(),
                    userPrompt,
                    /* temperature */ 0.2f,
                    /* maxTokens  */ 2000
            );

            log.debug("LLM candidate-matching raw response: {}", rawResponse);
            return parseMatchingResponse(rawResponse);
        } catch (LlmServiceException e) {
            log.warn("LLM unavailable during candidate matching — returning fallback. Reason: {}", e.getMessage());
            return CandidateMatchResponse.builder()
                    .matchScore(0.0)
                    .skillsScore(0.0)
                    .experienceScore(0.0)
                    .educationScore(0.0)
                    .domainScore(0.0)
                    .explanation("AI matching temporarily unavailable — please retry shortly.")
                    .strengths("")
                    .gaps("")
                    .recommendation("Error in Analysis")
                    .build();
        }
    }

    // -------------------------------------------------------------------------
    // Agentic source selection
    // -------------------------------------------------------------------------

    /**
     * Uses the LLM to decide which external data sources are most relevant for
     * the given candidate+job combination before a match request is processed.
     *
     * <p>This is the agentic decision-making step. The LLM reasons about the job
     * requirements and candidate profile to recommend which sources (GitHub, LinkedIn,
     * Twitter, INTERNET_SEARCH) to fetch before scoring begins.</p>
     *
     * <p>Falls back to {@code [INTERNET_SEARCH]} on any failure so the pipeline
     * is never blocked by an LLM error.</p>
     *
     * @param candidate the candidate being evaluated
     * @param job       the target job requirement
     * @return ordered list of recommended {@link ExternalProfileSource} values
     */
    @SuppressWarnings("unchecked")
    public List<ExternalProfileSource> selectEnrichmentSources(Candidate candidate, JobRequirement job) {
        log.info("[SOURCE-SELECT] Selecting enrichment sources for {} vs job '{}'",
                candidate.getName(), job.getTitle());
        try {
            String userPrompt = prompts.getSourceSelection().render(
                    "candidateSkills",   nullSafe(candidate.getSkills()),
                    "experienceSummary", nullSafe(candidate.getExperienceSummary()),
                    "yearsOfExperience", String.valueOf(candidate.getYearsOfExperience() != null ? candidate.getYearsOfExperience() : 0),
                    "jobTitle",          nullSafe(job.getTitle()),
                    "requiredSkills",    nullSafe(job.getRequiredSkills()),
                    "jobDescription",    nullSafe(job.getDescription())
            );

            String rawResponse = callLlm(
                    prompts.getSourceSelection().getSystem(),
                    userPrompt,
                    /* temperature */ 0.1f,
                    /* maxTokens  */ 300
            );

            String json = extractJson(rawResponse);
            Map<String, Object> parsed = objectMapper.readValue(json, new TypeReference<>() {});
            List<String> sourceNames = (List<String>) parsed.getOrDefault("sources", List.of("INTERNET_SEARCH"));
            String reasoning = (String) parsed.getOrDefault("reasoning", "");
            log.info("[SOURCE-SELECT] Recommended {} for {}: {}", sourceNames, candidate.getName(), reasoning);

            return sourceNames.stream()
                    .map(name -> {
                        try { return ExternalProfileSource.valueOf(name); }
                        catch (IllegalArgumentException e) { return null; }
                    })
                    .filter(s -> s != null)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("[SOURCE-SELECT] Failed ({}); defaulting to INTERNET_SEARCH", e.getMessage());
            return List.of(ExternalProfileSource.INTERNET_SEARCH);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers — LLM communication
    // -------------------------------------------------------------------------

    /**
     * Calls the configured LLM with the given system and user messages.
     *
     * <p>Per-call {@link OpenAiChatOptions} allow each task to use its own
     * temperature / token budget without altering global application configuration.</p>
     *
     * @param systemMessage Persona / contract instruction for the LLM.
     * @param userMessage   The rendered prompt to send.
     * @param temperature   Sampling temperature (lower = more deterministic).
     * @param maxTokens     Maximum tokens in the response.
     * @return Raw LLM response text, or {@code null} on unrecoverable failure.
     * @throws LlmServiceException if the LLM returns an empty or null response.
     */
    private String callLlm(String systemMessage, String userMessage, float temperature, int maxTokens) {
        try {
            log.debug("Calling LLM — temperature={}, maxTokens={}", temperature, maxTokens);

            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .temperature((double) temperature)
                    .maxTokens(maxTokens)
                    .build();

            String content = chatClient.prompt()
                    .system(systemMessage)
                    .user(userMessage)
                    .options(options)
                    .call()
                    .content();

            if (content == null || content.isBlank()) {
                throw new LlmServiceException("LLM returned an empty response");
            }

            return content;

        } catch (LlmServiceException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = "LLM API call failed: " + e.getMessage();
            log.error(errorMsg, e);
            throw new LlmServiceException(errorMsg, e);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers — response parsing
    // -------------------------------------------------------------------------

    /**
     * Parse the LLM response for resume analysis into a typed Java object.
     * Returns a safe fallback when the LLM is unreachable or the response is malformed.
     */
    private ResumeAnalysisResponse parseResumeAnalysisResponse(String rawResponse) {
        try {
            String json = extractJson(rawResponse);
            return objectMapper.readValue(json, ResumeAnalysisResponse.class);
        } catch (LlmServiceException e) {
            log.warn("LLM unavailable during resume analysis — returning fallback. Reason: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to parse LLM resume-analysis response. Raw response: {}", rawResponse, e);
        }
        return ResumeAnalysisResponse.builder()
                .name("Unknown")
                .email("")
                .mobile("")
                .experienceSummary("Resume processed (AI analysis unavailable)")
                .skills("")
                .domainKnowledge("")
                .academicBackground("")
                .yearsOfExperience(0)
                .confidenceScore(0.0)
                .build();
    }

    /**
     * Parse the LLM response for candidate matching into a typed Java object.
     * Returns a safe fallback when the LLM is unreachable or the response is malformed.
     */
    private CandidateMatchResponse parseMatchingResponse(String rawResponse) {
        try {
            String json = extractJson(rawResponse);
            return objectMapper.readValue(json, CandidateMatchResponse.class);
        } catch (LlmServiceException e) {
            log.warn("LLM unavailable during candidate matching — returning fallback. Reason: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to parse LLM candidate-matching response. Raw response: {}", rawResponse, e);
        }
        return CandidateMatchResponse.builder()
                .matchScore(0.0)
                .skillsScore(0.0)
                .experienceScore(0.0)
                .educationScore(0.0)
                .domainScore(0.0)
                .explanation("AI matching temporarily unavailable — please retry shortly.")
                .strengths("")
                .gaps("")
                .recommendation("Error in Analysis")
                .build();
    }

    /**
     * Extract the first complete JSON object from an LLM response string.
     *
     * <p>Some LLMs wrap their JSON in markdown code fences or add introductory text.
     * This method strips that noise by locating the outermost {@code { … }} block.</p>
     *
     * @param response Raw LLM response.
     * @return Clean JSON string.
     * @throws LlmServiceException if no JSON object can be detected.
     */
    private String extractJson(String response) {
        if (response == null || response.isBlank()) {
            throw new LlmServiceException("LLM returned null or empty response — cannot extract JSON");
        }

        // Strip markdown code fences that some models emit: ```json ... ```
        String cleaned = response.replaceAll("(?s)```[a-zA-Z]*\\n?", "").replaceAll("```", "").strip();

        int start = cleaned.indexOf('{');
        int end   = cleaned.lastIndexOf('}');

        if (start != -1 && end != -1 && end > start) {
            return cleaned.substring(start, end + 1);
        }

        throw new LlmServiceException(
                "No JSON object found in LLM response. Response starts with: "
                + response.substring(0, Math.min(200, response.length())));
    }

    // -------------------------------------------------------------------------
    // Private helpers — misc
    // -------------------------------------------------------------------------

    private static String nullSafe(String value) {
        return value != null ? value : "";
    }

    private static String buildEnrichedSection(String enrichedProfileContext) {
        if (enrichedProfileContext == null || enrichedProfileContext.isBlank()) {
            return "";
        }
        return "\nEXTERNAL PROFILE DATA (from GitHub / LinkedIn / Internet):\n"
                + enrichedProfileContext + "\n";
    }
}
