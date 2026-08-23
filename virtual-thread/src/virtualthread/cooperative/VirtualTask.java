package virtualthread.cooperative;

public class VirtualTask implements Runnable {

    private final int id;
    private State state = State.START;

    public VirtualTask(int id) {
        this.id = id;
    }

    @Override
    public void run() {
        switch (state) {

            case START -> {
                System.out.println(Thread.currentThread().getName() + " -> " + this + " Step 1");

                state = State.AFTER_SLEEP;

                System.out.println(Thread.currentThread().getName() + " -> " + this + " sleep 1000ms and yield");
                Scheduler.sleep(this, 1000);

                return;
            }

            case AFTER_SLEEP -> {
                System.out.println(Thread.currentThread().getName() + " -> " + this + " Step 2");

                state = State.DONE;
            }

            case DONE -> {

            }
        }
    }

    @Override
    public String toString() {
        return "task-" + id;
    }
}
