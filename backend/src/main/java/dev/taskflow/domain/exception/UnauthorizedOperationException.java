package dev.taskflow.domain.exception;

public class UnauthorizedOperationException extends DomainException {

    public UnauthorizedOperationException(String operation) {
        super(String.format("Not authorized to perform operation: %s", operation));
    }
}
