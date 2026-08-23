package virtualthread.workstealing.localqueue;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WorkStealingScheduler {

    private final WorkStealingWorker[] workers;

    private final Random random = new Random();

    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(runnable ->
            Thread.ofPlatform().name("timer").unstarted(runnable)
    );

    public WorkStealingScheduler(int nWorkers) {
        workers = new WorkStealingWorker[nWorkers];

        for (int i = 0; i < nWorkers; i++) {
            workers[i] = new WorkStealingWorker(i, this);
        }
    }

    public void start() {
        for (WorkStealingWorker worker : workers) {
            worker.start();
        }
    }

    public void submit(Runnable task) {

        int idx = random.nextInt(workers.length);

        System.out.println("scheduler submit " + task + " -> " + workers[idx].name());
        workers[idx].submit(task);
    }

    public void submitToWorker(Runnable task, int workerId) {
        if (workerId < 0 || workerId >= workers.length) {
            throw new IllegalArgumentException("Unknown worker: " + workerId);
        }

        System.out.println("scheduler submit " + task + " -> " + workers[workerId].name());
        workers[workerId].submit(task);
    }

    public Runnable steal(int thiefId) {

        for (int i = 0; i < workers.length; i++) {

            if (i == thiefId) {
                continue;
            }

            Runnable stolen = workers[i].stealFromHead();

            if (stolen != null) {
                System.out.println("worker-" + thiefId + " steal " + stolen + " from worker-" + i);

                return stolen;
            }
        }

        Thread.onSpinWait();

        return null;
    }

    public void sleep(Runnable task, long millis) {
        System.out.println(Thread.currentThread().getName() + " schedule wake-up " + task + " after " + millis + "ms");

        timer.schedule(() -> {
            System.out.println("timer wake " + task);
            submit(task);
        }, millis, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        for (WorkStealingWorker worker : workers) {
            worker.stop();
        }
        timer.shutdownNow();
    }
}
