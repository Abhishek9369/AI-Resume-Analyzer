package io.subbu.ai.firedrill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for Resume Analyzer.
 * Enables Spring Boot auto-configuration and async processing capabilities.
 * 
 * This application provides:
 * - AI-powered resume analysis using vector embeddings
 * - Candidate matching against job requirements
 * - Batch processing of resume uploads
 * - Real-time status tracking via GraphQL API
 * - System health monitoring with scheduled checks
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class ResumeAnalyzerApplication {

    /**
     * Application entry point.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(ResumeAnalyzerApplication.class, args);
    }
}
