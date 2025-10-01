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
        TACLabel prev = labels.put(new TACValue.Reference(records.size()), label);
        if (prev != null) {
            labelNames.put(label, labelNames.get(prev));
            return;
        }
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
        if (value == null) return "";
        if (value instanceof TACValue.Reference ref) {
            return varNames.get(ref);
        }
        return value.toString();
    }

    private int maxLength(List<String> items) {
        int max = 0;
        for (var item : items) {
            if (item != null && item.length() > max) max = item.length();
        }
        return max;
    }

    public String toTabledString() {
        List<String> labelColumn = new ArrayList<>();
        List<String> resColumn = new ArrayList<>();
        List<String> opColumn = new ArrayList<>();
        List<String> arg1Column = new ArrayList<>();
        List<String> arg2Column = new ArrayList<>();

        labelColumn.add("LABEL");
        resColumn.add("RES");
        opColumn.add("OP");
        arg1Column.add("ARG1");
        arg2Column.add("ARG2");
        for (int i = 0; i < records.size(); i++) {
            TACStmt stmt = records.get(i);
            TACValue.Reference ref = new TACValue.Reference(i);

            Optional.ofNullable(labels.get(ref)).ifPresentOrElse(label -> {
                labelColumn.add(labelNames.get(label));
            }, () -> labelColumn.add(""));
            switch (stmt) {
                case TACStmt.Assign(TACValue arg1, TACValue arg2, TACOperation op) -> {
                    resColumn.add(varNames.computeIfAbsent(ref, r -> generateVarName()));
                    opColumn.add(Objects.toString(op, ":="));
                    arg1Column.add(getValueString(arg1));
                    arg2Column.add(getValueString(arg2));
                }
                case TACStmt.BeginFunc beginFunc -> {
                    opColumn.add("FUNC_BEGIN");
                    resColumn.add("");
                    arg1Column.add("");
                    arg2Column.add("");
                }
                case TACStmt.CallFunc(String name) -> {
                    opColumn.add("FUNC_CALL");
                    resColumn.add("");
                    arg1Column.add(name);
                    arg2Column.add("");
                }
                case TACStmt.EndFunc endFunc -> {
                    opColumn.add("FUNC_END");
                    resColumn.add("");
                    arg1Column.add("");
                    arg2Column.add("");
                }
                case TACStmt.GoTo(TACLabel label) -> {
                    opColumn.add("GOTO");
                    resColumn.add("");
                    arg1Column.add(labelNames.get(label));
                    arg2Column.add("");
                }
                case TACStmt.IfFalse(TACValue condition, TACLabel label) -> {
                    opColumn.add("IFF");
                    resColumn.add("");
                    arg1Column.add(getValueString(condition));
                    arg2Column.add(labelNames.get(label));
                }
                case TACStmt.IfTrue(TACValue condition, TACLabel label) -> {
                    opColumn.add("IFT");
                    resColumn.add("");
                    arg1Column.add(getValueString(condition));
                    arg2Column.add(labelNames.get(label));
                }
            }
        }

        int labelMax = maxLength(labelColumn);
        int resMax = maxLength(resColumn);
        int opMax = maxLength(opColumn);
        int arg1Max = maxLength(arg1Column);
        int arg2Max = maxLength(arg2Column);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < records.size(); i++) {
            sb
                    .append(String.format("%-" + labelMax + "s\t", labelColumn.get(i)))
                    .append(String.format("%-" + resMax + "s\t", resColumn.get(i)))
                    .append(String.format("%-" + opMax + "s\t", opColumn.get(i)))
                    .append(String.format("%-" + arg1Max + "s\t", arg1Column.get(i)))
                    .append(String.format("%-" + arg2Max + "s\n", arg2Column.get(i)));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < records.size(); i++) {
            TACStmt stmt = records.get(i);
            TACValue.Reference ref = new TACValue.Reference(i);

            Optional.ofNullable(labels.get(ref)).ifPresent(label -> {
                sb.append(labelNames.get(label)).append(":\n");
            });

            sb.append("\t");
            switch (stmt) {
                case TACStmt.Assign assign -> {
                    sb
                            .append(varNames.computeIfAbsent(ref, r -> generateVarName()))
                            .append("\t:= ");
                    if (assign.op() == TACOperation.NEG) {
                        sb.append("!").append(getValueString(assign.arg1()));
                    } else {
                        sb.append(getValueString(assign.arg1()));
                        if (assign.op() != null) {
                            sb
                                    .append("\t")
                                    .append(assign.op())
                                    .append("\t")
                                    .append(getValueString(assign.arg2()));
                        }
                    }
                }
                case TACStmt.GoTo(TACLabel label) -> sb
                        .append("GOTO ")
                        .append(labelNames.get(label));
                case TACStmt.IfFalse(TACValue condition, TACLabel label) -> sb
                        .append("IFF ")
                        .append(getValueString(condition))
                        .append(" GOTO ")
                        .append(labelNames.get(label));
                case TACStmt.IfTrue(TACValue condition, TACLabel label) -> sb
                        .append("IFT ")
                        .append(getValueString(condition))
                        .append(" GOTO ")
                        .append(labelNames.get(label));
                case TACStmt.BeginFunc beginFunc -> sb.append("FUNC_BEGIN");
                case TACStmt.EndFunc endFunc -> sb.append("FUNC_END");
                case TACStmt.CallFunc(String name) -> sb.append("FUNC_CALL ").append(name);
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
