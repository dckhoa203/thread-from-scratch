package phase1.threadmentalmodel.s4.heapisshared;

public class Lab04LocalVsShared {

    static final Box SHARED_BOX = new Box();

    public static void main(String[] args) throws InterruptedException {

        Thread t1 = new Thread(() -> work(), "T1");
        Thread t2 = new Thread(() -> work(), "T2");

        t1.start();
        t2.start();

        t1.join();
        t2.join();
    }

    static void work() {
        int localValue = 0;
        localValue++;

        System.out.printf(
                "[LOCAL] thread=%s localValue=%d%n",
                Thread.currentThread().getName(),
                localValue
        );
        System.out.printf(
                "[SHARED_FIELD_READ] thread=%s SHARED_BOX.value=%d (read only)%n",
                Thread.currentThread().getName(),
                SHARED_BOX.value
        );
    }

    static class Box {
        int value;
    }
}
