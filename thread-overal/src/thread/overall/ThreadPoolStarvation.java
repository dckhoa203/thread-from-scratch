package thread.overall;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPoolStarvation {

    public static void main(String[] args) {

        ExecutorService pool = Executors.newFixedThreadPool(4);

        for (int i = 0; i < 100; i++) {
            int taskId = i;

            System.out.println("Submitting task " + taskId);

            pool.submit(() -> {
                System.out.println("Running task " + taskId + " on " + Thread.currentThread());

                try {
                    Thread.sleep(10_000);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                System.out.println("Finished task " + taskId);
            });
        }
    }
}
