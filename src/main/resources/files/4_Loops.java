class Loops {
    public void testLoops() {
        for (int i = 0; i < 16; i++) {
            System.out.println(i);
        }
        System.out.println("next");
        int a = 10;
        while (a-->0) {
            System.out.print("%d ", a);
        }
        System.out.println("\n");
        do {
            System.out.print(a++);
        } while (a < 9);
    }
}