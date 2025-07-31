package dev.reet.goal_forge.exception;

public class GoalPausedException extends RuntimeException {
    public GoalPausedException(String message) {
        super(message);
    }
}