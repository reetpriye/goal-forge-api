package dev.reet.goal_forge.repository;

import dev.reet.goal_forge.model.Goal;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GoalRepository extends MongoRepository<Goal, String> {
}
