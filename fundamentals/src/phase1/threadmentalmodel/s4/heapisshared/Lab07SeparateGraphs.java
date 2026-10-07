package phase1.threadmentalmodel.s4.heapisshared;

public class Lab07SeparateGraphs {

    public static void main(String[] args) throws InterruptedException {
        Service[] graphs = new Service[2];

        Thread t1 = new Thread(() -> graphs[0] = new Service(), "T1");
        Thread t2 = new Thread(() -> graphs[1] = new Service(), "T2");

        t1.start();
        t2.start();

        t1.join();
        t2.join();
        System.out.printf(
                "[GRAPH_COMPARE] sameService=%s sameConfig=%s sameCache=%s (expected all false)%n",
                graphs[0] == graphs[1],
                graphs[0].config == graphs[1].config,
                graphs[0].config.cache == graphs[1].config.cache
        );
    }

    static class Service {
        final Config config = new Config();
    }

    static class Config {
        final Cache cache = new Cache();
    }

    static class Cache {

    }
}
