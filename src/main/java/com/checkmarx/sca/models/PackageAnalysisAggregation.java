package com.checkmarx.sca.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PackageAnalysisAggregation {
    @SerializedName("packageVulnerabilitiesAggregation")
    private VulnerabilitiesAggregation _vulnerabilitiesAggregation;

    public VulnerabilitiesAggregation getVulnerabilitiesAggregation(){
        return _vulnerabilitiesAggregation;
    }

    @SerializedName("packageLicenses")
    private List<String> _licenses;

    public List<String> getLicenses() {
        return _licenses;
    }

    public void setLicenses(List<String> licenses) {
        this._licenses = licenses;
    }
}
