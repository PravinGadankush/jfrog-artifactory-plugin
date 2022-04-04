package com.checkmarx.sca;

public enum PackageManager implements IPackageManager {

    GRADLE("gradle", "maven"),
    MAVEN("maven", "maven"),
    SBT("sbt", "maven"),
    IVY("ivy", "maven"),

    NPM("npm", "npm"),
    BOWER("bower", "npm"),

    GO("go", "go"),

    PYPI("pypi", "python"),

    NUGET("nuget", "nuget"),

    DOCKER("docker", "docker"),

    COMPOSER("composer", "php"),

    COCOAPODS("cocoapods", "ios"),

    NOTSUPPORTED("not-supported", null);

    private final String key;
    private final String packageType;

    PackageManager(String key, String packageType) {
        this.key = key;
        this.packageType = packageType;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public String packageType() {
        return packageType;
    }

    public static PackageManager GetPackageType(String packageType) {

        if (packageType == null) {
            return NOTSUPPORTED;
        }

        switch (packageType.trim().toLowerCase()) {
            case "gradle":
                return GRADLE;
            case "maven":
                return MAVEN;
            case "sbt":
                return SBT;
            case "ivy":
                return IVY;
            case "bower":
                return BOWER;
            case "npm":
                return NPM;
            case "composer":
                return COMPOSER;
            case "docker":
                return DOCKER;
            case "go":
                return GO;
            case "pypi":
                return PYPI;
            case "cocoapods":
                return COCOAPODS;
            case "nuget":
                return NUGET;
            default:
                return NOTSUPPORTED;
        }
    }
}