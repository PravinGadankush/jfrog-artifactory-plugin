package com.checkmarx.sca.communication.exceptions;

import static java.lang.String.format;

public class UnexpectedResponseCodeException extends RuntimeException {
    public final int StatusCode;

    public UnexpectedResponseCodeException(int statusCode) {
        super(format("Received an unexpected response code from the Sca API (Code: %d)", statusCode));

        StatusCode = statusCode;
    }
}