package virtualthread.workstealing;

public class VirtualTask implements Runnable {

    enum State {
        START,
        STEP_1,
        DONE
    }

    private State state = State.START;

    private final int id;

    public VirtualTask(int id) {
        this.id = id;
    }

    @Override
    public void run() {

        switch (state) {
            case START -> {
                System.out.println(Thread.currentThread().getName() + " run " + this + " step-1");

                state = State.STEP_1;

                System.out.println(Thread.currentThread().getName() + " yield " + this + " for sleep");
                MiniSleep.sleep(this, 1000);

                return;
            }

            case STEP_1 -> {
                System.out.println(Thread.currentThread().getName() + " resume " + this + " step-2");

                state = State.DONE;
            }
        }
    }

    @Override
    public String toString() {
        return "task-" + id;
    }
}
