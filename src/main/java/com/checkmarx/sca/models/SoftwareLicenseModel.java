package com.checkmarx.sca.models;

import com.google.gson.annotations.SerializedName;

public class SoftwareLicenseModel {
    @SerializedName("name")
    private String _name;

    public String getName() {
        return _name;
    }

}
