package com.checkmarx.sca.models;

import com.google.gson.annotations.SerializedName;

public class PackageLicense {
    public PackageLicense (String licenseName){
        _packageLicense = licenseName;
    }

    @SerializedName("PackageLicense")
    private String _packageLicense;

    public String getPackageLicense() {
        return _packageLicense;
    }
}
