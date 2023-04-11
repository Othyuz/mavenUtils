package home.barcooda.dlt.utils.maven.objects;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MavenSearchCriteria {

    private String groupId;
    private String artifactId;
    private String version;
    private boolean useBranchName;

    public MavenSearchCriteria() {

    }

    public MavenSearchCriteria(String groupId, String artifactId, String version, boolean useBranchName) {
        setGroupId(groupId);
        setArtifactId(artifactId);
        setVersion(version);
        setUseBranchName(useBranchName);
    }
}
