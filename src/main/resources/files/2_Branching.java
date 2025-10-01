class Branching {
    public void testBranching() {
        int a = 5;
        int b = false;
        int c;
        if (a > 10) {
            c = 13;
        } else if (a < 3 && !b) {
            c = 14;
            a -= 1;
        } else c = 2;
    }

    public void testSwitch() {
        int a = 10;
        int b = 3;
        int c;
        switch (a + b) {
            case 1:
            case 3:
            case 5:
                c = 1;
            case 7:
                c = 2;
                break;
            case 14:
            case 13: {
                c = 3;
                break;
            }
            default: c = 4;
        }
    }

    public void testInnerBranching() {
        int a = 5;
        int b = false;
        int c;
        if (a > 10) {
            if (b) c = 15;
            else c = 18;
        } else if (a < 3) {
            c = 14;
            if (!b) c++;
        } else {
            c = 13;
        }
    }
}