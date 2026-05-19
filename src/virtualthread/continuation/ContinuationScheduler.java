package virtualthread.continuation;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ContinuationScheduler {

    private final BlockingQueue<Continuation> queue = new LinkedBlockingQueue<>();

    private final Thread[] workers;

    private volatile boolean running;

    public ContinuationScheduler() {
        this(3);
    }

    public ContinuationScheduler(int nWorkers) {
        workers = new Thread[nWorkers];
    }

    public void submit(Continuation c) {
        System.out.println("scheduler submit " + c);
        queue.offer(c);
    }

    public void start() {
        running = true;

        for (int i = 0; i < workers.length; i++) {
            int workerId = i;

            workers[i] = Thread.ofPlatform().name("worker-" + workerId).start(() -> {
                while (running) {
                    try {

                        Continuation c = queue.take();

                        System.out.println(Thread.currentThread().getName() + " take " + c);

                        runContinuation(c);
                    } catch (InterruptedException e) {
                        if (running) {
                            Thread.currentThread().interrupt();
                        }
                        break;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void runContinuation(Continuation c) {
        try {
            c.resume();

            if (c.isDone()) {
                System.out.println(Thread.currentThread().getName() + " done " + c);
            } else {
                System.out.println(Thread.currentThread().getName() + " returned without yield " + c + ", requeue");
                queue.offer(c);
            }
        } catch (YieldException e) {
            System.out.println(Thread.currentThread().getName() + " yield " + c + " nextState=" + e.nextState());
            queue.offer(c);
        } catch (Exception e) {
            System.out.println(Thread.currentThread().getName() + " failed " + c);
            e.printStackTrace();
        }
    }

    public void shutdown() {
        running = false;

        for (Thread worker : workers) {
            if (worker != null) {
                worker.interrupt();
            }
        }
    }
}
