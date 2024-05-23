package home.barcooda.dlt.utils.maven;

import com.fasterxml.jackson.databind.ObjectMapper;
import home.barcooda.dlt.utils.maven.objects.MavenSearchCriteria;
import home.barcooda.dlt.utils.maven.objects.Module;
import home.barcooda.dlt.utils.maven.objects.UpdateError;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@lombok.Getter
@lombok.Setter
public class MavenUtilsMain {

    public static void runMain() {
        // https://stackoverflow.com/questions/6192661/how-to-reference-a-resource-file-correctly-for-jar-and-debugging
        try {
            String mavenUpdateInformationString = DltEnvironment.getProperty(Constants.MAVEN_UPDATE_INFORMATION);
            if (mavenUpdateInformationString == null)
                throw new Exception("'" + Constants.MAVEN_UPDATE_INFORMATION + "' is empty or not defined!");
            ObjectMapper mapper = new ObjectMapper();
            MavenUpdate mavenUpdate = null;
            mavenUpdate = mapper.readValue(mavenUpdateInformationString, MavenUpdate.class);
            if (mavenUpdate.getMasterName() == null || mavenUpdate.getMasterName().length() == 0)
                throw new Exception("'masterName' is empty or not defined in '" + Constants.MAVEN_UPDATE_INFORMATION + "'!");
            if (mavenUpdate.getDevelopName() == null || mavenUpdate.getDevelopName().length() == 0)
                throw new Exception("'developName' is empty or not defined in '" + Constants.MAVEN_UPDATE_INFORMATION + "'!");
            if (mavenUpdate.getPaths() == null || mavenUpdate.getPaths().size() == 0)
                throw new Exception("no 'paths' are defined in '" + Constants.MAVEN_UPDATE_INFORMATION + "'!");
            if (mavenUpdate.getCriteria() == null || mavenUpdate.getCriteria().size() == 0) {
                System.out.println("WARNING: no 'Criteria' are defined in '" + Constants.MAVEN_UPDATE_INFORMATION + "'!");
                if (mavenUpdate.getCriteria() == null)
                    mavenUpdate.setCriteria(new ArrayList<>());
            }
            List<String> invalidPaths = new ArrayList<>();
            List<String> targetDirectories = mavenUpdate.getPaths().stream().map(path -> {
                String newPath = path;
                File filePath = new File(path);
                if (!filePath.isAbsolute()) {
                    newPath = DltEnvironment.replaceDltEnvironmentProperties(path);
                    filePath = new File(newPath);
                }
                if (!filePath.exists())
                    invalidPaths.add(path);
                return newPath;
            }).collect(Collectors.toList());
            if (invalidPaths.size() > 0)
                throw new Exception("'paths' contains invalid directories: " + String.join(" ", invalidPaths));
            for (MavenSearchCriteria criteria : mavenUpdate.getCriteria()) {
                if (criteria.getGroupId() == null || criteria.getGroupId().length() == 0)
                    throw new Exception("no 'groupId' defined in criteria of '" + Constants.MAVEN_UPDATE_INFORMATION + "'!");
                if (criteria.getArtifactId() == null)
                    throw new Exception("every 'artifactId' should be at least empty in '" + Constants.MAVEN_UPDATE_INFORMATION + "'!");
                if (criteria.getVersion() == null)
                    criteria.setVersion("");
            }
            List<Module> modules = MavenUtils.checkPomFiles(targetDirectories, mavenUpdate.getMasterName(), mavenUpdate.getDevelopName());
            if (modules != null && modules.size() > 0) {
                List<UpdateError> validationErrors = MavenUtils.validatePomInformation(modules);
                if (validationErrors != null && validationErrors.size() > 0) {
                    String lineSeparator = System.lineSeparator();
                    StringBuilder errorMessage = new StringBuilder();
                    errorMessage.append("Some modules lack important information!");
                    errorMessage.append(lineSeparator).append(lineSeparator);
                    validationErrors.forEach(validationError -> {
                        errorMessage.append("path: ").append(validationError.getPath()).append(lineSeparator);
                        if (validationError.isGroupIdMissing())
                            errorMessage.append("  groupId is missing").append(lineSeparator);
                        if (validationError.isArtifactIdMissing())
                            errorMessage.append("  artifactId is missing").append(lineSeparator);
                        errorMessage.append(lineSeparator).append(lineSeparator);
                    });
                    throw new Exception(errorMessage.toString());
                } else {
                    if (modules.stream().anyMatch(Module::isHasRevisionAlias))
                        System.out.println("INFORMATION: revision version assignment(s) found - in matching cases the matching properties revision(s) will be updated!");
                    MavenUtils.updatePomInformation(modules, mavenUpdate.getCriteria(), mavenUpdate.getMasterName(), mavenUpdate.getDevelopName());
                }
                System.out.println("INFORMATION: update completed!");
            } else
                throw new Exception("no modules found!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
