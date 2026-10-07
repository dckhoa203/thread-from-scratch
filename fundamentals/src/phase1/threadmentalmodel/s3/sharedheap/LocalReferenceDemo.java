package phase1.threadmentalmodel.s3.sharedheap;

import java.util.ArrayList;

public class LocalReferenceDemo {

    private static final ArrayList<String> SHARED_LIST = new ArrayList<>();

    public static void main(String[] args) {
        SHARED_LIST.add("created by main");

        Thread t1 = new Thread(LocalReferenceDemo::inspectSharedList, "worker-1");
        Thread t2 = new Thread(LocalReferenceDemo::inspectSharedList, "worker-2");

        t1.start();
        t2.start();
    }

    private static void inspectSharedList() {
        ArrayList<String> localReference = SHARED_LIST;

        System.out.println(
                Thread.currentThread().getName()
                        + " -> localReference == SHARED_LIST: "
                        + (localReference == SHARED_LIST)
                        + ", contents=" + localReference
        );
    }
}
