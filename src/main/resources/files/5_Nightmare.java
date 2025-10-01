class Nightmamre {
    public void testNightmare() {
        int a = 4;
        int b = 5;
        b += a++;
        int c;
        if (b < a) {
            c = 3;
        } else if (a == b) {
            c = -a;
        } else c = --b;

        for(int i = 0; i < 16; ++i) {
            a += b;
        }

        labelled:
        for(int i = 0; i < 16; ++i) {
            while (a > 0) {
                a -= i;
                if (a == 0) break labelled;
            }
            switch (b) {
                case 1:
                case 12:
                    continue;
                case 42: {
                    i += 1;
                    break labelled;
                }
            }
        }
    }
}