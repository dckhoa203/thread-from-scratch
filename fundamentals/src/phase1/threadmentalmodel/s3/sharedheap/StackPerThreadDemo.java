package phase1.threadmentalmodel.s3.sharedheap;

import java.util.ArrayList;

public class StackPerThreadDemo {

    private static final ArrayList<String> SHARED_LIST = new ArrayList<>();

    public static void main(String[] args) {
        SHARED_LIST.add("created by main");

        Thread t1 = new Thread(StackPerThreadDemo::work, "worker-1");
        Thread t2 = new Thread(StackPerThreadDemo::work, "worker-2");

        t1.start();
        t2.start();
    }

    private static void work() {
        int localPrimitive = 0;
        ArrayList<String> localReference = SHARED_LIST;

        for (int i = 0; i < 3; i++) {
            localPrimitive++;

            System.out.printf(
                    "%s -> localPrimitive=%d, localReference==SHARED_LIST:%s, contents=%s%n",
                    Thread.currentThread().getName(),
                    localPrimitive,
                    localReference == SHARED_LIST,
                    localReference
            );
        }
    }
}
