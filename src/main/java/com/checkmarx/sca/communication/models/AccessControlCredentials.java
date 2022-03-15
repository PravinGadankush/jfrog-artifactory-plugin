package com.checkmarx.sca.communication.models;

import org.jetbrains.annotations.NotNull;

public class AccessControlCredentials {
    private final String _username;
    private final String _password;
    private final String _tenant;

    public AccessControlCredentials(@NotNull String username, @NotNull String password, @NotNull String tenant)
    {
        _username = username;
        _password = password;
        _tenant = tenant;
    }

    public String getUsername() {
        return _username;
    }

    public String getPassword() {
        return _password;
    }

    public String getTenant() {
        return _tenant;
    }
}
