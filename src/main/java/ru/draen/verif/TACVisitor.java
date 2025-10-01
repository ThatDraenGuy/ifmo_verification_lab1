package ru.draen.verif;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithBody;
import com.github.javaparser.ast.nodeTypes.NodeWithCondition;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.GenericVisitorWithDefaults;
import com.github.javaparser.utils.Pair;
import ru.draen.verif.tac.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class TACVisitor extends GenericVisitorWithDefaults<TACValue, ScopeContext> {
    private final TACRegistry registry;
    private final LabelVisitor labelVisitor = new LabelVisitor();

    public TACVisitor(TACRegistry registry) {
        this.registry = registry;
    }

    @Override
    public TACValue defaultAction(Node n, ScopeContext ctx) {
        n.getChildNodes().forEach(node -> node.accept(this, ctx));
        return new TACValue.Unknown(n);
    }

    //region operations
    private TACValue.Reference handleBinary(Expression left, Expression right, BinaryExpr.Operator op, ScopeContext ctx) {
        var arg1 = left.accept(this, ctx);
        var arg2 = right.accept(this, ctx);
        var tacOp = TACOperation.getByCode(op.asString());
        return registry.register(new TACStmt.Assign(arg1, arg2, tacOp));
    }
    @Override
    public TACValue visit(final BinaryExpr n, final ScopeContext ctx) {
        return handleBinary(n.getLeft(), n.getRight(), n.getOperator(), ctx);
    }

    @Override
    public TACValue visit(UnaryExpr n, ScopeContext ctx) {
        var arg = n.getExpression().accept(this, ctx);
        var op = n.getOperator();
        switch (op) {
            case PLUS -> {
                return arg;
            }
            case MINUS -> {
                return registry.register(new TACStmt.Assign(new TACValue.Const(0), arg, TACOperation.MINUS));
            }
            case PREFIX_INCREMENT -> {
                var targetName = arg instanceof TACValue.Named(String name) ? name : "UNKNOWN";

                var inc = registry.register(new TACStmt.Assign(arg, new TACValue.Const(1), TACOperation.PLUS));
                registry.registerVarName(new TACMark(inc, targetName));
                return registry.register(new TACStmt.Assign(new TACValue.Named(targetName)));
            }
            case PREFIX_DECREMENT -> {
                var targetName = arg instanceof TACValue.Named(String name) ? name : "UNKNOWN";

                var inc = registry.register(new TACStmt.Assign(arg, new TACValue.Const(1), TACOperation.MINUS));
                registry.registerVarName(new TACMark(inc, targetName));
                return registry.register(new TACStmt.Assign(new TACValue.Named(targetName)));
            }
            case POSTFIX_INCREMENT -> {
                var targetName = arg instanceof TACValue.Named(String name) ? name : "UNKNOWN";

                var old = registry.register(new TACStmt.Assign(new TACValue.Named(targetName)));
                var inc = registry.register(new TACStmt.Assign(arg, new TACValue.Const(1), TACOperation.PLUS));
                registry.registerVarName(new TACMark(inc, targetName));
                return old;
            }
            case POSTFIX_DECREMENT -> {
                var targetName = arg instanceof TACValue.Named(String name) ? name : "UNKNOWN";

                var old = registry.register(new TACStmt.Assign(new TACValue.Named(targetName)));
                var inc = registry.register(new TACStmt.Assign(arg, new TACValue.Const(1), TACOperation.MINUS));
                registry.registerVarName(new TACMark(inc, targetName));
                return old;
            }
            case LOGICAL_COMPLEMENT -> {
                return registry.register(new TACStmt.Assign(arg, null, TACOperation.NEG));
            }
            default -> {
                return new TACValue.Unknown(op);
            }
        }
    }

    @Override
    public TACValue visit(AssignExpr n, ScopeContext ctx) {
        var op = n.getOperator();
        var res = op.toBinaryOperator()
                .map(bop -> handleBinary(n.getTarget(), n.getValue(), bop, ctx))
                .orElseGet(() -> {
                    var value = n.getValue().accept(this, ctx);
                    return value instanceof TACValue.Reference ref
                            ? ref
                            : registry.register(new TACStmt.Assign(value));
                });
        registry.registerVarName(new TACMark(res, n.getTarget().toString()));
        return res;
    }

    @Override
    public TACValue visit(VariableDeclarator n, ScopeContext ctx) {
        n.getInitializer().ifPresent(init -> {
            var value = init.accept(this, ctx);
            var res = value instanceof TACValue.Reference ref
                    ? ref
                    : registry.register(new TACStmt.Assign(value));
            registry.registerVarName(new TACMark(res, n.getName().toString()));
        });
        return TACValue.NONE;
    }

    @Override
    public TACValue visit(EnclosedExpr n, ScopeContext ctx) {
        return n.getInner().accept(this, ctx);
    }

    //endregion

    //region names

    @Override
    public TACValue visit(NameExpr n, ScopeContext ctx) {
        var name = n.getName().getIdentifier();
        return new TACValue.Named(name);
    }

    //endregion

    //region flow statements

    private String getStmtLabel(Statement stmt) {
        return stmt.getParentNode().flatMap(parent ->
                        Optional.ofNullable(parent.accept(labelVisitor, null)))
                .orElse(null);
    }

    @Override
    public TACValue visit(IfStmt n, ScopeContext ctx) {
        n.getElseStmt().ifPresentOrElse(elseStmt -> {
            var elseLabel = TACLabel.create();
            var afterLabel = TACLabel.create();
            var condition = n.getCondition().accept(this, ctx);
            registry.register(new TACStmt.IfFalse(condition, elseLabel));
            n.getThenStmt().accept(this, ctx);
            registry.register(new TACStmt.GoTo(afterLabel));
            registry.register(elseLabel);
            elseStmt.accept(this, ctx);
            registry.register(afterLabel);
        }, () -> {
            var afterLabel = TACLabel.create();
            var condition = n.getCondition().accept(this, ctx);
            registry.register(new TACStmt.IfFalse(condition, afterLabel));
            n.getThenStmt().accept(this, ctx);
            registry.register(afterLabel);
        });

        return TACValue.NONE;
    }


    @Override
    public TACValue visit(ForStmt n, ScopeContext ctx) {
        n.getInitialization().forEach(node -> node.accept(this, ctx));
        var startLabel = TACLabel.create();
        var afterLabel = TACLabel.create();
        var stmtLabel = getStmtLabel(n);
        ctx.enterScope(ScopeContext.ScopeType.BREAK, afterLabel, stmtLabel);
        ctx.enterScope(ScopeContext.ScopeType.CONTINUE, startLabel, stmtLabel);
        registry.register(startLabel);

        n.getCompare().ifPresent(compare -> {
            var condition = compare.accept(this, ctx);
            registry.register(new TACStmt.IfFalse(condition, afterLabel));
        });
        n.getBody().accept(this, ctx);
        n.getUpdate().forEach(node -> node.accept(this, ctx));

        registry.register(new TACStmt.GoTo(startLabel));
        registry.register(afterLabel);
        ctx.exitScope(ScopeContext.ScopeType.BREAK);
        ctx.exitScope(ScopeContext.ScopeType.CONTINUE);
        return TACValue.NONE;
    }

    private <T extends Statement & NodeWithBody<T> & NodeWithCondition<T>>
    TACValue handleWhile(T n, ScopeContext ctx, boolean isReverse) {
        var startLabel = TACLabel.create();
        var afterLabel = TACLabel.create();
        var stmtLabel = getStmtLabel(n);
        ctx.enterScope(ScopeContext.ScopeType.BREAK, afterLabel, stmtLabel);
        ctx.enterScope(ScopeContext.ScopeType.CONTINUE, startLabel, stmtLabel);
        registry.register(startLabel);

        if (isReverse) {
            n.getBody().accept(this, ctx);
            var condition = n.getCondition().accept(this, ctx);
            registry.register(new TACStmt.IfFalse(condition, afterLabel));
        } else {
            var condition = n.getCondition().accept(this, ctx);
            registry.register(new TACStmt.IfFalse(condition, afterLabel));
            n.getBody().accept(this, ctx);
        }


        registry.register(new TACStmt.GoTo(startLabel));
        registry.register(afterLabel);
        ctx.exitScope(ScopeContext.ScopeType.BREAK);
        ctx.exitScope(ScopeContext.ScopeType.CONTINUE);
        return TACValue.NONE;
    }
    @Override
    public TACValue visit(DoStmt n, ScopeContext ctx) {
        return handleWhile(n, ctx, true);
    }

    @Override
    public TACValue visit(WhileStmt n, ScopeContext ctx) {
        return handleWhile(n, ctx, false);
    }

    @Override
    public TACValue visit(SwitchStmt n, ScopeContext ctx) {
        var afterLabel = TACLabel.create();
        ctx.enterScope(ScopeContext.ScopeType.BREAK, afterLabel, getStmtLabel(n));
        var selector = n.getSelector().accept(this, ctx);

        List<TACValue.Reference> currentConditions = new ArrayList<>();
        List<Pair<TACLabel, NodeList<Statement>>> blocks = new ArrayList<>();
        for (var entry : n.getEntries()) {
            for (var check : entry.getLabels()) {
                var checkValue = check.accept(this, ctx);
                var condition = registry.register(new TACStmt.Assign(selector, checkValue, TACOperation.EQUALS));
                currentConditions.add(condition);
            }
            if (entry.getStatements().isEmpty()) {
                continue;
            }

            var blockLabel = TACLabel.create();
            if (currentConditions.isEmpty()) {
                //default-ветка
                registry.register(new TACStmt.GoTo(blockLabel));
            } else {
                var fullCondition = currentConditions.getFirst();
                for (var condition : currentConditions.stream().skip(1).toList()) {
                    fullCondition = registry.register(new TACStmt.Assign(fullCondition, condition, TACOperation.OR));
                }
                registry.register(new TACStmt.IfTrue(fullCondition, blockLabel));
            }
            blocks.add(new Pair<>(blockLabel, entry.getStatements()));
            currentConditions.clear();
        }

        for (var pair : blocks) {
            registry.register(pair.a);
            pair.b.forEach(stmt -> stmt.accept(this, ctx));
        }

        registry.register(afterLabel);
        ctx.exitScope(ScopeContext.ScopeType.BREAK);
        return TACValue.NONE;
    }

    @Override
    public TACValue visit(BreakStmt n, ScopeContext ctx) {
        var target = n.getLabel().map(label -> ctx.getTarget(ScopeContext.ScopeType.BREAK, label.getIdentifier()))
                .orElseGet(() -> ctx.getTarget(ScopeContext.ScopeType.BREAK));
        return registry.register(new TACStmt.GoTo(target));
    }

    @Override
    public TACValue visit(ContinueStmt n, ScopeContext ctx) {
        var target = n.getLabel().map(label -> ctx.getTarget(ScopeContext.ScopeType.CONTINUE, label.getIdentifier()))
                .orElseGet(() -> ctx.getTarget(ScopeContext.ScopeType.CONTINUE));
        return registry.register(new TACStmt.GoTo(target));
    }

    //endregion

    //region methods

    @Override
    public TACValue visit(MethodDeclaration n, ScopeContext ctx) {
        var methodLabel = TACLabel.create("_method_" + n.getName().getIdentifier());
        registry.register(methodLabel);
        registry.register(new TACStmt.BeginFunc());
        n.getBody().ifPresent(body -> body.accept(this, ctx));
        registry.register(new TACStmt.EndFunc());
        return TACValue.NONE;
    }

    @Override
    public TACValue visit(MethodCallExpr n, ScopeContext arg) {
        return registry.register(new TACStmt.CallFunc(n.getNameAsString()));
    }

    //endregion

    // region literals
    @Override
    public TACValue visit(BooleanLiteralExpr n, ScopeContext arg) {
        return new TACValue.Const(n.getValue());
    }
    @Override
    public TACValue visit(CharLiteralExpr n, ScopeContext arg) {
        return new TACValue.Const(n.getValue());
    }
    @Override
    public TACValue visit(DoubleLiteralExpr n, ScopeContext arg) {
        return new TACValue.Const(n.getValue());
    }
    @Override
    public TACValue visit(IntegerLiteralExpr n, ScopeContext arg) {
        return new TACValue.Const(n.getValue());
    }
    @Override
    public TACValue visit(LongLiteralExpr n, ScopeContext arg) {
        return new TACValue.Const(n.getValue());
    }
    @Override
    public TACValue visit(NullLiteralExpr n, ScopeContext arg) {
        return new TACValue.Const("null");
    }
    @Override
    public TACValue visit(StringLiteralExpr n, ScopeContext arg) {
        return new TACValue.Const(n.getValue());
    }
    @Override
    public TACValue visit(TextBlockLiteralExpr n, ScopeContext arg) {
        return new TACValue.Const(n.getValue());
    }
    // endregion
}
