package thread.fundamentals.step2;

public class ThreadOwnershipLab {

    public static void main(String[] args) {
        Counter sharedCounter = new Counter();

        // Both workers receive the same object reference.
        Thread t1 = new Thread(() -> work(sharedCounter), "worker-1");
        Thread t2 = new Thread(() -> work(sharedCounter), "worker-2");

        t1.start();
        t2.start();
    }

    static void work(Counter sharedCounter) {
        // Each call has its own local variable in that call's execution context.
        int localValue = 0;
        localValue++;

        System.out.printf(
                "%s -> localValue=%d, sharedCounterIdentityHash=%x, sharedCounter.value=%d%n",
                Thread.currentThread().getName(),
                localValue,
                System.identityHashCode(sharedCounter),
                sharedCounter.value
        );
    }

    static class Counter {
        int value;
    }
}
