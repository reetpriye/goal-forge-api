package dev.reet.goal_forge.service;

import dev.reet.goal_forge.exception.GoalPausedException;
import dev.reet.goal_forge.exception.PreviousDateEffortException;
import dev.reet.goal_forge.exception.GoalNotFoundException;
import dev.reet.goal_forge.exception.GoalNotStartedException;
import dev.reet.goal_forge.model.Goal;
import dev.reet.goal_forge.repository.GoalRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map;
import java.util.ArrayList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Collectors.toMap;

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
        
        // Set display order to be at the end of current goals
        if (goal.getUserId() != null) {
            List<Goal> userGoals = goalRepository.findByUserId(goal.getUserId());
            goal.setDisplayOrder(userGoals.size());
        } else {
            goal.setDisplayOrder(0);
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
     * Description: Retrieves all goals for a specific user ordered by displayOrder.
     */
    public List<Goal> getGoals(String userId) {
        return goalRepository.findByUserIdOrderByDisplayOrder(userId);
    }

    /**
     * Route: POST /api/goals/{goalId}/progress
     * Args: String goalId (path variable), LocalDate date, double effort (request body)
     * Description: Adds progress effort for a goal on a specific date.
     */
    public Goal addProgress(String goalId, LocalDate date, double effort) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new GoalNotFoundException("Goal not found"));
        
        // Check if goal has been started
        if ("NOT_STARTED".equals(goal.getStatus())) {
            throw new GoalNotStartedException("Cannot add progress to a goal that has not been started. Please start the goal first.");
        }
        
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
     * Route: PUT /api/goals/{id}
     * Args: String id (path variable), Goal updatedGoal (request body), String userId
     * Description: Updates an existing goal with new data while preserving certain fields.
     */
    public Goal updateGoal(String id, Goal updatedGoal, String userId) {
        Goal existingGoal = goalRepository.findById(id)
                .orElseThrow(() -> new GoalNotFoundException("Goal not found: " + id));
        
        // Verify that the goal belongs to the user
        if (!userId.equals(existingGoal.getUserId())) {
            throw new GoalNotFoundException("Goal not found or doesn't belong to user: " + id);
        }
        
        // Update only the fields that can be modified
        if (updatedGoal.getGoalName() != null) {
            existingGoal.setGoalName(updatedGoal.getGoalName());
        }
        
        if (updatedGoal.getProgressType() != null) {
            existingGoal.setProgressType(updatedGoal.getProgressType().toLowerCase());
        }
        
        if (updatedGoal.getEstimatedEffort() > 0) {
            existingGoal.setEstimatedEffort(updatedGoal.getEstimatedEffort());
            // Recalculate remaining effort when estimated effort changes
            existingGoal.setRemainingEffort(updatedGoal.getEstimatedEffort() - existingGoal.getInvestedEffort());
        }
        
        // Preserve important fields that shouldn't be changed via edit
        // - userId, displayOrder, investedEffort, progressCalendar, status, startDate stay the same
        
        logger.info("Updating goal: {} for user: {}", id, userId);
        return goalRepository.save(existingGoal);
    }

    /**
     * Route: PUT /api/goals/reorder
     * Args: List<String> goalIds (request body) - ordered list of goal IDs
     * Description: Updates the displayOrder of goals based on their position in the provided list.
     */
    public List<Goal> updateGoalOrders(String userId, List<String> goalIds) {
        logger.info("Updating goal orders for user: {} with {} goals", userId, goalIds.size());
        
        // Fetch user goals once and create efficient lookup structures
        List<Goal> userGoals = goalRepository.findByUserId(userId);
        Set<String> userGoalIds = userGoals.stream().map(Goal::getId).collect(toSet());
        Map<String, Goal> goalMap = userGoals.stream().collect(toMap(Goal::getId, goal -> goal));
        
        // Validate that all goals belong to the user (O(n) instead of O(n×m))
        for (String goalId : goalIds) {
            if (!userGoalIds.contains(goalId)) {
                throw new GoalNotFoundException("Goal not found or doesn't belong to user: " + goalId);
            }
        }
        
        // Collect all goals that need updating
        List<Goal> goalsToUpdate = new ArrayList<>();
        for (int i = 0; i < goalIds.size(); i++) {
            String goalId = goalIds.get(i);
            Goal goal = goalMap.get(goalId); // O(1) lookup instead of database call
            if (goal != null && goal.getDisplayOrder() != i) { // Only update if order changed
                goal.setDisplayOrder(i);
                goalsToUpdate.add(goal);
            }
        }
        
        // Batch update all modified goals (single database operation)
        if (!goalsToUpdate.isEmpty()) {
            goalRepository.saveAll(goalsToUpdate);
            logger.info("Updated display order for {} goals", goalsToUpdate.size());
        }
        
        // Return the updated goals in order
        return goalRepository.findByUserIdOrderByDisplayOrder(userId);
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
