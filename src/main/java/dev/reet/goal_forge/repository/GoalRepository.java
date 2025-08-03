
package dev.reet.goal_forge.repository;
import java.util.List;

import dev.reet.goal_forge.model.Goal;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GoalRepository extends MongoRepository<Goal, String> {
    List<Goal> findByUserId(String userId);
    List<Goal> findByUserIdIsNull();
    List<Goal> findByUserIdOrderByDisplayOrder(String userId);
}
