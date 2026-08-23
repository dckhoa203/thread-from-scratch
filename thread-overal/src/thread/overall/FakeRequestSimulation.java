package thread.overall;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FakeRequestSimulation {

    static void dbCall() throws Exception {
        Thread.sleep(50);
    }

    static void partnerCall() throws Exception {
        Thread.sleep(200);
    }

    static void handleRequest(int id) {
        try {
            dbCall();

            partnerCall();

            System.out.println("Done request " + id);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ExecutorService pool = Executors.newFixedThreadPool(8);

        long start = System.currentTimeMillis();

        for (int i = 0; i < 1000; i++) {

            int id = i;

            pool.submit(() -> handleRequest(id));
        }

        System.out.println("Submitted all");
    }
}
