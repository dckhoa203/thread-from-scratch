package thread.fundamentals.step1;

public class ProcessVsThreadLab {

    public static void main(String[] args) {
        printCurrent("MAIN");

        Thread t1 = new Thread(
                () -> printCurrent("T1"),
                "worker-1"
        );

        Thread t2 = new Thread(
                () -> printCurrent("T2"),
                "worker-2"
        );

        t1.start();
        t2.start();
    }

    static void printCurrent(String label) {
        Thread t = Thread.currentThread();

        System.out.printf(
                "%s -> process id=%d, thread name=%s, thread id=%d%n",
                label,
                ProcessHandle.current().pid(),
                t.getName(),
                t.threadId()
        );
    }
}
