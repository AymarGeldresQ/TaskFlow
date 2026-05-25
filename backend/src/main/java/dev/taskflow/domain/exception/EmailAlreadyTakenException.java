package dev.taskflow.domain.exception;

public class EmailAlreadyTakenException extends DomainException {

    public EmailAlreadyTakenException(String email) {
        super(String.format("Email address '%s' is already in use", email));
    }
}
