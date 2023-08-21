package com.checkmarx.sca.models;

import com.google.gson.annotations.SerializedName;

public class ArtifactInfo {

    @SerializedName("packageId")
    private String _packageId;

    @SerializedName("legacyPackageId")
    private String _legacyPackageId;

    @SerializedName("name")
    private String _name;

    @SerializedName("version")
    private String _version;

    @SerializedName("type")
    private String _type;

    @SerializedName("releaseDate")
    private String _releaseDate;

    @SerializedName("description")
    private String _description;

    @SerializedName("projectUrl")
    private String _projectUrl;

    @SerializedName("projectHomePage")
    private String _projectHomePage;

    public String getId() {
        return _legacyPackageId;
    }

    public String getPackageType() {
        return _type;
    }
    public String getName() {
        return _name;
    }
    public String getVersion() {
        return _version;
    }

}
