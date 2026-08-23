package virtualthread.workstealing.submissionqueue;

import java.util.concurrent.ConcurrentLinkedDeque;

public final class WorkStealingWorker implements Runnable {

    private final int id;

    private final WorkStealingScheduler scheduler;

    private final ConcurrentLinkedDeque<Runnable> localQueue = new ConcurrentLinkedDeque<>();

    private final Thread carrierThread;

    private volatile boolean running = true;

    public WorkStealingWorker(int id, WorkStealingScheduler scheduler) {
        this.id = id;
        this.scheduler = scheduler;

        this.carrierThread = new Thread(this, "carrier-" + id);
    }

    public int getId() {
        return id;
    }

    public void start() {
        carrierThread.start();
    }

    /*
     * Worker owner đưa task vào cuối deque.
     */
    public void submitLocal(Runnable task) {
        localQueue.offerLast(task);
    }

    /*
     * Worker owner lấy task từ cuối deque.
     *
     * Đây là hướng LIFO:
     * task mới thường được xử lý trước.
     */
    public Runnable takeLocal() {
        return localQueue.pollLast();
    }

    /*
     * Worker khác steal từ đầu đối diện.
     */
    public Runnable steal() {
        return localQueue.pollFirst();
    }

    @Override
    public void run() {
        /*
         * Đánh dấu carrier thread hiện tại
         * thuộc về worker này.
         */
        scheduler.register(this);

        try {
            while (running) {
                Runnable task = scheduler.nextTask(this);

                if (task != null) {
                    try {
                        task.run();
                    } catch (RuntimeException exception) {
                        System.err.printf("Task failed on %s: %s%n",
                                Thread.currentThread().getName(), exception);
                    }
                } else {
                    waitForWork();
                }
            }
        } finally {
            scheduler.clearWorkerContext();
        }
    }

    private void waitForWork() {
        try {
            /*
             * Đây mới là idle strategy đơn giản.
             *
             * park/unpark sẽ được bổ sung
             * ở một step riêng.
             */
            Thread.sleep(1);

        } catch (InterruptedException ignored) {
            /*
             * shutdown() thay running thành false
             * trước khi interrupt worker.
             */
        }
    }

    public void shutdown() {
        running = false;
        carrierThread.interrupt();
    }
}
