package ru.draen.verif;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithBody;
import com.github.javaparser.ast.nodeTypes.NodeWithCondition;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.GenericVisitorWithDefaults;
import ru.draen.verif.tac.*;

import java.util.Optional;


public class TACVisitor extends GenericVisitorWithDefaults<TACValue, TACContext> {
    private final TACRegistry registry;
    private final LabelVisitor labelVisitor = new LabelVisitor();

    public TACVisitor(TACRegistry registry) {
        this.registry = registry;
    }

    @Override
    public TACValue defaultAction(Node n, TACContext ctx) {
        n.getChildNodes().forEach(node -> node.accept(this, ctx));
        return new TACValue.Unknown(n);
    }

    //region operations
    private TACValue.Reference handleBinary(Expression left, Expression right, BinaryExpr.Operator op, TACContext ctx) {
        var arg1 = left.accept(this, ctx);
        var arg2 = right.accept(this, ctx);
        var tacOp = TACOperation.getByCode(op.asString());
        return registry.register(new TACStmt.Assign(arg1, arg2, tacOp));
    }
    @Override
    public TACValue visit(final BinaryExpr n, final TACContext ctx) {
        return handleBinary(n.getLeft(), n.getRight(), n.getOperator(), ctx);
    }

    @Override
    public TACValue visit(UnaryExpr n, TACContext ctx) {
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
                return registry.register(new TACStmt.Assign(new TACValue.Const(1), arg, TACOperation.MINUS));
            }
            default -> {
                return new TACValue.Unknown(op);
            }
        }
    }

    @Override
    public TACValue visit(AssignExpr n, TACContext ctx) {
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
    public TACValue visit(VariableDeclarator n, TACContext ctx) {
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
    public TACValue visit(EnclosedExpr n, TACContext ctx) {
        return n.getInner().accept(this, ctx);
    }

    //endregion

    //region names

    @Override
    public TACValue visit(NameExpr n, TACContext ctx) {
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
    public TACValue visit(IfStmt n, TACContext ctx) {
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
    public TACValue visit(ForStmt n, TACContext ctx) {
        n.getInitialization().forEach(node -> node.accept(this, ctx));
        var startLabel = TACLabel.create();
        var afterLabel = TACLabel.create();
        ctx.enterLoop(startLabel, afterLabel, getStmtLabel(n));
        registry.register(startLabel);

        n.getCompare().ifPresent(compare -> {
            var condition = compare.accept(this, ctx);
            registry.register(new TACStmt.IfFalse(condition, afterLabel));
        });
        n.getBody().accept(this, ctx);
        n.getUpdate().forEach(node -> node.accept(this, ctx));

        registry.register(new TACStmt.GoTo(startLabel));
        registry.register(afterLabel);
        ctx.exitLoop();
        return TACValue.NONE;
    }

    private <T extends Statement & NodeWithBody<T> & NodeWithCondition<T>>
    TACValue handleWhile(T n, TACContext ctx, boolean isReverse) {
        var startLabel = TACLabel.create();
        var afterLabel = TACLabel.create();
        ctx.enterLoop(startLabel, afterLabel, getStmtLabel(n));
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
        ctx.exitLoop();
        return TACValue.NONE;
    }
    @Override
    public TACValue visit(DoStmt n, TACContext ctx) {
        return handleWhile(n, ctx, true);
    }

    @Override
    public TACValue visit(WhileStmt n, TACContext ctx) {
        return handleWhile(n, ctx, false);
    }

    @Override
    public TACValue visit(BreakStmt n, TACContext ctx) {
        var target = n.getLabel().map(label -> ctx.breakLoop(label.getIdentifier()))
                .orElseGet(ctx::breakLoop);
        return registry.register(new TACStmt.GoTo(target));
    }

    @Override
    public TACValue visit(ContinueStmt n, TACContext ctx) {
        var target = n.getLabel().map(label -> ctx.continueLoop(label.getIdentifier()))
                .orElseGet(ctx::continueLoop);
        return registry.register(new TACStmt.GoTo(target));
    }

    //endregion

    //region methods

    @Override
    public TACValue visit(MethodDeclaration n, TACContext ctx) {
        var methodLabel = TACLabel.create("_method_" + n.getName().getIdentifier());
        registry.register(methodLabel);
        registry.register(new TACStmt.BeginFunc());
        n.getBody().ifPresent(body -> body.accept(this, ctx));
        registry.register(new TACStmt.EndFunc());
        return TACValue.NONE;
    }

    //endregion

    // region literals
    @Override
    public TACValue visit(BooleanLiteralExpr n, TACContext arg) {
        return new TACValue.Const(n.getValue());
    }
    @Override
    public TACValue visit(CharLiteralExpr n, TACContext arg) {
        return new TACValue.Const(n.getValue());
    }
    @Override
    public TACValue visit(DoubleLiteralExpr n, TACContext arg) {
        return new TACValue.Const(n.getValue());
    }
    @Override
    public TACValue visit(IntegerLiteralExpr n, TACContext arg) {
        return new TACValue.Const(n.getValue());
    }
    @Override
    public TACValue visit(LongLiteralExpr n, TACContext arg) {
        return new TACValue.Const(n.getValue());
    }
    @Override
    public TACValue visit(NullLiteralExpr n, TACContext arg) {
        return new TACValue.Const("null");
    }
    @Override
    public TACValue visit(StringLiteralExpr n, TACContext arg) {
        return new TACValue.Const(n.getValue());
    }
    @Override
    public TACValue visit(TextBlockLiteralExpr n, TACContext arg) {
        return new TACValue.Const(n.getValue());
    }
    // endregion
}
