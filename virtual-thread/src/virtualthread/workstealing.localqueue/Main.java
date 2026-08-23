package virtualthread.workstealing.localqueue;


public class Main {

    public static void main(String[] args) {

        WorkStealingScheduler scheduler = new WorkStealingScheduler(4);

        MiniSleep.scheduler = scheduler;

        scheduler.start();

        for (int i = 1; i <= 8; i++) {
            scheduler.submitToWorker(new VirtualTask(i), 0);
        }

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            scheduler.shutdown();
        }
    }
}
