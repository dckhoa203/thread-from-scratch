package phase1.threadmentalmodel.s4.heapisshared;

public class Lab10TurnSingletonStyle {

    static final PaymentService SERVICE = new PaymentService();

    public static void main(String[] args) throws InterruptedException {
        SERVICE.retryCount = 7;

        Thread requestA = new Thread(() -> SERVICE.process("REQ-A"), "request-A");
        Thread requestB = new Thread(() -> SERVICE.process("REQ-B"), "request-B");

        requestA.start();
        requestB.start();

        requestA.join();
        requestB.join();
    }

    static class PaymentService {

        int retryCount;

        void process(String requestId) {
            int localRetry = 0;

            System.out.printf(
                    "[FIELD_VS_LOCAL] thread=%s sameService=%s requestId=%s sharedFieldRetry=%d localRetry=%d%n",
                    Thread.currentThread().getName(),
                    this == SERVICE,
                    requestId,
                    retryCount,
                    localRetry
            );
        }
    }
}
