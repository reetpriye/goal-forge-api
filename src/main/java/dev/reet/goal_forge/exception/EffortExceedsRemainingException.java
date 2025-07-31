package dev.reet.goal_forge.exception;

public class EffortExceedsRemainingException extends RuntimeException {
    public EffortExceedsRemainingException(String message) {
        super(message);
    }
}
