package home.barcooda.dlt.utils.maven.objects;

@lombok.Getter
@lombok.Setter
public class UpdateError {

    private String path;
    private boolean groupIdMissing;
    private boolean artifactIdMissing;

    public UpdateError() {

    }
}
