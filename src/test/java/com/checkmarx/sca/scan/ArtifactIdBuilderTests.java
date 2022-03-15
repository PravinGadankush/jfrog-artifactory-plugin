package com.checkmarx.sca.scan;

import org.artifactory.fs.FileLayoutInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;

public class ArtifactIdBuilderTests {

    @DisplayName("Get artifact id with success - NPM")
    @Test
    public void getNpmArtifactIdWithSuccess() {

        var artifactIdBuilder = new ArtifactIdBuilder();
        var fileLayoutInfo = Mockito.mock(FileLayoutInfo.class);

        when(fileLayoutInfo.getBaseRevision()).thenReturn("1.2.3");
        when(fileLayoutInfo.getModule()).thenReturn("test");

        var id = artifactIdBuilder.getArtifactId(fileLayoutInfo, "NPM");

        Assertions.assertEquals("test", id.Name);
        Assertions.assertEquals("1.2.3", id.Version);
        Assertions.assertEquals("NPM", id.PackageType);
    }

    @DisplayName("Get artifact id with success - Maven")
    @Test
    public void getMavenArtifactIdWithSuccess() {

        var artifactIdBuilder = new ArtifactIdBuilder();
        var fileLayoutInfo = Mockito.mock(FileLayoutInfo.class);

        when(fileLayoutInfo.getBaseRevision()).thenReturn("1.2.3");
        when(fileLayoutInfo.getOrganization()).thenReturn("org");
        when(fileLayoutInfo.getModule()).thenReturn("test");

        var id = artifactIdBuilder.getArtifactId(fileLayoutInfo, "MAVEN");

        Assertions.assertEquals("org:test", id.Name);
        Assertions.assertEquals("1.2.3", id.Version);
        Assertions.assertEquals("MAVEN", id.PackageType);
    }

    @DisplayName("Get artifact id with success - Maven with file revision")
    @Test
    public void getMavenArtifactIdWithSuccessWithFileRevision() {

        var artifactIdBuilder = new ArtifactIdBuilder();
        var fileLayoutInfo = Mockito.mock(FileLayoutInfo.class);

        when(fileLayoutInfo.getBaseRevision()).thenReturn("1.2.3");
        when(fileLayoutInfo.getOrganization()).thenReturn("org");
        when(fileLayoutInfo.getFileIntegrationRevision()).thenReturn("2");
        when(fileLayoutInfo.getModule()).thenReturn("test");

        var id = artifactIdBuilder.getArtifactId(fileLayoutInfo, "MAVEN");

        Assertions.assertEquals("org:test", id.Name);
        Assertions.assertEquals("1.2.3-2", id.Version);
        Assertions.assertEquals("MAVEN", id.PackageType);
    }
}
