package virtualthread.simple;

public interface Task {
    default String name() {
        return "anonymous";
    }

    void run();
}
