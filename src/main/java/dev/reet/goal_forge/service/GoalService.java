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
    public Goal completeGoal(String id) {
        Goal goal = goalRepository.findById(id)
                .orElseThrow(() -> new GoalNotFoundException("Goal not found"));
        if ("COMPLETED".equals(goal.getStatus())) {
            throw new RuntimeException("Goal is already completed");
        }
        goal.setStatus("COMPLETED");
        return goalRepository.save(goal);
    }
    public List<Goal> getGoalsByUser(String userId) {
        return goalRepository.findByUserId(userId);
    }
    public void deleteGoalsByUser(String userId) {
        if (userId == null) return;
        List<Goal> userGoals = goalRepository.findByUserId(userId);
        goalRepository.deleteAll(userGoals);
    }
    private static final Logger logger = LoggerFactory.getLogger(GoalService.class);

    private final GoalRepository goalRepository;

    public GoalService(GoalRepository goalRepository) {
        this.goalRepository = goalRepository;
    }

    public Goal createGoal(Goal goal) {
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

    public Optional<Goal> getGoal(String id) {
        return goalRepository.findById(id);
    }

    public List<Goal> getAllGoals() {
        return goalRepository.findAll();
    }


    public void deleteAllGoals() {
        goalRepository.deleteAll();
    }

    public List<Goal> saveAllGoals(List<Goal> goals) {
        return goalRepository.saveAll(goals);
    }

    public Goal addProgress(String goalId, LocalDate date, double effort) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new GoalNotFoundException("Goal not found"));
        if ("PAUSED".equals(goal.getStatus())) throw new GoalPausedException("Goal is paused");
        LocalDate today = LocalDate.now();
        // if (!date.equals(today)) {
        //     throw new PreviousDateEffortException("Effort can only be added for today: " + today);
        // }
        if (date.isBefore(today)) {
            throw new PreviousDateEffortException("Effort cannot be added for previous days. Today: " + today);
        }

        // Only one effort entry per day: overwrite today's value
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

    public Goal pauseGoal(String id) {
        Goal goal = goalRepository.findById(id)
                .orElseThrow(() -> new GoalNotFoundException("Goal not found"));
        if (!"ACTIVE".equals(goal.getStatus())) {
            throw new GoalPausedException("Goal is not active and cannot be paused");
        }
        goal.setStatus("PAUSED");
        return goalRepository.save(goal);
    }

    public Goal resumeGoal(String id) {
        Goal goal = goalRepository.findById(id)
                .orElseThrow(() -> new GoalNotFoundException("Goal not found"));
        if (!"PAUSED".equals(goal.getStatus())) {
            throw new GoalPausedException("Goal is not paused and cannot be resumed");
        }
        goal.setStatus("ACTIVE");
        return goalRepository.save(goal);
    }
}
