package com.example.taskservice.api.dto;

import lombok.Data;

@Data
public class TaskRequest {
    private String type;
    private Object payload; // Can be a Map or String, Jackson will handle it
}