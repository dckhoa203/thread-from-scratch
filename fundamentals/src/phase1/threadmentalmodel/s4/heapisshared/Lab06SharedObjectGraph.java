package phase1.threadmentalmodel.s4.heapisshared;

public class Lab06SharedObjectGraph {

    static final Service SERVICE = new Service();

    public static void main(String[] args) throws InterruptedException {

        Thread t1 = new Thread(() -> inspect(), "T1");
        Thread t2 = new Thread(() -> inspect(), "T2");

        t1.start();
        t2.start();

        t1.join();
        t2.join();
    }

    static void inspect() {
        Service service = SERVICE;
        Config config = service.config;
        Cache cache = config.cache;

        System.out.printf(
                "[SHARED_GRAPH] thread=%s service=%s config=%s cache=%s%n",
                Thread.currentThread().getName(),
                service == SERVICE,
                config == SERVICE.config,
                cache == SERVICE.config.cache
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
