package virtualthread.continuation;

public interface Continuation {

    void resume();

    boolean isDone();
}
