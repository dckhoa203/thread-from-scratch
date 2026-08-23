package thread.overall;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VirtualThreadDemo {
    public static void main(String[] args) throws Exception {

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        for (int i = 0; i < 100_000; i++) {

            executor.submit(() -> {

                System.out.println(Thread.currentThread());

                Thread.sleep(10_000);

                return null;
            });

            if (i % 10000 == 0) {
                System.out.println("Submitted: " + i);
            }
        }

        Thread.sleep(Long.MAX_VALUE);
    }
}
