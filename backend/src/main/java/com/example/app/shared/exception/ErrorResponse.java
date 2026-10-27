// filepath: /backend/src/main/java/com/example/app/shared/exception/ErrorResponse.java
package com.example.app.shared.exception;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        int status,
        String code,
        String message,
        List<FieldErrorItem> errors,
        Instant timestamp
) {
    public record FieldErrorItem(String field, String message) {}

    public static ErrorResponse of(ErrorCode code, String message, List<FieldErrorItem> errors) {
        return new ErrorResponse(code.status().value(), code.name(), message, errors, Instant.now());
    }

    public static ErrorResponse of(ErrorCode code) {
        return of(code, code.defaultMessage(), List.of());
    }
}
