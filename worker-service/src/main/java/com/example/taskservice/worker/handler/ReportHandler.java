package com.example.taskservice.worker.handler;

import com.example.taskservice.common.Task;
import org.springframework.stereotype.Component;

@Component
public class ReportHandler implements TaskHandler {

    @Override
    public String getType() {
        return "REPORT";
    }

    @Override
    public String handle(Task task) throws Exception {
        // Simulate work
        Thread.sleep(1500); 
        
        // Simulate random failure (20% chance) for testing retries
        if (Math.random() < 0.2) {
            throw new RuntimeException("External Reporting Service Unavailable");
        }
        
        return "Report generated for payload: " + task.getPayload();
    }
}