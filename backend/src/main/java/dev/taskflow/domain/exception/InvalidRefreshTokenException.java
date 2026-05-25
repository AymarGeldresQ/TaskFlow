package dev.taskflow.domain.exception;

public class InvalidRefreshTokenException extends DomainException {

    public InvalidRefreshTokenException() {
        super("Refresh token is invalid or has expired");
    }
}
