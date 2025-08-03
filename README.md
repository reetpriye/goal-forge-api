# Goal Forge API

A RESTful API for managing user goals, tracking progress, and handling goal lifecycle events.

## Base URL
```
/api/goals
```

## Progress Types
The API supports two types of progress tracking:
- **dur** (Duration): Track time-based goals where users can input hours and minutes (e.g., "2hr 30min" gets converted to total minutes)
- **cnt** (Count): Track count-based goals (e.g., "Read 10 books", "Complete 5 projects")

## Endpoints

### Create Goal
- **POST /api/goals**
  - **Request Body:** Goal object
  - **Description:** Creates a new goal with default values.

### Get Goal by ID
- **GET /api/goals/{id}**
  - **Path Variable:** id (String)
  - **Description:** Retrieves a goal by its ID.

### Get All Goals for a User
- **GET /api/goals/user/{userId}**
  - **Path Variable:** userId (String)
  - **Description:** Retrieves all goals for a specific user.


### Delete All Goals for a User
- **DELETE /api/goals/user/{userId}**
  - **Path Variable:** userId (String)
  - **Description:** Deletes all goals for a specific user.

### Batch Save Goals
- **POST /api/goals/batch**
  - **Request Body:** List of Goal objects
  - **Description:** Saves a batch of goals to the repository.


### Add Progress to Goal
- **POST /api/goals/{goalId}/progress**
  - **Path Variable:** goalId (String)
  - **Request Body:** date (LocalDate), effort (double)
  - **Description:** Adds progress effort for a goal on a specific date.

### Start Goal
- **POST /api/goals/{id}/start**
  - **Path Variable:** id (String)
  - **Description:** Starts a goal, setting its status to ACTIVE and start date to today.

### Pause Goal
- **POST /api/goals/{id}/pause**
  - **Path Variable:** id (String)
  - **Description:** Pauses an active goal, setting its status to PAUSED.

### Resume Goal
- **POST /api/goals/{id}/resume**
  - **Path Variable:** id (String)
  - **Description:** Resumes a paused goal, setting its status to ACTIVE.

### Complete Goal
- **POST /api/goals/{id}/complete**
  - **Path Variable:** id (String)
  - **Description:** Marks a goal as completed, setting its status to COMPLETED.

### Delete Goal by ID
- **DELETE /api/goals/{id}**
  - **Path Variable:** id (String)
  - **Description:** Deletes a goal by its ID.

### Delete All Goals for a User
- **DELETE /api/goals/user/{userId}**
  - **Path Variable:** userId (String)
  - **Description:** Deletes all goals for a specific user.

### Delete All Goals
- **DELETE /api/goals**
  - **Description:** Deletes all goals in the system.

### Batch Save Goals
- **POST /api/goals/batch**
  - **Request Body:** List of Goal objects
  - **Description:** Saves a batch of goals to the repository.

---

## Goal Object Example
```json
{
  "id": "string",
  "userId": "string",
  "title": "string",
  "description": "string",
  "estimatedEffort": 10.0,
  "investedEffort": 0.0,
  "remainingEffort": 10.0,
  "status": "NOT_STARTED",
  "startDate": "2025-07-31",
  "progressType": "dur",
  "progressCalendar": {
    "2025-07-31": 2.0
  }
}
```


## Error Handling
- Returns appropriate HTTP status codes and error messages for not found, unauthorized, and invalid operations.

## License
MIT
