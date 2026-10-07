package phase1.threadmentalmodel.s1.processvsthread;

public class Main {
    public static void main(String[] args) {
        System.out.printf(
                "process id=%d, current thread=%s%n",
                ProcessHandle.current().pid(),
                Thread.currentThread().getName()
        );
    }
}
