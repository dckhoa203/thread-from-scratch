package phase1.threadmentalmodel.s4.heapisshared;

public class Lab01SeparateObjects {

    public static void main(String[] args) throws InterruptedException {
        Box[] boxes = new Box[2];

        Thread t1 = new Thread(() -> boxes[0] = createBox(), "T1");
        Thread t2 = new Thread(() -> boxes[1] = createBox(), "T2");

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.printf(
                "[COMPARE] sameObject=%s (expected false)%n",
                boxes[0] == boxes[1]
        );
    }

    static Box createBox() {
        Box box = new Box();
        System.out.printf("[ALLOC] thread=%s created its own Box%n", Thread.currentThread().getName());
        return box;
    }

    static class Box {

    }
}
