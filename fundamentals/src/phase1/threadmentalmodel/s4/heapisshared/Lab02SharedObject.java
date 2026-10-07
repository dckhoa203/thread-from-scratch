package phase1.threadmentalmodel.s4.heapisshared;

public class Lab02SharedObject {

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
        Box localRef = SHARED_BOX;

        System.out.printf(
                "[SAME_OBJECT] thread=%s localRef==SHARED_BOX:%s%n",
                Thread.currentThread().getName(),
                localRef == SHARED_BOX
        );
    }

    static class Box {

    }
}
