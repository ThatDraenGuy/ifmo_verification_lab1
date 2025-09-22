package ru.draen.verif.tac;

import java.util.*;

public class TACRegistry {
    private final List<TACStmt> records = new ArrayList<>();
    private final Map<TACValue.Reference, String> varNames = new HashMap<>();
    private final Map<TACValue.Reference, TACLabel> labels = new HashMap<>();
    private final Map<TACLabel, String> labelNames = new HashMap<>();
    private int labelCounter = 0;
    private int varCounter = 0;

    public TACValue.Reference register(TACStmt record) {
        records.add(record);
        return new TACValue.Reference(records.size() - 1);
    }

    public void registerVarName(TACMark mark) {
        varNames.put(mark.ref(), mark.name());
    }

    public void register(TACLabel label) {
        labels.put(new TACValue.Reference(records.size() - 1), label);
        if (label.getName() != null) {
            labelNames.put(label, label.getName());
        } else {
            labelNames.put(label, "_L" + labelCounter++);
        }
    }

    private String generateVarName() {
        return "_t" + varCounter++;
    }

    private String getValueString(TACValue value) {
        if (value == null) return "null";
        if (value instanceof TACValue.Reference ref) {
            return varNames.get(ref);
        }
        return value.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        Optional.ofNullable(labels.get(new TACValue.Reference(-1))).ifPresent(label -> {
            sb.append(labelNames.get(label)).append(":\n");
        });

        for (int i = 0; i < records.size(); i++) {
            TACStmt stmt = records.get(i);
            TACValue.Reference ref = new TACValue.Reference(i);
            sb.append("\t");
            switch (stmt) {
                case TACStmt.Assign assign -> {
                    sb
                            .append(varNames.computeIfAbsent(ref, r -> generateVarName()))
                            .append("\t:= ")
                            .append(getValueString(assign.arg1()));
                    if (assign.op() != null) {
                        sb
                                .append("\t")
                                .append(assign.op())
                                .append("\t")
                                .append(getValueString(assign.arg2()));
                    }
                }
                case TACStmt.GoTo goTo -> sb
                        .append("GOTO ")
                        .append(labelNames.get(goTo.label()));
                case TACStmt.IfFalse ifFalse -> sb
                        .append("IFF ")
                        .append(getValueString(ifFalse.condition()))
                        .append(" GOTO ")
                        .append(labelNames.get(ifFalse.label()));
                case TACStmt.BeginFunc beginFunc -> sb.append("BeginFunc");
                case TACStmt.EndFunc endFunc -> sb.append("EndFunc");
            }
            sb.append("\n");
            Optional.ofNullable(labels.get(ref)).ifPresent(label -> {
                sb.append(labelNames.get(label)).append(":\n");
            });
        }
        return sb.toString();
    }
}
