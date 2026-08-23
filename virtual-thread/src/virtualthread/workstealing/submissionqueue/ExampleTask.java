package virtualthread.workstealing.submissionqueue;

public final class ExampleTask extends VirtualTask {

    private final WorkStealingScheduler scheduler;

    private final MiniSleep miniSleep;

    private final Runnable onChildComplete;

    private final Runnable onTaskComplete;

    public ExampleTask(WorkStealingScheduler scheduler,
                       MiniSleep miniSleep,
                       Runnable onChildComplete,
                       Runnable onTaskComplete) {
        super("example-task");

        this.scheduler = scheduler;
        this.miniSleep = miniSleep;
        this.onChildComplete = onChildComplete;
        this.onTaskComplete = onTaskComplete;
    }

    @Override
    protected void resume() {
        switch (state) {
            case 0 -> stepOne();
            case 1 -> stepTwo();

            default -> throw new IllegalStateException(
                    "Unknown state: " + state);
        }
    }

    private void stepOne() {
        print("STEP 1");

        /*
         * Hiện tại ExampleTask đang chạy
         * bên trong scheduler worker.
         *
         * Child task sẽ đi vào local queue.
         */
        scheduler.submit(() -> {
            try {
                System.out.println("child-task runs on "
                        + Thread.currentThread().getName());
            } finally {
                onChildComplete.run();
            }
        });

        /*
         * Lần tiếp theo chạy state 1.
         */
        moveTo(1);

        /*
         * Đăng ký chạy lại sau 1 giây.
         *
         * Method này không block carrier hiện tại.
         */
        miniSleep.sleep(this, 1_000);

        /*
         * stepOne kết thúc.
         * VirtualTask.run() cũng kết thúc.
         *
         * Carrier được tự do chạy task khác.
         */
    }

    private void stepTwo() {
        print("STEP 2");

        complete();

        onTaskComplete.run();
    }

    private void print(String step) {
        System.out.printf("%s - %s runs on %s%n",
                getName(), step, Thread.currentThread().getName());
    }
}
