package com.checkmarx.sca.scan;

import com.checkmarx.sca.models.ArtifactId;
import org.artifactory.fs.FileLayoutInfo;
import javax.annotation.Nonnull;

public class ArtifactIdBuilder {

    public ArtifactId getArtifactId(@Nonnull FileLayoutInfo fileLayoutInfo, @Nonnull String packageType){
        var revision = fileLayoutInfo.getBaseRevision();
        var name = fileLayoutInfo.getModule();

        if ("maven".equalsIgnoreCase(packageType)) {
            var organization = fileLayoutInfo.getOrganization();
            var fileIntegrationRevision = fileLayoutInfo.getFileIntegrationRevision();

            name = String.format("%s:%s", organization, name);

            if (fileIntegrationRevision != null) {
                revision = String.format("%s-%s", revision, fileIntegrationRevision);
            }
        }

        return new ArtifactId(packageType, name, revision);
    }
}
