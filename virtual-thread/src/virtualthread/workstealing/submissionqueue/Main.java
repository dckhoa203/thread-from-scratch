package virtualthread.workstealing.submissionqueue;

import java.util.concurrent.CountDownLatch;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        WorkStealingScheduler scheduler = new WorkStealingScheduler(3);

        MiniSleep miniSleep = new MiniSleep(scheduler);

        /*
         * Chờ hai task:
         *
         * 1. child-task hoàn thành
         * 2. example-task STEP 2 hoàn thành
         */
        CountDownLatch done = new CountDownLatch(2);

        scheduler.start();

        ExampleTask exampleTask = new ExampleTask(
                scheduler,
                miniSleep,
                done::countDown,
                done::countDown
        );

        /*
         * Main thread không phải worker.
         *
         * exampleTask đi vào SubmissionQueue.
         */
        scheduler.submit(exampleTask);

        /*
         * Chờ exampleTask và child-task hoàn thành.
         */
        done.await();

        scheduler.shutdown();

        System.out.println("Main finished");
    }
}
