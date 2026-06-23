package io.subbu.ai.firedrill.services;

import io.subbu.ai.firedrill.entities.Candidate;
import io.subbu.ai.firedrill.entities.ResumeEmbedding;
import io.subbu.ai.firedrill.repos.ResumeEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;

/**
 * Service for generating and managing vector embeddings using Spring AI.
 * Handles text chunking and embedding generation for resume content.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final ResumeEmbeddingRepository embeddingRepository;
    private final EmbeddingModel embeddingModel;

    @Value("${app.embedding.batch-size:10}")
    private Integer batchSize;

    /**
     * Generate embeddings for resume content and store in vector database.
     * Chunks the text into manageable segments for better semantic understanding.
     * 
     * @param candidate The candidate entity
     * @param resumeContent Full resume text content
     * @return List of created embedding entities
     */
    @Transactional
    public List<ResumeEmbedding> generateAndStoreEmbeddings(Candidate candidate, String resumeContent) {
        log.info("Generating embeddings for candidate: {}", candidate.getId());

        // Delete existing embeddings for this candidate
        embeddingRepository.deleteByCandidate(candidate);

        // Chunk the resume content
        List<TextChunk> chunks = chunkText(resumeContent);
        List<ResumeEmbedding> embeddings = new ArrayList<>();

        // Process chunks in batches
        for (int i = 0; i < chunks.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, chunks.size());
            List<TextChunk> batch = chunks.subList(i, endIndex);

            // Generate embeddings for the batch
            List<String> texts = batch.stream()
                    .map(TextChunk::text)
                    .toList();

            List<float[]> batchEmbeddings = generateEmbeddingsWithFallback(texts);

            // Create and save embedding entities using native SQL to handle vector type
            for (int j = 0; j < batch.size(); j++) {
                TextChunk chunk = batch.get(j);
                float[] embedding = batchEmbeddings.get(j);
                String embeddingStr = vectorToString(embedding);

                String embId = java.util.UUID.randomUUID().toString();
                embeddingRepository.insertEmbeddingNative(
                        embId,
                        candidate.getId().toString(),
                        chunk.text(),
                        embeddingStr,
                        chunk.sectionType()
                );
                embeddings.add(new ResumeEmbedding(
                        java.util.UUID.fromString(embId),
                        candidate,
                        chunk.text(),
                        embeddingStr,
                        chunk.sectionType(),
                        null
                ));
            }
        }

        log.info("Generated {} embeddings for candidate: {}", embeddings.size(), candidate.getId());
        return embeddings;
    }

    /**
     * Generate embedding vector for a query text.
     * Used for similarity search.
     * 
     * @param queryText Text to embed
     * @return Embedding vector as string
     */
    public String generateQueryEmbedding(String queryText) {
        log.debug("Generating embedding for query: {}", queryText.substring(0, Math.min(50, queryText.length())));
        
        List<float[]> embeddings = generateEmbeddingsWithFallback(List.of(queryText));
        return vectorToString(embeddings.get(0));
    }

    /**
     * Generate embeddings with fallback to avoid Spring AI M4 usage-null issue.
     * Tries embedForResponse first, falls back to per-text embed() on failure.
     */
    private List<float[]> generateEmbeddingsWithFallback(List<String> texts) {
        try {
            EmbeddingResponse response = embeddingModel.embedForResponse(texts);
            return response.getResults().stream()
                    .map(e -> e.getOutput())
                    .toList();
        } catch (IllegalArgumentException | NullPointerException e) {
            log.warn("embedForResponse failed ({}), falling back to single embed()", e.getMessage());
            // Fallback: call embed() per text (bypasses usage checks)
            List<float[]> results = new ArrayList<>();
            for (String text : texts) {
                try {
                    float[] vec = embeddingModel.embed(text);
                    results.add(vec);
                } catch (Exception inner) {
                    log.warn("embed() also failed for text, using zero vector: {}", inner.getMessage());
                    results.add(new float[768]);
                }
            }
            return results;
        }
    }

    /**
     * Chunk resume text into smaller segments for embedding.
     * Aims to create semantically meaningful chunks.
     * 
     * @param text Full resume text
     * @return List of text chunks with section types
     */
    private List<TextChunk> chunkText(String text) {
        List<TextChunk> chunks = new ArrayList<>();

        // Split by sections (simple heuristic)
        String[] sections = text.split("\\n\\n+");
        
        for (String section : sections) {
            if (section.trim().isEmpty()) continue;

            String sectionType = identifySectionType(section);

            // If section is too large, split into smaller chunks
            if (section.length() > 1000) {
                String[] sentences = section.split("\\. ");
                StringBuilder currentChunk = new StringBuilder();

                for (String sentence : sentences) {
                    if (currentChunk.length() + sentence.length() > 1000 && currentChunk.length() > 0) {
                        chunks.add(new TextChunk(currentChunk.toString(), sectionType));
                        currentChunk = new StringBuilder();
                    }
                    currentChunk.append(sentence).append(". ");
                }

                if (currentChunk.length() > 0) {
                    chunks.add(new TextChunk(currentChunk.toString(), sectionType));
                }
            } else {
                chunks.add(new TextChunk(section, sectionType));
            }
        }

        return chunks;
    }

    /**
     * Identify the section type based on content.
     * Simple heuristic-based approach.
     * 
     * @param text Section text
     * @return Section type identifier
     */
    private String identifySectionType(String text) {
        String lowerText = text.toLowerCase();

        if (lowerText.contains("education") || lowerText.contains("degree") || 
            lowerText.contains("university") || lowerText.contains("college")) {
            return "education";
        } else if (lowerText.contains("experience") || lowerText.contains("worked") || 
                   lowerText.contains("position") || lowerText.contains("company")) {
            return "experience";
        } else if (lowerText.contains("skill") || lowerText.contains("proficient") || 
                   lowerText.contains("expertise")) {
            return "skills";
        } else if (lowerText.contains("project")) {
            return "projects";
        } else if (lowerText.contains("certification") || lowerText.contains("certified")) {
            return "certifications";
        } else {
            return "general";
        }
    }

    /**
     * Convert float array to PostgreSQL vector string format.
     * 
     * @param vector Embedding vector
     * @return String representation for pgvector
     */
    private String vectorToString(float[] vector) {
        return Arrays.toString(vector);
    }

    /**
     * Record class for text chunks with section information.
     */
    private record TextChunk(String text, String sectionType) {}
}
