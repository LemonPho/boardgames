package com.motomutterers.boardgames.exceptions;

import java.util.HashMap;
import java.util.Map;

public class ValidationBuilder {
    private final Map<String, String> errors = new HashMap<>();

    public ValidationBuilder addError(boolean condition, String field, String message) {
        if (condition) {
            errors.put(field, message);
        }
        return this;
    }

    public void validate() {
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
