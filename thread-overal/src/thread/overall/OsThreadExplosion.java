package thread.overall;

import java.util.ArrayList;
import java.util.List;

public class OsThreadExplosion {

    public static void main(String[] args) throws Exception {

        List<Thread> threads = new ArrayList<>();

        int count = 0;

        while (true) {

            int id = count++;

            Thread t = new Thread(() -> {

                try {

                    System.out.println("Thread start: " + id);

                    Thread.sleep(1_000_000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            t.start();

            threads.add(t);

            if (count % 1000 == 0) {
                System.out.println("Create threads: " + count);

                Thread.sleep(1000);
            }
        }
    }
}
