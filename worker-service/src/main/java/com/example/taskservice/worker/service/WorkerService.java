package com.example.taskservice.worker.service;

import com.example.taskservice.common.Task;
import com.example.taskservice.common.TaskStatus;
import com.example.taskservice.worker.handler.TaskHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WorkerService {

    private static final Logger log = LoggerFactory.getLogger(WorkerService.class);
    private static final String TASK_DATA_PREFIX = "task:data:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<String, TaskHandler> handlers;

    @Value("${worker.dead-letter-queue}")
    private String deadLetterQueue;

    @Value("${worker.delayed-queue}")
    private String delayedQueue;

    @Value("${worker.max-attempts}")
    private int maxAttempts;

    @Value("${worker.backoff-factor}")
    private long backoffFactor;

    public WorkerService(RedisTemplate<String, Object> redisTemplate, List<TaskHandler> handlerList) {
        this.redisTemplate = redisTemplate;
        // Map handlers by type for O(1) lookup
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(TaskHandler::getType, Function.identity()));
    }

    public void processTask(String taskId) {
        String taskKey = TASK_DATA_PREFIX + taskId;
        Task task = (Task) redisTemplate.opsForValue().get(taskKey);

        if (task == null) {
            log.warn("Task metadata missing for ID: {}", taskId);
            return;
        }

        try {
            // 1. Mark PROCESSING
            task.setStatus(TaskStatus.PROCESSING);
            task.setUpdatedAt(System.currentTimeMillis());
            task.setAttempts(task.getAttempts() + 1);
            redisTemplate.opsForValue().set(taskKey, task);
            log.info("Processing Task: {} (Attempt {})", taskId, task.getAttempts());

            // 2. Find Handler
            TaskHandler handler = handlers.get(task.getType());
            if (handler == null) {
                throw new IllegalStateException("No handler for type: " + task.getType());
            }

            // 3. Execute Logic
            String result = handler.handle(task);

            // 4. Mark SUCCESS
            task.setStatus(TaskStatus.SUCCEEDED);
            task.setResult(result);
            task.setUpdatedAt(System.currentTimeMillis());
            redisTemplate.opsForValue().set(taskKey, task);
            log.info("Task Completed: {}", taskId);

        } catch (Exception e) {
            handleFailure(task, taskKey, e.getMessage());
        }
    }

    private void handleFailure(Task task, String taskKey, String errorMessage) {
        log.error("Task failed: {} - {}", task.getTaskId(), errorMessage);
        
        task.setUpdatedAt(System.currentTimeMillis());
        task.setErrorMessage(errorMessage);

        if (task.getAttempts() < maxAttempts) {
            // RETRY STRATEGY: Exponential Backoff
            task.setStatus(TaskStatus.PENDING); // Reset to pending
            redisTemplate.opsForValue().set(taskKey, task);

            long delay = backoffFactor * (long) Math.pow(2, task.getAttempts() - 1);
            long processAt = System.currentTimeMillis() + delay;

            // Add to ZSET (Sorted Set) for delayed processing
            redisTemplate.opsForZSet().add(delayedQueue, task.getTaskId(), processAt);
            log.info("Scheduled retry for task {} in {}ms", task.getTaskId(), delay);
        } else {
            // DEAD LETTER
            task.setStatus(TaskStatus.FAILED);
            redisTemplate.opsForValue().set(taskKey, task);
            
            redisTemplate.opsForList().leftPush(deadLetterQueue, task.getTaskId());
            log.error("Task {} moved to Dead Letter Queue", task.getTaskId());
        }
    }
}