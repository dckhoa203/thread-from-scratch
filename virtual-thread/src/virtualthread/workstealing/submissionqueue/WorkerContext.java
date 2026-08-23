package virtualthread.workstealing.submissionqueue;

public final class WorkerContext {

    private final ThreadLocal<WorkStealingWorker> currentWorker = new ThreadLocal<>();

    public void register(WorkStealingWorker worker) {
        currentWorker.set(worker);
    }

    public WorkStealingWorker current() {
        return currentWorker.get();
    }

    public void clear() {
        currentWorker.remove();
    }
}
