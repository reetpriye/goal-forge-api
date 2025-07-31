package dev.reet.goal_forge.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;
import java.time.LocalDate;

@Data
@Document(collection = "goals")
public class Goal {
    @Id
    private String id;
    private String userId; // Reference to User.id, null for anonymous
    private String goalName;
    private String progressType; // Hr or Cnt
    private double estimatedEffort;
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = ProgressCalendarDeserializer.class)
    private Map<String, Double> progressCalendar = new HashMap<>(); // date -> effort
    private double investedEffort = 0.0;
    private double remainingEffort = 0.0;
    private LocalDate startDate; // null until started
    private String status = "NOT_STARTED"; // NOT_STARTED, ACTIVE, PAUSED, COMPLETED
}
