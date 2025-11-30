package com.example.taskservice.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task implements Serializable {
    private String taskId;
    private String type;     // REPORT, NOTIFICATION
    private String payload;  // Arbitrary JSON
    private TaskStatus status;
    private int attempts;
    private long createdAt;
    private long updatedAt;
    
    // Output fields
    private String result;
    private String errorMessage;
}