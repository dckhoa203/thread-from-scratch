package phase1.threadmentalmodel.s4.heapisshared;

public class Lab09SingletonStyle {

    static final PaymentService SERVICE = new PaymentService();

    public static void main(String[] args) throws InterruptedException {

        Thread requestA = new Thread(() -> SERVICE.process("REQ-A"), "request-A");
        Thread requestB = new Thread(() -> SERVICE.process("REQ-B"), "request-B");

        requestA.start();
        requestB.start();

        requestA.join();
        requestB.join();
    }

    static class PaymentService {

        void process(String requestId) {
            int localRetry = 0;

            System.out.printf(
                    "[SINGLETON_LOCAL] thread=%s sameService=%s requestId=%s localRetry=%d%n",
                    Thread.currentThread().getName(),
                    this == SERVICE,
                    requestId,
                    localRetry
            );
        }
    }
}
