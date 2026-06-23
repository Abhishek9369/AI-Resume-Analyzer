package io.subbu.ai.firedrill.exceptions;

/**
 * Thrown when the LLM service fails to respond or returns an unparseable response.
 * Callers should catch this and return a graceful fallback to the user.
 */
public class LlmServiceException extends RuntimeException {

    public LlmServiceException(String message) {
        super(message);
    }

    public LlmServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
