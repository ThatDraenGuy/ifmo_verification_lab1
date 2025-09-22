package ru.draen.verif.tac;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum TACOperation {
    OR("||"),
    AND("&&"),
    BINARY_OR("|"),
    BINARY_AND("&"),
    XOR("^"),
    EQUALS("=="),
    NOT_EQUALS("!="),
    LESS("<"),
    GREATER(">"),
    LESS_EQUALS("<="),
    GREATER_EQUALS(">="),
    LEFT_SHIFT("<<"),
    SIGNED_RIGHT_SHIFT(">>"),
    UNSIGNED_RIGHT_SHIFT(">>>"),
    PLUS("+"),
    MINUS("-"),
    MULTIPLY("*"),
    DIVIDE("/"),
    REMAINDER("%");

    private final String codeRepresentation;

    TACOperation(String codeRepresentation) {
        this.codeRepresentation = codeRepresentation;
    }

    public String getCodeRepresentation() {
        return codeRepresentation;
    }

    @Override
    public String toString() {
        return codeRepresentation;
    }

    private final static Map<String, TACOperation> byCode = Arrays.stream(TACOperation.values())
            .collect(Collectors.toMap(
                    TACOperation::getCodeRepresentation,
                    Function.identity()
            ));

    public static TACOperation getByCode(String code) {
        return byCode.get(code);
    }
}
