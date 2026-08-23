package virtualthread.workstealing.submissionqueue;

public final class MiniSleep {

    private final WorkStealingScheduler scheduler;

    public MiniSleep(WorkStealingScheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void sleep(Runnable task, long milliseconds) {
        Thread timerThread = new Thread(()
                -> waitingAndSubmit(task, milliseconds));

        timerThread.start();
    }

    private void waitingAndSubmit(Runnable task, long milliseconds) {
        try {
            Thread.sleep(milliseconds);

            /*
             * Thread hiện tại là mini-sleep-timer.
             *
             * Nó không phải scheduler worker,
             * nên task đi vào SubmissionQueue.
             */
            scheduler.submit(task);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
