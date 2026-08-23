package virtualthread.workstealing.submissionqueue;

public class WorkStealingScheduler {

    private final SubmissionQueue submissionQueue = new SubmissionQueue();

    private final WorkerContext workerContext = new WorkerContext();

    private final WorkStealingWorker[] workers;

    private boolean shutdown;

    public WorkStealingScheduler(int parallelism) {
        if (parallelism <= 0) {
            throw new IllegalArgumentException("parallelism must be greater than 0");
        }

        workers = new WorkStealingWorker[parallelism];

        for (int i = 0; i < parallelism; i++) {
            workers[i] = new WorkStealingWorker(i, this);
        }
    }

    public void start() {
        for (WorkStealingWorker worker : workers) {
            worker.start();
        }
    }

    /*
     * ==================================================
     * HAI ĐƯỜNG SUBMIT
     * ==================================================
     */

    public synchronized void submit(Runnable task) {

        if (shutdown) {
            throw new IllegalStateException("scheduler is shut down");
        }

        WorkStealingWorker currentWorker = workerContext.current();

        if (currentWorker != null) {
            /*
             * INTERNAL SUBMISSION
             *
             * Thread gọi submit đang là worker
             * thuộc scheduler.
             *
             * Task đi vào local queue.
             */
            currentWorker.submitLocal(task);

            System.out.printf("[LOCAL] %s -> worker-%d%n", taskName(task), currentWorker.getId());

            return;
        }

        /*
         * EXTERNAL SUBMISSION
         *
         * Thread gọi submit không thuộc scheduler.
         *
         * Task đi vào SubmissionQueue.
         */
        submissionQueue.submit(task);

        System.out.printf("[SUBMISSION] %s -> global queue%n", taskName(task));
    }

    /*
     * ==================================================
     * WORKER CONTEXT
     * ==================================================
     */

    void register(WorkStealingWorker worker) {
        workerContext.register(worker);
    }

    void clearWorkerContext() {
        workerContext.clear();
    }

    /*
     * ==================================================
     * WORKER TÌM TASK
     * ==================================================
     */

    Runnable nextTask(WorkStealingWorker currentWorker) {

        /*
         * Ưu tiên 1:
         * Local task của worker hiện tại.
         */
        Runnable task = currentWorker.takeLocal();

        if (task != null) {
            return task;
        }

        /*
         * Ưu tiên 2:
         * External task từ SubmissionQueue.
         */
        task = submissionQueue.poll();

        if (task != null) {
            return task;
        }

        /*
         * Ưu tiên 3:
         * Steal task của worker khác.
         */
        return stealFromAnotherWorker(currentWorker);
    }

    private Runnable stealFromAnotherWorker(WorkStealingWorker thief) {
        for (WorkStealingWorker victim : workers) {
            if (victim == thief) {
                continue;
            }

            Runnable task = victim.steal();

            if (task != null) {
                System.out.printf("[STEAL] worker-%d steals %s from worker-%d%n",
                        thief.getId(), taskName(task), victim.getId());

                return task;
            }
        }

        return null;
    }

    public synchronized void shutdown() {
        if (shutdown) {
            return;
        }

        shutdown = true;

        for (WorkStealingWorker worker : workers) {
            worker.shutdown();
        }
    }

    private String taskName(Runnable task) {
        if (task instanceof VirtualTask virtualTask) {
            return virtualTask.getName();
        }

        if (task.getClass().isSynthetic()) {
            return "lambda-task";
        }

        String simpleName = task.getClass().getSimpleName();
        if (simpleName.isBlank()) {
            return "lambda-task";
        }

        return simpleName;
    }
}
