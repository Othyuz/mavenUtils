package home.barcooda.dlt.utils.maven;

import home.barcooda.dlt.utils.maven.objects.MavenSearchCriteria;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MavenUpdate {

    private String masterName;
    private String developName;
    private List<MavenSearchCriteria> criteria;
    private List<String> paths;

    public MavenUpdate() {

    }
}
