package virtualthread.simple;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MiniScheduler {

    private final BlockingQueue<Task> queue = new LinkedBlockingQueue<>();

    public void submit(Task task) {
        queue.offer(task);
    }

    public void start() {
        int workers = Runtime.getRuntime().availableProcessors();
        System.out.println("Workers: " + workers);
        for (int i = 0; i < workers; i++) {
            int workerId = i;
            Thread.ofPlatform().start(() -> {
                while (true) {
                    try {
                        Task task = queue.take();
                        System.out.println("worker " + workerId + " run " + task.name());
                        task.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}
