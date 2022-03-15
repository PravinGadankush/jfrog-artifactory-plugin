package com.checkmarx.sca.communication.exceptions;

import static java.lang.String.format;

public class UnexpectedAuthenticationResponseException extends RuntimeException {
    public UnexpectedAuthenticationResponseException(String message) {
        super(format("Received an unexpected response from the authentication server (Response: %s)", message));
    }
}