# Distributed Task Processing Service

A robust, distributed task queue system built with Java 17, Spring Boot, and Redis. It features reliable queuing, exponential backoff retries, and dead-letter queue management.

## Architecture

1. **API Service**: Accepts HTTP requests, validates input, saves task metadata, and pushes task IDs to a Redis Queue.
2. **Redis**: Acts as the message broker (Lists) and state store (Hashes).
3. **Worker Service**: Consumes tasks via `BRPOP` (blocking pop), processes them, and handles retries via a Delayed Priority Queue (ZSET).

## Features

* **At-Least-Once Delivery**: Ensures tasks are not lost during worker crashes.
* **Exponential Backoff**: Failed tasks retry with increasing delays (e.g., 2s, 4s, 8s).
* **Dead Letter Queue (DLQ)**: Tasks exceeding max attempts are moved to a DLQ for inspection.
* **Scalable**: Workers can be scaled horizontally using Docker Compose.

## Project Structure
```text
distributed-task-service/
├── common/          # Shared data models (Task, Status)
├── api-service/     # REST API (producer)
├── worker-service/  # Task processor (consumer)
├── docker/          # Docker configuration
└── pom.xml          # Parent POM
```

## How to Run

### Prerequisites
* Java 17+
* Maven
* Docker & Docker Compose

### Steps

1.  **Build the project**
    ```bash
    mvn clean package -DskipTests
    ```

2.  **Start the infrastructure**
    ```bash
    cd docker
    docker-compose up --build
    ```
    *This starts Redis, 1 API instance, and 2 Worker instances.*

3.  **Teardown**
    ```bash
    docker-compose down
    ```

## API Usage

### 1. Create a Task (Report)
```bash
curl -X POST http://localhost:8080/tasks \
     -H "Content-Type: application/json" \
     -d '{"type": "REPORT", "payload": {"userId": 123}}'
```

### 2. Check Task Status

# Replace {taskId} with the ID returned from the POST
curl http://localhost:8080/tasks/{taskId}

## Simulation Scenarios
- Success: Send a "REPORT" task. It will sleep for 1.5s and succeed.
- Retry Logic: The code is hardcoded to fail 20% of "REPORT" tasks. Watch the logs to see retries:
  - Processing Task...
  - Task failed...
  - Scheduled retry...

### 3. The Live Demo
1.  **Build**:
    `mvn clean package -DskipTests`
2.  **Run**:
    `cd docker && docker-compose up --build`
3.  **Test Success**:
    Run this curl command:
    ```bash
    curl -X POST http://localhost:8080/tasks \
      -H "Content-Type: application/json" \
      -d '{"type": "REPORT", "payload": "Normal Data"}'
    ```
    *Result:* You should see a log in the `worker-service` container: `Task Completed: {uuid}`.

4.  **Test Retry/Failure**:
    Since we added a 20% random failure chance in `ReportHandler`, verify retries by creating 5-10 tasks quickly:
    ```bash
    for i in {1..10}; do
      curl -X POST http://localhost:8080/tasks \
        -H "Content-Type: application/json" \
        -d "{\"type\": \"REPORT\", \"payload\": \"Load Test $i\"}"
    done
    ```
    *Watch the Logs:* You will eventually see a failure. Look for lines like `Scheduled retry for task...`. Wait a few seconds, and you will see it process again.
