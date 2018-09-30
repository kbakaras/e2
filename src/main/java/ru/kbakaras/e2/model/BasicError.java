package ru.kbakaras.e2.model;

import ru.kbakaras.jpa.ProperEntity;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.time.Instant;

@MappedSuperclass
public abstract class BasicError extends ProperEntity {
    @Column(columnDefinition = "text")
    private String error;

    @Column(columnDefinition = "text")
    private String stackTrace;

    private Instant timestamp;

    public BasicError() {
        timestamp = Instant.now();
    }

    public String getError() {
        return error;
    }
    public void setError(String error) {
        this.error = error;
    }

    public String getStackTrace() {
        return stackTrace;
    }
    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return  "\n____________________________________________________________________\n" +
                this.getClass().getSimpleName() + " (" + getId() + ")\n" + stackTrace +
                "\n____________________________________________________________________";
    }
}