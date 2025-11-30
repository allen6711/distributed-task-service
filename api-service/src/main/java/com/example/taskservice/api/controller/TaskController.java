package com.example.taskservice.api.controller;

import com.example.taskservice.api.dto.TaskRequest;
import com.example.taskservice.api.service.TaskService;
import com.example.taskservice.common.Task;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<Task> submitTask(@RequestBody TaskRequest request) {
        if (request.getType() == null || request.getType().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        Task createdTask = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<Task> getTask(@PathVariable String taskId) {
        Task task = taskService.getTask(taskId);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(task);
    }
}