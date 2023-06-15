package home.barcooda.dlt.utils.maven.objects;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class Module {

   private String groupId;
   private String artifactId;
   private String version;
   private String path;
   private String uuid;
   private boolean updatePomHeader;
   private String gitModuleVersion;

   private boolean hasRevisionAlias;

   public Module() {
      setUuid(UUID.randomUUID().toString());
   }
}
