package com.checkmarx.sca.models;

import com.google.gson.annotations.SerializedName;

public class VulnerabilitiesAggregation {
    @SerializedName("vulnerabilitiesCount")
    private int _vulnerabilitiesCount;

    @SerializedName("maxRiskSeverity")
    private String _maxRiskSeverity;

    @SerializedName("criticalRiskCount")
    private int _criticalRiskCount;

    @SerializedName("maxRiskScore")
    private double _maxRiskScore;

    @SerializedName("highRiskCount")
    private int _highRiskCount;

    @SerializedName("mediumRiskCount")
    private int _mediumRiskCount;

    @SerializedName("lowRiskCount")
    private int _lowRiskCount;

    public int getVulnerabilitiesCount(){
        return _vulnerabilitiesCount;
    }

    public String getMaxRiskSeverity(){
        return _maxRiskSeverity;
    }

    public double getMaxRiskScore(){
        return _maxRiskScore;
    }

    public int getCriticalRiskCount(){
        return _criticalRiskCount;
    }

    public int getHighRiskCount(){
        return _highRiskCount;
    }

    public int getMediumRiskCount(){
        return _mediumRiskCount;
    }

    public int getLowRiskCount(){
        return _lowRiskCount;
    }
}
