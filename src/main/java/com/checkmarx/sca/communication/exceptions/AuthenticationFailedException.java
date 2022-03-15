package com.checkmarx.sca.communication.exceptions;

import static java.lang.String.format;

public class AuthenticationFailedException extends RuntimeException {
    public AuthenticationFailedException(int statusCode) {
        super(format("Failed to authenticate client with authentication server (Code %d)", statusCode));
    }
}