package virtualthread.continuation;

public class ImplicitReturnTask extends ContinuationTask {

    public ImplicitReturnTask(int id) {
        super(id);
    }

    @Override
    public void resume() {
        if (state < 2) {
            System.out.println(Thread.currentThread().getName() + " run " + this
                    + " state=" + state + " -> partial work, return without yield");
            state++;
            return;
        }

        System.out.println(Thread.currentThread().getName() + " run " + this
                + " state=" + state + " -> final work");
        done = true;
    }
}
