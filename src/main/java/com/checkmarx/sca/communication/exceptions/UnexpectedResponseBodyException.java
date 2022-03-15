package com.checkmarx.sca.communication.exceptions;

import static java.lang.String.format;

public class UnexpectedResponseBodyException extends RuntimeException {
    public UnexpectedResponseBodyException(String message) {
        super(format("Received an unexpected response from the Sca API (Response: %s)", message));
    }
}