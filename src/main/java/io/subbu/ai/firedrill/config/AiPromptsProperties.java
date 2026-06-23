package io.subbu.ai.firedrill.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for AI prompt templates.
 * Templates are loaded from ai-prompts.yml so they can be updated
 * without modifying Java source code.
 *
 * <p>Placeholder syntax: Use {placeholderName} within template strings.
 * Replacements are performed in AIService before sending to the LLM.</p>
 */
@Configuration
@ConfigurationProperties(prefix = "ai.prompts")
@Getter
@Setter
public class AiPromptsProperties {

    /** Prompt templates for resume analysis. */
    private PromptTemplate resumeAnalysis = new PromptTemplate();

    /** Prompt templates for candidate-to-job matching. */
    private PromptTemplate candidateMatching = new PromptTemplate();

    /** Prompt templates for agentic source selection before matching. */
    private PromptTemplate sourceSelection = new PromptTemplate();

    /**
     * Holds a system message and a user-facing prompt template for a specific LLM task.
     */
    @Getter
    @Setter
    public static class PromptTemplate {

        /**
         * System instruction that sets the LLM's persona and output contract.
         */
        private String system = "";

        /**
         * User prompt template. Use {placeholderName} tokens that will be
         * replaced at runtime by AIService before the prompt is sent to the LLM.
         */
        private String userTemplate = "";

        /**
         * Convenience helper: replace all {key} tokens in the user template.
         *
         * @param replacements alternating key/value pairs, e.g. "resumeContent", text
         * @return the rendered prompt string ready to send to the LLM
         */
        public String render(String... replacements) {
            if (replacements.length % 2 != 0) {
                throw new IllegalArgumentException("render() requires an even number of arguments (key-value pairs)");
            }
            String result = userTemplate;
            for (int i = 0; i < replacements.length; i += 2) {
                result = result.replace("{" + replacements[i] + "}", replacements[i + 1] != null ? replacements[i + 1] : "");
            }
            return result;
        }
    }
}
