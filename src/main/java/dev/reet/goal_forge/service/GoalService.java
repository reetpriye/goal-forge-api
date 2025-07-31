package dev.reet.goal_forge.service;

import dev.reet.goal_forge.exception.GoalPausedException;
import dev.reet.goal_forge.exception.PreviousDateEffortException;
import dev.reet.goal_forge.exception.GoalNotFoundException;
import dev.reet.goal_forge.model.Goal;
import dev.reet.goal_forge.repository.GoalRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class GoalService {
    private static final Logger logger = LoggerFactory.getLogger(GoalService.class);
    private final GoalRepository goalRepository;

    public GoalService(GoalRepository goalRepository) {
        this.goalRepository = goalRepository;
    }

    /**
     * Route: POST /api/goals
     * Args: Goal goal (request body)
     * Description: Creates a new goal with default values and saves it to the repository.
     */
    public Goal addGoal(Goal goal) {
        goal.setInvestedEffort(0.0);
        goal.setRemainingEffort(goal.getEstimatedEffort());
        goal.setStatus("NOT_STARTED");
        goal.setStartDate(null);
        if (goal.getProgressType() != null) {
            goal.setProgressType(goal.getProgressType().toLowerCase());
        }
        logger.info("Creating goal: {}", goal);
        return goalRepository.save(goal);
    }

    /**
     * Route: GET /api/goals/{id}
     * Args: String id (path variable)
     * Description: Retrieves a goal by its ID.
     */
    public Optional<Goal> getGoal(String id) {
        return goalRepository.findById(id);
    }

    /**
     * Route: GET /api/goals/user/{userId}
     * Args: String userId (path variable)
     * Description: Retrieves all goals for a specific user.
     */
    public List<Goal> getGoals(String userId) {
        return goalRepository.findByUserId(userId);
    }

    /**
     * Route: POST /api/goals/{goalId}/progress
     * Args: String goalId (path variable), LocalDate date, double effort (request body)
     * Description: Adds progress effort for a goal on a specific date.
     */
    public Goal addProgress(String goalId, LocalDate date, double effort) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new GoalNotFoundException("Goal not found"));
        if ("PAUSED".equals(goal.getStatus())) throw new GoalPausedException("Goal is paused");
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            throw new PreviousDateEffortException("Effort cannot be added for previous days. Today: " + today);
        }
        double totalEffortExcludingToday = goal.getProgressCalendar().entrySet().stream()
            .filter(e -> !e.getKey().equals(date.toString()))
            .mapToDouble(e -> e.getValue()).sum();
        double remainingEffort = goal.getEstimatedEffort() - totalEffortExcludingToday;
        if (effort > remainingEffort) {
            throw new dev.reet.goal_forge.exception.EffortExceedsRemainingException("Effort for today exceeds remaining effort. Remaining: " + remainingEffort);
        }
        goal.getProgressCalendar().put(date.toString(), effort);
        double totalInvestedEffort = totalEffortExcludingToday + effort;
        goal.setInvestedEffort(totalInvestedEffort);
        goal.setRemainingEffort(goal.getEstimatedEffort() - totalInvestedEffort);
        return goalRepository.save(goal);
    }

    /**
     * Route: POST /api/goals/{id}/start
     * Args: String id (path variable)
     * Description: Starts a goal, setting its status to ACTIVE and start date to today.
     */
    public Goal startGoal(String id) {
        Goal goal = goalRepository.findById(id)
                .orElseThrow(() -> new GoalNotFoundException("Goal not found"));
        if (!"NOT_STARTED".equals(goal.getStatus())) {
            throw new RuntimeException("Goal already started or completed");
        }
        goal.setStartDate(LocalDate.now());
        goal.setStatus("ACTIVE");
        return goalRepository.save(goal);
    }

    /**
     * Route: POST /api/goals/{id}/pause
     * Args: String id (path variable)
     * Description: Pauses an active goal, setting its status to PAUSED.
     */
    public Goal pauseGoal(String id) {
        Goal goal = goalRepository.findById(id)
                .orElseThrow(() -> new GoalNotFoundException("Goal not found"));
        if (!"ACTIVE".equals(goal.getStatus())) {
            throw new GoalPausedException("Goal is not active and cannot be paused");
        }
        goal.setStatus("PAUSED");
        return goalRepository.save(goal);
    }

    /**
     * Route: POST /api/goals/{id}/resume
     * Args: String id (path variable)
     * Description: Resumes a paused goal, setting its status to ACTIVE.
     */
    public Goal resumeGoal(String id) {
        Goal goal = goalRepository.findById(id)
                .orElseThrow(() -> new GoalNotFoundException("Goal not found"));
        if (!"PAUSED".equals(goal.getStatus())) {
            throw new GoalPausedException("Goal is not paused and cannot be resumed");
        }
        goal.setStatus("ACTIVE");
        return goalRepository.save(goal);
    }

    /**
     * Route: POST /api/goals/{id}/complete
     * Args: String id (path variable)
     * Description: Marks a goal as completed, setting its status to COMPLETED.
     */
    public Goal completeGoal(String id) {
        Goal goal = goalRepository.findById(id)
                .orElseThrow(() -> new GoalNotFoundException("Goal not found"));
        if ("COMPLETED".equals(goal.getStatus())) {
            throw new RuntimeException("Goal is already completed");
        }
        goal.setStatus("COMPLETED");
        return goalRepository.save(goal);
    }

    /**
     * Route: DELETE /api/goals/{id}
     * Args: String id (path variable)
     * Description: Deletes a goal by its ID.
     */
    public void deleteGoal(String id) {
        Goal goal = goalRepository.findById(id)
            .orElseThrow(() -> new GoalNotFoundException("Goal not found"));
        goalRepository.delete(goal);
    }

    /**
     * Route: DELETE /api/goals/user/{userId}
     * Args: String userId (path variable)
     * Description: Deletes all goals for a specific user.
     */
    public void deleteGoals(String userId) {
        if (userId == null) return;
        List<Goal> userGoals = goalRepository.findByUserId(userId);
        goalRepository.deleteAll(userGoals);
    }
    
    /**
     * Route: POST /api/goals/batch
     * Args: List<Goal> goals (request body)
     * Description: Saves a batch of goals to the repository.
     */
    public List<Goal> saveAllGoals(List<Goal> goals) {
        return goalRepository.saveAll(goals);
    }
}
