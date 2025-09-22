package ru.draen.verif.tac;

public sealed interface TACStmt {
    record Assign(TACValue arg1, TACValue arg2, TACOperation op) implements TACStmt {
        public Assign(TACValue arg) {
            this(arg, null, null);
        }
    }
    record IfFalse(TACValue condition, TACLabel label) implements TACStmt {}
    record GoTo(TACLabel label) implements TACStmt {}
    record BeginFunc() implements TACStmt {}
    record EndFunc() implements TACStmt {}
}
