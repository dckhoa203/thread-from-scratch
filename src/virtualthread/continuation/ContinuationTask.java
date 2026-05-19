package virtualthread.continuation;

public abstract class ContinuationTask implements Continuation {

    private final int id;

    protected int state = 0;

    protected boolean done = false;

    protected ContinuationTask(int id) {
        this.id = id;
    }

    protected void yieldC(int nextState) {
        state = nextState;

        throw new YieldException(nextState);
    }

    @Override
    public boolean isDone() {
        return done;
    }

    protected int state() {
        return state;
    }

    @Override
    public String toString() {
        return "continuation-" + id;
    }
}
