package virtualthread.continuation;

public class MyTask extends ContinuationTask {

    public MyTask(int id) {
        super(id);
    }

    @Override
    public void resume() {

        switch (state) {

            case 0 -> {
                System.out.println(Thread.currentThread().getName() + " run " + this + " state=0 -> Step A");

                yieldC(1);
            }

            case 1 -> {
                System.out.println(Thread.currentThread().getName() + " run " + this + " state=1 -> Step B");

                yieldC(2);
            }

            case 2 -> {
                System.out.println(Thread.currentThread().getName() + " run " + this + " state=2 -> Step C");

                done = true;
            }

            default -> throw new IllegalStateException("Unknown state: " + state);
        }
    }
}
