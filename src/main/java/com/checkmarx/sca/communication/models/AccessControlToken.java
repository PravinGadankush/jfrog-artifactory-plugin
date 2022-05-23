package com.checkmarx.sca.communication.models;

import com.google.gson.annotations.SerializedName;

import java.time.Duration;
import java.time.Instant;

public class AccessControlToken {

    @SerializedName("access_token")
    private String _accessToken;

    @SerializedName("token_type")
    private String _tokenType;

    @SerializedName("expires_in")
    private int _expiresIn;

    private transient final Instant _requestDate;

    public AccessControlToken() {
        _requestDate = Instant.now();
    }

    public String getAccessToken() {
        return _accessToken;
    }

    public String getTokenType() {
        return _tokenType;
    }

    public double ExpiresIn() {
        var utcNow = Instant.now();

        var expirationDate = _requestDate.plusSeconds(_expiresIn);

        return Duration.between(utcNow, expirationDate).toSeconds();
    }

    public boolean isActive()
    {
        return ExpiresIn() > 0;
    }

    public boolean isBearerToken() {
        return _accessToken != null
                && _tokenType != null
                && _tokenType.equalsIgnoreCase("Bearer")
                && !_accessToken.trim().isEmpty();
    }
}
