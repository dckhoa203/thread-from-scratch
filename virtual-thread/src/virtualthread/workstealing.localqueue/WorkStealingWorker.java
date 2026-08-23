package virtualthread.workstealing.localqueue;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.LockSupport;

public class WorkStealingWorker {

    final int id;

    final ConcurrentLinkedDeque<Runnable> deque = new ConcurrentLinkedDeque<>();

    final WorkStealingScheduler scheduler;

    Thread carrier;

    private volatile boolean running;

    public WorkStealingWorker(int id, WorkStealingScheduler scheduler) {
        this.id = id;
        this.scheduler = scheduler;
    }

    public void start() {
        running = true;
        carrier = Thread.ofPlatform().name(name()).start(() -> {
            while (running) {

                Runnable task = pollTask();

                if (task != null) {
                    task.run();
                } else {
                    LockSupport.parkNanos(1_000_000);
                }
            }
        });
    }

    private Runnable pollTask() {
        // 1. local pop first
        Runnable task = deque.pollLast();

        if (task != null) {
            System.out.println(name() + " local-pop " + task);
            return task;
        }

        // 2. steal from others
        return scheduler.steal(id);
    }

    public void submit(Runnable task) {
        deque.offerLast(task);
    }

    public Runnable stealFromHead() {
        return deque.pollFirst();
    }

    public void stop() {
        running = false;
        LockSupport.unpark(carrier);
    }

    public String name() {
        return "worker-" + id;
    }
}
