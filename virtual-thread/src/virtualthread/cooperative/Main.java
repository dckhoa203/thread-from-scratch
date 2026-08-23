package virtualthread.cooperative;

public class Main {

    public static void main(String[] args) {
        Scheduler.start();

        Scheduler.submit(new VirtualTask(1));
        Scheduler.submit(new VirtualTask(2));
        Scheduler.submit(new VirtualTask(3));
    }
}
