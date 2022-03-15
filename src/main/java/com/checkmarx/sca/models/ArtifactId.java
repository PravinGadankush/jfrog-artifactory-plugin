package com.checkmarx.sca.models;

import com.google.gson.annotations.SerializedName;

public class ArtifactId {

    public final transient String Name;
    public final transient String Version;
    public final transient String PackageType;

    @SerializedName("identifier")
    private String _identifier;

    public ArtifactId(String packageType, String name, String version) {
        Name = name;
        Version = version;
        PackageType = packageType;
    }

    public ArtifactId(String identifier) {
        Name = null;
        Version = null;
        PackageType = null;
        _identifier = identifier;
    }

    public boolean isInvalid(){
        return Name == null || Name.trim().isEmpty()
                || Version == null || Version.trim().isEmpty()
                || PackageType == null || PackageType.trim().isEmpty();
    }

    public String getIdentifier() {
        return _identifier;
    }
}
