package service;

import model.Task;
import java.util.ArrayList;
import java.util.List;

public class TaskManager {
    private final List<Task> tasks = new ArrayList<>();

    public void addTask(Task task) {
        tasks.add(task);
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public boolean markTaskCompleted(String id) {
        for (Task task : tasks) {
            if (task.getId().equals(id)) {
                task.setCompleted(true);
                return true;
            }
        }
        return false;
    }
}