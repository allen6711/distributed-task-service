package com.example.taskservice.api.service;

import com.example.taskservice.api.dto.TaskRequest;
import com.example.taskservice.common.Task;
import com.example.taskservice.common.TaskStatus;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TaskService {

    private static final String TASK_QUEUE_KEY = "task:queue";
    private static final String TASK_DATA_PREFIX = "task:data:";

    private final RedisTemplate<String, Object> redisTemplate;

    public TaskService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Task createTask(TaskRequest request) {
        String taskId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        // 1. Construct the internal Task object
        Task task = Task.builder()
                .taskId(taskId)
                .type(request.getType())
                // Convert payload to String if it's an object, or store as is
                // For simplicity here, we assume the serializer handles the Object payload fine,
                // but usually, we store payload as a JSON string.
                .payload(request.getPayload().toString()) 
                .status(TaskStatus.PENDING)
                .attempts(0)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // 2. Save metadata to Redis Hash (Key: task:data:{uuid})
        String taskKey = TASK_DATA_PREFIX + taskId;
        redisTemplate.opsForValue().set(taskKey, task);

        // 3. Push ID to the Queue (Key: task:queue)
        // Left Push adds to the head, Workers will Right Pop from the tail (FIFO)
        redisTemplate.opsForList().leftPush(TASK_QUEUE_KEY, taskId);

        return task;
    }

    public Task getTask(String taskId) {
        String taskKey = TASK_DATA_PREFIX + taskId;
        return (Task) redisTemplate.opsForValue().get(taskKey);
    }
}