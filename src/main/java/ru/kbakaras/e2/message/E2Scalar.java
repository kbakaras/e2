package ru.kbakaras.e2.message;

public class E2Scalar extends E2AttributeValue {
    private String scalar;

    public E2Scalar(String scalar) {
        this.scalar = scalar;
    }

    public String string() {
        return scalar;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R extends E2Referring<?>> R apply(R referring) {
        return (R) referring.setValue(scalar);
    }

    @Override
    public int hashCode() {
        return scalar.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;

        if (obj instanceof E2Scalar) {
            return scalar.equals(((E2Scalar) obj).scalar);
        }

        return false;
    }
}