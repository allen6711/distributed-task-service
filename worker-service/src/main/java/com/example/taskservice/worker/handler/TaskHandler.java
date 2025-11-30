package com.example.taskservice.worker.handler;

import com.example.taskservice.common.Task;

public interface TaskHandler {
    /**
     * Executes the task logic. 
     * @return A string result to be stored in the task metadata.
     * @throws Exception if the task fails.
     */
    String handle(Task task) throws Exception;
    
    String getType();
}