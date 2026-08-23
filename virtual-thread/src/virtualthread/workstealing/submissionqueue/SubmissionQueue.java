package virtualthread.workstealing.submissionqueue;

import java.util.concurrent.ConcurrentLinkedDeque;

public final class SubmissionQueue {

    private final ConcurrentLinkedDeque<Runnable> tasks = new ConcurrentLinkedDeque<>();

    public void submit(Runnable task) {
        tasks.offer(task);
    }

    public Runnable poll() {
        return tasks.poll();
    }
}
