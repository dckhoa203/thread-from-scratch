package virtualthread.continuation;

public class YieldException extends RuntimeException {

    private final int nextState;

    public YieldException(int nextState) {
        this.nextState = nextState;
    }

    public int nextState() {
        return nextState;
    }
}
