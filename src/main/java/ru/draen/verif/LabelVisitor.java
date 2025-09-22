package ru.draen.verif;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.visitor.GenericVisitorWithDefaults;

public class LabelVisitor extends GenericVisitorWithDefaults<String, Void> {
    @Override
    public String defaultAction(NodeList n, Void arg) {
        return null;
    }

    @Override
    public String defaultAction(Node n, Void arg) {
        return null;
    }

    @Override
    public String visit(LabeledStmt n, Void arg) {
        return n.getLabel().getIdentifier();
    }
}
