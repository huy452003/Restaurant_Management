package com.handle_exceptions;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@ToString
public class TooManyRequestsExceptionHandle extends RuntimeException {
    private final String controllerName;

    public TooManyRequestsExceptionHandle(String message, String controllerName) {
        super(message);
        this.controllerName = controllerName;
    }
}
