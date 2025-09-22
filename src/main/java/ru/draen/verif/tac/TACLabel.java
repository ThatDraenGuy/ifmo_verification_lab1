package ru.draen.verif.tac;

import java.util.Objects;

public class TACLabel {
    private final int id;
    private final String name;

    private TACLabel(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "_L->" + id;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TACLabel tacLabel = (TACLabel) o;
        return id == tacLabel.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }


    private static int labelCount = 0;

    public static TACLabel create() {
        return new TACLabel(labelCount++, null);
    }

    public static TACLabel create(String name) {
        return new TACLabel(labelCount++, name);
    }
}
