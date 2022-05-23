package com.checkmarx.sca.models;

import com.google.gson.annotations.SerializedName;

public class PackageRiskAggregation {
    @SerializedName("packageVulnerabilitiesAggregation")
    private VulnerabilitiesAggregation _vulnerabilitiesAggregation;

    public VulnerabilitiesAggregation getVulnerabilitiesAggregation(){
        return _vulnerabilitiesAggregation;
    }
}
