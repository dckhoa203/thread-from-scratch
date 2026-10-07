package phase1.threadmentalmodel.s3.sharedheap;

public class LocalPrimitiveDemo {

    public static void main(String[] args) {

        Thread t1 = new Thread(() -> work("T1"), "worker1");
        Thread t2 = new Thread(() -> work("T2"), "worker2");

        t1.start();
        t2.start();
    }

    static void work(String name) {
        int local = 0;

        for (int i = 0; i < 5; i++) {
            local++;
            System.out.println(name + " local=" + local);
        }
    }
}
