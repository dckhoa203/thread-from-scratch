package phase1.threadmentalmodel.s4.heapisshared;

public class Lab08SharedImmutable {

    static final String ENV = "PROD";

    public static void main(String[] args) throws InterruptedException {

        Thread t1 = new Thread(() -> inspect(), "T1");
        Thread t2 = new Thread(() -> inspect(), "T2");

        t1.start();
        t2.start();

        t1.join();
        t2.join();
    }

    static void inspect() {
        String localEnv = ENV;
        System.out.printf(
                "[SHARED_IMMUTABLE_READ] thread=%s localEnv==ENV:%s value=%s%n",
                Thread.currentThread().getName(),
                localEnv == ENV,
                localEnv
        );
    }
}
