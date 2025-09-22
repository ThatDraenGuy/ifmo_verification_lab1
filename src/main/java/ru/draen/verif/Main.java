package ru.draen.verif;

import com.github.javaparser.StaticJavaParser;
import ru.draen.verif.tac.TACContext;
import ru.draen.verif.tac.TACRegistry;

public class Main {
    public static void main(String[] args) {
        var res = StaticJavaParser.parse("""
                class A {
                    public static void test() {
                        int i = 3;
                        int b = (3 - i * 7) / 5;
                    }
                    public static void main() {
                        a += b++;
                        a = ++b;
                        a = 1;
                        b = 2;
                        c = a + b;
                        a -= b;
                        c -= (a + b) * 2;
                        if (c < 0) {
                            c += 1;
                        } else {
                            c -= 1;
                        }
                        test:
                        while (a > 0) {
                            for (int i = 0; i < 15; i++) {
                                if (b) {
                                    break test;
                                }
                            }
                            a++;
                        }
                    }
                }
                """);
        TACRegistry tacRegistry = new TACRegistry();
        TACContext context = new TACContext();
        res.accept(new TACVisitor(tacRegistry   ), context);
        System.out.println(tacRegistry);
    }
}