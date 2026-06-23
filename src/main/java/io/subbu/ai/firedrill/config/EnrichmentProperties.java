package io.subbu.ai.firedrill.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the agentic enrichment pipeline.
 *
 * <p>All values are overridable via environment variables, making it easy to tune
 * behaviour without changing source code.</p>
 *
 * <pre>
 * app.enrichment.staleness-ttl-days          (default: 7)
 * app.enrichment.source-selection-enabled    (default: false)
 * app.enrichment.multi-pass.enabled          (default: true)
 * app.enrichment.multi-pass.borderline-min   (default: 50)
 * app.enrichment.multi-pass.borderline-max   (default: 75)
 * app.enrichment.tavily.api-key              (default: "")
 * </pre>
 */
@Configuration
@ConfigurationProperties(prefix = "app.enrichment")
@Getter
@Setter
public class EnrichmentProperties {

    /**
     * Enriched profiles older than this many days are considered stale and are
     * automatically re-fetched before a match request is processed.
     */
    private int stalenessTtlDays = 7;

    /**
     * When {@code true}, an extra LLM call is made before each match to decide
     * which external sources are most relevant for the given job requirement.
     * This improves retrieval quality at the cost of one additional LLM round-trip.
     */
    private boolean sourceSelectionEnabled = false;

    /** Multi-pass matching configuration. */
    private MultiPassConfig multiPass = new MultiPassConfig();

    /** Tavily web-search integration for the INTERNET_SEARCH enricher. */
    private TavilyConfig tavily = new TavilyConfig();

    // -------------------------------------------------------------------------

    @Getter
    @Setter
    public static class MultiPassConfig {

        /**
         * When {@code true}, candidates whose initial match score falls in the
         * borderline range are re-matched after auto-enrichment.  This typically
         * raises precision for unclear cases without slowing down clear ones.
         */
        private boolean enabled = true;

        /** Lower bound (inclusive) of the borderline score range (0–100). */
        private int borderlineMin = 50;

        /** Upper bound (inclusive) of the borderline score range (0–100). */
        private int borderlineMax = 75;
    }

    @Getter
    @Setter
    public static class TavilyConfig {

        /**
         * Tavily API key — obtain from <a href="https://app.tavily.com">app.tavily.com</a>.
         * When blank, the INTERNET_SEARCH enricher falls back to a locally-synthesised
         * context built from the candidate's existing resume fields.
         */
        private String apiKey = "";
    }
}
