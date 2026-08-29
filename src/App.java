import model.Task;
import service.TaskManager;

public class App {
    public static void main(String[] args) {
        TaskManager manager = new TaskManager();

        manager.addTask(new Task("1", "Setup Java JDK & VS Code"));
        manager.addTask(new Task("2", "Build Java OOP Portfolio Repository"));

        System.out.println("--- Task List ---");
        for (Task t : manager.getTasks()) {
            System.out.println(t);
        }

        manager.markTaskCompleted("1");

        System.out.println("\n--- Updated Task List ---");
        for (Task t : manager.getTasks()) {
            System.out.println(t);
        }
    }
}