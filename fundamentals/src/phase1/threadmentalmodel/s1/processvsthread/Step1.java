package phase1.threadmentalmodel.s1.processvsthread;

public class Step1 {

    public static void main(String[] args) {
        long processId = ProcessHandle.current().pid();

        System.out.printf(
                "process id=%d, current thread=%s%n",
                processId,
                Thread.currentThread().getName()
        );

        Thread worker = new Thread(
                () -> System.out.printf(
                        "process id=%d, current thread=%s%n",
                        ProcessHandle.current().pid(),
                        Thread.currentThread().getName()
                ),
                "worker"
        );

        System.out.println("before start: worker state=" + worker.getState());

        worker.start();
    }
}
