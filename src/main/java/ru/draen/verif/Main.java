package ru.draen.verif;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import ru.draen.verif.tac.ScopeContext;
import ru.draen.verif.tac.TACRegistry;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length > 0) {
            String file = args[0];
            TACRegistry result = process(new File(file));
            System.out.println(result);
            System.out.println("===========================");
            System.out.println(result.toTabledString());
            return;
        }

        File outDir = new File("target/tac");
        outDir.mkdirs();
        for (File javaFile : new File(Main.class.getResource("/files").getFile()).listFiles()) {
            TACRegistry result = process(javaFile);
            System.out.println("FILE " + javaFile.getName() + ":\n");
            System.out.println(result);
            File out = new File("target/tac/" + javaFile.getName());
            try (FileWriter writer = new FileWriter(out, false)) {
                writer.write(result.toString());
            }
            File outTable = new File("target/tac/" + javaFile.getName() + "_table");
            try (FileWriter writer = new FileWriter(outTable, false)) {
                writer.write(result.toTabledString());
            }
        }
    }

    private static TACRegistry process(File file) throws FileNotFoundException {
        CompilationUnit ast = StaticJavaParser.parse(file);
        TACRegistry tacRegistry = new TACRegistry();
        ScopeContext context = new ScopeContext();
        ast.accept(new TACVisitor(tacRegistry), context);
        return tacRegistry;
    }
}