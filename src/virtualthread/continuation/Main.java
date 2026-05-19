package virtualthread.continuation;

public class Main {
    public static void main(String[] args) throws Exception {
        ContinuationScheduler scheduler = new ContinuationScheduler(3);

        scheduler.start();

        scheduler.submit(new MyTask(1));
        scheduler.submit(new MyTask(2));
        scheduler.submit(new MyTask(3));

        Thread.sleep(1000);

        scheduler.shutdown();
    }
}
