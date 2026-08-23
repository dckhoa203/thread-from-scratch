package thread.overall;

public class ContextSwitchHell {

    public static void main(String[] args) {
        int threads  = 20_000;

        for (int i = 0; i < threads; i++) {

            Thread.startVirtualThread(() -> {
                while (true) {

                    Math.sin(System.nanoTime());
                }
            });
        }
    }
}
