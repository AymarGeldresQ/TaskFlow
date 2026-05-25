package dev.taskflow.infrastructure.rest.advice;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
    String code,
    String message,
    Instant timestamp,
    List<FieldError> errors
) {

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, Instant.now(), null);
    }

    public static ApiError withFieldErrors(String code, String message, List<FieldError> errors) {
        return new ApiError(code, message, Instant.now(), errors);
    }

    public record FieldError(String field, String message) {}
}
