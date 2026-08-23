package virtualthread.workstealing.submissionqueue;

public abstract class VirtualTask implements Runnable {

    private final String name;

    protected int state = 0;

    private boolean done = false;

    public VirtualTask(String name) {
        this.name = name;
    }

    @Override
    public final void run() {
        if (!done) {
            resume();
        }
    }

    /*
     * Class con định nghĩa cách tiếp tục chạy
     * dựa trên state hiện tại.
     */
    protected abstract void resume();

    protected final void moveTo(int nextState) {
        state = nextState;
    }

    protected final void complete() {
        done = true;
    }

    public final String getName() {
        return name;
    }

    public final boolean isDone() {
        return done;
    }
}
