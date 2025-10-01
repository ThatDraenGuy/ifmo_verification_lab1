class Operations {
    public void testOperations() {
        int a = 3;
        int b;
        b = 4;
        int c = a - b;
        int d = c++;
        int e  = ++d - 2 * (a - b ^ 2);
        c += a;
        boolean aBool = false;
        aBool = !aBool;
    }
}