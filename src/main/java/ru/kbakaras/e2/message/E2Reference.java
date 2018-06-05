package ru.kbakaras.e2.message;

public class E2Reference extends E2AttributeValue {
    public final String entityName;
    public final String elementUid;

    public E2Reference(String entityName, String elementUid) {
        this.entityName = entityName;
        this.elementUid = elementUid;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R extends E2Referring<?>> R apply(R referring) {
        return (R) referring.setReference(entityName, elementUid);
    }

    @Override
    public int hashCode() {
        return elementUid.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;

        if (obj instanceof E2Reference) {
            return entityName.equals(((E2Reference) obj).entityName) &&
                    elementUid.equals(((E2Reference) obj).elementUid);
        }

        return false;
    }
}