package virtualthread.workstealing.localqueue;

public class MiniSleep {

    public static WorkStealingScheduler scheduler;

    public static void sleep(Runnable task, long milli) {
        if (scheduler == null) {
            throw new IllegalStateException("MiniSleep.scheduler has not been initialized");
        }

        scheduler.sleep(task, milli);
    }
}
