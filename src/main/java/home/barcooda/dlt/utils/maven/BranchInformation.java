package home.barcooda.dlt.utils.maven;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BranchInformation {

    private String branchName;
    private String developVersion;
    private boolean masterBranch;
    private boolean developBranch;

    public BranchInformation() {

    }
}
