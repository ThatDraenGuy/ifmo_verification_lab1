package ru.draen.verif.tac;

import java.util.Objects;

public sealed interface TACValue {
    record Reference(int id) implements TACValue {
        @Override
        public String toString() {
            return "->" + id;
        }
    }
    record Named(String name) implements TACValue {
        @Override
        public String toString() {
            return name;
        }
    }
    record Const(Object value) implements TACValue {
        @Override
        public String toString() {
            return Objects.toString(value);
        }
    }
    record Unknown(Object repr) implements TACValue {
        @Override
        public String toString() {
            return Objects.toString(repr);
        }
    }
    TACValue NONE = new TACValue.Unknown("NONE");
}
