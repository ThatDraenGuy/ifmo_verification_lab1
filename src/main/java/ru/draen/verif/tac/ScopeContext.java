package ru.draen.verif.tac;

import java.util.*;

public class ScopeContext {
    private static final String UNNAMED = "_unnamed";
    private final Map<ScopeType, Deque<ScopeInfo>> scopes = new HashMap<>();

    public void enterScope(ScopeType type, TACLabel targetPoint, String name) {
        scopes.computeIfAbsent(type, t -> new ArrayDeque<>()).push(
                new ScopeInfo(name == null ? UNNAMED : name, targetPoint)
        );
    }
    public void exitScope(ScopeType type) {
        scopes.get(type).pop();
    }

    public TACLabel getTarget(ScopeType type) {
        var info = scopes.get(type).peek();
        return info.targetPoint;
    }
    public TACLabel getTarget(ScopeType type, String name) {
        for (var info : scopes.get(type)) {
            if (Objects.equals(info.name, name)) return info.targetPoint;
        }
        throw new IllegalStateException("unknown scope: " + name);
    }

    record ScopeInfo(String name, TACLabel targetPoint) {}
    public enum ScopeType {
        BREAK,
        CONTINUE;
    }
}
