package phase1.threadmentalmodel.s4.heapisshared;

public class Lab03SharedMutation {

    static final Box SHARED_BOX = new Box();

    public static void main(String[] args) throws InterruptedException {

        Thread write = new Thread(() -> {
            SHARED_BOX.value = 100;

            System.out.printf("[WRITE] thread=Writer SHARED_BOX.value=%d%n", SHARED_BOX.value);
        }, "Writer");


        write.start();
        write.join();
        System.out.println("[ORDER] writer.join() returned; starting reader now");

        Thread reader = new Thread(() -> {
            System.out.printf("[READ_AFTER_JOIN] thread=Reader SHARED_BOX.value=%d%n", SHARED_BOX.value);
        }, "Reader");

        reader.start();
        reader.join();
    }

    static class Box {
        int value;
    }
}
