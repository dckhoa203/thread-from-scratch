package phase1.threadmentalmodel.s4.heapisshared;

public class Lab11Aliasing {

    public static void main(String[] args) {

        Box box = new Box();

        Box a = box;
        Box b = box;

        System.out.printf("[ALIAS] a==b:%s%n", a == b);

        a.value = 123;

        System.out.printf("[ALIAS] valueReadThroughB=%d%n", b.value);
    }

    static class Box {
        int value;
    }
}
