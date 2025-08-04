package dev.reet.goal_forge.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.reet.goal_forge.model.Goal;
import dev.reet.goal_forge.service.GoalService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping
    public Goal createGoal(@RequestBody Goal goal, @RequestAttribute String userId) {
        goal.setUserId(userId);
        return goalService.addGoal(goal);
    }
    
    @DeleteMapping("/{id}")
    public void deleteGoal(@PathVariable String id) {
        goalService.deleteGoal(id);
    }
   
    @PostMapping("/{id}/start")
    public Goal startGoal(@PathVariable String id) {
        return goalService.startGoal(id);
    }
   
    @PostMapping("/{id}/complete")
    public Goal completeGoal(@PathVariable String id) {
        return goalService.completeGoal(id);
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
        return goalService.getGoals(userId);
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
                !(goal.getProgressType().equalsIgnoreCase("dur") || 
                  goal.getProgressType().equalsIgnoreCase("cnt"))) {
                throw new IllegalArgumentException("progressType must be 'dur' or 'cnt' (case-insensitive)");
            }
            goal.setProgressType(goal.getProgressType().toLowerCase());
            goal.setUserId(userId);
        }
        if ("reset".equalsIgnoreCase(mode)) {
            goalService.deleteGoals(userId);
        }
        return goalService.saveAllGoals(goals);
    }

    // Export all user goals as downloadable JSON file
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportGoals(@RequestAttribute String userId) throws Exception {
        List<Goal> goals = goalService.getGoals(userId);
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        byte[] jsonBytes = mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(goals);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentDispositionFormData("attachment", "goals.json");
        return ResponseEntity.ok().headers(headers).body(jsonBytes);
    }

    // Update goal display order
    @PutMapping("/reorder")
    public List<Goal> reorderGoals(@RequestBody Map<String, List<String>> payload, @RequestAttribute String userId) {
        List<String> goalIds = payload.get("goalIds");
        if (goalIds == null || goalIds.isEmpty()) {
            throw new IllegalArgumentException("goalIds is required and cannot be empty");
        }
        return goalService.updateGoalOrders(userId, goalIds);
    }

    // Update/Edit a goal
    @PutMapping("/{id}")
    public Goal updateGoal(@PathVariable String id, @RequestBody Goal updatedGoal, @RequestAttribute String userId) {
        return goalService.updateGoal(id, updatedGoal, userId);
    }
}

@RestController
@RequestMapping("/api")
class PingController {
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }
}