package virtualthread.cooperative;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Scheduler {

    private static final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();

    public static void start() {

        for (int i = 0; i < 2; i++) {
            int workerId = i;

            Thread.ofPlatform().name("worker-" + workerId).start(() -> {
                while (true) {
                    try {
                        Runnable task = queue.take();
                        System.out.println(Thread.currentThread().getName() + " take " + task);
                        task.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public static void submit(Runnable task) {
        System.out.println("main offer " + task);
        queue.offer(task);
    }

    public static void sleep(Runnable task, long millis) {
        System.out.println(Thread.currentThread().getName() + " schedule wake-up for " + task);
        Thread.ofPlatform().name("timer-" + task).start(() -> {
            try {
                Thread.sleep(millis);

                System.out.println(Thread.currentThread().getName() + " offer " + task + " back to queue");
                queue.offer(task);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
