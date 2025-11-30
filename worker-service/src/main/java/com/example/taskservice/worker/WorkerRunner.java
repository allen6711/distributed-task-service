package com.example.taskservice.worker;

import com.example.taskservice.worker.service.WorkerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

@Component
public class WorkerRunner implements CommandLineRunner {

    private final WorkerService workerService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${worker.queue}")
    private String queueName;

    @Value("${worker.delayed-queue}")
    private String delayedQueue;

    public WorkerRunner(WorkerService workerService, RedisTemplate<String, Object> redisTemplate) {
        this.workerService = workerService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void run(String... args) {
        // Start the delayed task poller in a separate thread
        new Thread(this::processDelayedTasks).start();

        // Main processing loop
        log("Worker started. Listening on " + queueName);
        while (true) {
            try {
                // BRPOP: Blocking Right Pop (waits up to 5 seconds)
                // Note: rightPop returns the value, or null if timeout
                Object taskIdObj = redisTemplate.opsForList().rightPop(queueName, Duration.ofSeconds(5));

                if (taskIdObj != null) {
                    workerService.processTask(taskIdObj.toString());
                }
            } catch (Exception e) {
                log("Error in worker loop: " + e.getMessage());
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
        }
    }

    private void processDelayedTasks() {
        while (true) {
            try {
                long now = System.currentTimeMillis();
                // Fetch tasks that are due (score <= now)
                Set<Object> dueTasks = redisTemplate.opsForZSet().rangeByScore(delayedQueue, 0, now);

                if (dueTasks != null && !dueTasks.isEmpty()) {
                    for (Object taskIdObj : dueTasks) {
                        String taskId = taskIdObj.toString();
                        // Move back to main queue
                        redisTemplate.opsForList().leftPush(queueName, taskId);
                        // Remove from delayed set
                        redisTemplate.opsForZSet().remove(delayedQueue, taskId);
                        log("Moved task " + taskId + " from delayed to main queue");
                    }
                }
                // Sleep a bit to prevent tight loop on Redis
                Thread.sleep(1000);
            } catch (Exception e) {
                log("Error in delayed task poller: " + e.getMessage());
            }
        }
    }
    
    private void log(String msg) {
        System.out.println("[WorkerRunner] " + msg);
    }
}