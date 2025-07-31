package dev.reet.goal_forge.controller;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.reet.goal_forge.exception.GoalNotFoundException;
import dev.reet.goal_forge.model.Goal;
import dev.reet.goal_forge.service.GoalService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goals")
public class GoalController {
    @PostMapping("/{id}/start")
    public Goal startGoal(@PathVariable String id) {
        return goalService.startGoal(id);
    }
    @PostMapping("/{id}/complete")
    public Goal completeGoal(@PathVariable String id) {
        return goalService.completeGoal(id);
    }

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping
    public Goal createGoal(@RequestBody Goal goal, @RequestAttribute String userId) {
        goal.setUserId(userId);
        return goalService.createGoal(goal);
    }

    @GetMapping("/{id}")
    public Goal getGoal(@PathVariable String id) {
        return goalService.getGoal(id).orElseThrow(() -> new GoalNotFoundException("Goal not found"));
    }

    @PostMapping("/{id}/progress")
    public Goal addProgress(@PathVariable String id, @RequestBody Map<String, Object> payload) {
        String dateStr = (String) payload.get("date");
        double effort = Double.parseDouble(payload.get("effort").toString());
        return goalService.addProgress(id, LocalDate.parse(dateStr), effort);
    }

    @PostMapping("/{id}/pause")
    public Goal pauseGoal(@PathVariable String id) {
        return goalService.pauseGoal(id);
    }

    @PostMapping("/{id}/resume")
    public Goal resumeGoal(@PathVariable String id) {
        return goalService.resumeGoal(id);
    }

    @GetMapping
    public List<Goal> getAllGoals(@RequestAttribute String userId) {
        return goalService.getGoalsByUser(userId);
    }


    // Download all goals as JSON
    @GetMapping("/export")
    public List<Goal> exportGoals() {
        return goalService.getAllGoals();
    }

    // Upload goals with mode: append or reset
    @PostMapping("/import")
    public List<Goal> importGoals(@RequestBody Map<String, Object> payload, @RequestAttribute String userId) {
        String mode = (String) payload.getOrDefault("mode", "append");
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        List<Goal> goals = mapper.convertValue(payload.get("goals"), new com.fasterxml.jackson.core.type.TypeReference<List<Goal>>() {});
        for (Goal goal : goals) {
            if (goal.getProgressType() == null ||
                !(goal.getProgressType().equalsIgnoreCase("hr") || goal.getProgressType().equalsIgnoreCase("cnt"))) {
                throw new IllegalArgumentException("progressType must be 'hr' or 'cnt' (case-insensitive)");
            }
            goal.setProgressType(goal.getProgressType().toLowerCase());
            goal.setUserId(userId);
        }
        if ("reset".equalsIgnoreCase(mode)) {
            goalService.deleteGoalsByUser(userId);
        }
        return goalService.saveAllGoals(goals);
    }
}
