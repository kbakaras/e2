package ru.kbakaras.e2.message;

public class E2Value extends E2AttributeValue {
    private String value;

    public E2Value(String value) {
        this.value = value;
    }

    public String string() {
        return value;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R extends E2Referring<?>> R apply(R referring) {
        return (R) referring.setValue(value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;

        if (obj instanceof E2Value) {
            return value.equals(((E2Value) obj).value);
        }

        return false;
    }
}