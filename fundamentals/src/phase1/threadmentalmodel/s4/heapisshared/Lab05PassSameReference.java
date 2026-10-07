package phase1.threadmentalmodel.s4.heapisshared;

public class Lab05PassSameReference {

    public static void main(String[] args) throws InterruptedException {

        Box sharedBox = new Box();

        // Both tasks capture and pass this same local reference from main.
        Thread t1 = new Thread(() -> inspect(sharedBox), "T1");
        Thread t2 = new Thread(() -> inspect(sharedBox), "T2");

        t1.start();
        t2.start();

        t1.join();
        t2.join();
    }

    static void inspect(Box box) {
        System.out.printf(
                "[PASSED_REFERENCE] thread=%s received Box created once in main; value=%d%n",
                Thread.currentThread().getName(),
                box.value
        );
    }

    static class Box {
        int value;
    }
}
