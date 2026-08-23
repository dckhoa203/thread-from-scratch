package virtualthread.simple;

public class Main {

    public static void main(String[] args) {

        MiniScheduler scheduler = new MiniScheduler();
        scheduler.start();

        for (int i = 0; i < 100_000; i++) {
            int id = i;

            scheduler.submit(new Task() {
                @Override
                public String name() {
                    return "task " + id;
                }

                @Override
                public void run() {
                    System.out.println("task " + id);
                }
            });
        }
    }
}
