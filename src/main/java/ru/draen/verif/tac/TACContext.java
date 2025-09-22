package ru.draen.verif.tac;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

public class TACContext {
    private static final String UNNAMED = "_unnamed";
    private final Deque<LoopInfo> loopScopes = new ArrayDeque<>();

    public void enterLoop(TACLabel entryPoint, TACLabel exitPoint, String name) {
        loopScopes.push(new LoopInfo(name == null ? UNNAMED : name, entryPoint, exitPoint));
    }
    public void exitLoop() {
        loopScopes.pop();
    }

    public TACLabel continueLoop() {
        var info = loopScopes.peek();
        return info.entryPoint;
    }
    public TACLabel continueLoop(String name) {
        for (var info : loopScopes) {
            if (Objects.equals(info.name, name)) return info.entryPoint;
        }
        throw new IllegalStateException("unknown loop: " + name);
    }
    public TACLabel breakLoop() {
        var info = loopScopes.peek();
        return info.exitPoint;
    }
    public TACLabel breakLoop(String name) {
        for (var info : loopScopes) {
            if (Objects.equals(info.name, name)) return info.exitPoint;
        }
        throw new IllegalStateException("unknown loop: " + name);
    }

    record LoopInfo(String name, TACLabel entryPoint, TACLabel exitPoint) {}
}
