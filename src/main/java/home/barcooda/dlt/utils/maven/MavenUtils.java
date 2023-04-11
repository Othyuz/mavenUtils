package home.barcooda.dlt.utils.maven;

import home.barcooda.dlt.utils.maven.objects.MavenSearchCriteria;
import home.barcooda.dlt.utils.maven.objects.Module;
import home.barcooda.dlt.utils.maven.objects.UpdateError;
import home.barcooda.dlt.utils.xml.XmlUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MavenUtils {

    private static final String PATH_SLASH = "/";
    public static final String HYPHEN = "-";
    private static final String XML_TAG_PROJECT = "project";
    private static final String XML_TAG_ARTIFACT_ID = "artifactId";
    private static final String XML_TAG_VERSION = "version";
    private static final String XML_TAG_DEPENDENCIES = "dependencies";
    private static final String XML_TAG_GROUP_ID = "groupId";
    private static final String XML_TAG_PARENT = "parent";
    private static final String FILE_NAME_POM_XML = "pom.xml";
    private static final String GIT_BRANCH_LIST_PREFIX = "refs/heads/";

    private static final String GIT_SNAPSHOT_SUFFIX = "-SNAPSHOT";


    private static final String GIT_FEATURE_BRANCH_PREFIX = "feature/";


    @Setter(AccessLevel.PROTECTED)
    @Getter(AccessLevel.PROTECTED)
    private List<Module> moduleInformation;
    @Setter(AccessLevel.PROTECTED)
    @Getter(AccessLevel.PROTECTED)
    private String gitBranchMaster;
    @Setter(AccessLevel.PROTECTED)
    @Getter(AccessLevel.PROTECTED)
    private String gitBranchDevelop;

    private MavenUtils() {

    }

    public static List<Module> checkPomFiles(List<String> baseFilePaths, String gitBranchMaster, String gitBranchDevelop) {
        MavenUtils pomUpdater = new MavenUtils();
        pomUpdater.setGitBranchMaster(gitBranchMaster);
        pomUpdater.setGitBranchDevelop(gitBranchDevelop);
        return pomUpdater.getPomInformation(baseFilePaths);
    }

    private List<Module> getPomInformation(List<String> baseFilePaths) {
        List<Module> modules = new ArrayList<>();
        if (baseFilePaths != null && baseFilePaths.size() > 0) {


            for (String baseFilePath : baseFilePaths) {
                String gitModuleVersion = null;
                BranchInformation branchInformation = getGitBranchName(baseFilePath);
                if (branchInformation != null
                        && !branchInformation.isMasterBranch() && !branchInformation.isDevelopBranch()) {
                    gitModuleVersion = branchInformation.getBranchName() + GIT_SNAPSHOT_SUFFIX;
                    if (branchInformation.getDevelopVersion() != null)
                        gitModuleVersion = branchInformation.getDevelopVersion() + HYPHEN + gitModuleVersion;
                }
                modules.addAll(getMavenModulesOfPath(baseFilePath, true, gitModuleVersion));
            }

            if (modules.size() > 0) {
                List<String> modulesToIgnore = new ArrayList<>();
                for (Module module : modules) {
                    Document doc = XmlUtils.readXmlFile(getPom(module));
                    assert doc != null;
                    readModuleInformation(module, doc);

                    if (Constants.DLT_GROUP_MAVEN_UTILS.equals(module.getGroupId())
                            && Constants.DLT_ARTIFACT_MAVEN_UTILS.equals(module.getArtifactId()))
                        modulesToIgnore.add(module.getUuid());
                }

                if (modulesToIgnore.size() > 0) {
                    modules = modules.stream().filter(module -> !modulesToIgnore.contains(module.getUuid())).collect(Collectors.toList());
                }
            }
        }

        return modules;
    }

    public static List<UpdateError> validatePomInformation(List<Module> modules) {
        List<UpdateError> updateErrors = new ArrayList<>();
        if (modules != null && modules.size() > 0) {
            modules.forEach(module -> {
                if (module.getGroupId() == null || module.getGroupId().length() == 0
                        || module.getArtifactId() == null || module.getArtifactId().length() == 0) {
                    UpdateError updateError = new UpdateError();
                    Path path = Paths.get(module.getPath());
                    updateError.setPath(path.toAbsolutePath().normalize().toString());
                    if (module.getGroupId() == null || module.getGroupId().length() == 0)
                        updateError.setGroupIdMissing(true);
                    if (module.getArtifactId() == null || module.getArtifactId().length() == 0)
                        updateError.setArtifactIdMissing(true);
                    updateErrors.add(updateError);
                }
            });
        }
        return updateErrors;
    }

    public static void updatePomInformation(List<Module> modules, List<MavenSearchCriteria> criteria,
                                            String gitBranchMaster, String gitBranchDevelop) {
        if (criteria == null)
            criteria = new ArrayList<>();
        MavenUtils pomUpdater = new MavenUtils();
        pomUpdater.setGitBranchMaster(gitBranchMaster);
        pomUpdater.setGitBranchDevelop(gitBranchDevelop);
        List<MavenSearchCriteria> changeCriteria = criteria;
        if (modules != null && modules.size() > 0 && changeCriteria.size() > 0) {
            modules.forEach(module -> changeCriteria.stream().filter(changeCriteriaEntry -> module.getGroupId().equals(changeCriteriaEntry.getGroupId())
                    && module.getArtifactId().startsWith(changeCriteriaEntry.getArtifactId())).findFirst().ifPresent(mavenSearchCriteria -> {
                String newVersion = mavenSearchCriteria.getVersion();
                if (mavenSearchCriteria.isUseBranchName())
                    newVersion = module.getGitModuleVersion();
                module.setVersion(newVersion);
                if (module.getVersion() != null && module.getVersion().length() > 0)
                    module.setUpdatePomHeader(true);
            }));
        }

        pomUpdater.setModuleInformation(modules);

        for (Module module : modules) {
            XmlUtils.updateXmlFile(getPom(module), pomUpdater::updatePomFileContent);
        }
    }

    private List<Module> getMavenModulesOfPath(String baseFilePath, boolean checkSubdirectory, String gitModuleVersion) {

        List<Module> modules = new ArrayList<>();
        File[] directories = new File(baseFilePath).listFiles(File::isDirectory);

        if (directories != null && directories.length > 0) {
            for (File directory : directories) {
                File[] pomFiles = directory.listFiles(pathname -> !pathname.isDirectory() && FILE_NAME_POM_XML.equals(pathname.getName()));

                if (pomFiles != null && pomFiles.length > 0) {
                    modules.add(getNewModule(directory.getAbsolutePath(), gitModuleVersion));
                } else if (checkSubdirectory)
                    modules.addAll(getMavenModulesOfPath(directory.getAbsolutePath(), false, gitModuleVersion));
            }
        }

        if (checkSubdirectory) {
            File[] parentPomFiles = new File(baseFilePath).listFiles(pathname -> !pathname.isDirectory() && FILE_NAME_POM_XML.equals(pathname.getName()));
            if (parentPomFiles != null && parentPomFiles.length > 0) {
                modules.add(getNewModule(baseFilePath, gitModuleVersion));
            }
        }

    return modules;
    }

    public void updatePomFileContent(Document doc) {
        NodeList nodeList = doc.getElementsByTagName(XML_TAG_PROJECT);
        int nodeListLength = nodeList.item(0).getChildNodes().getLength();

        updatePom(nodeList.item(0).getChildNodes(), nodeListLength, true);

        for (int i = 0; i < nodeListLength; i++) {
            Node nodeEntry = nodeList.item(0).getChildNodes().item(i);
            if (XML_TAG_DEPENDENCIES.equals(nodeEntry.getNodeName())) {
                int innerNodeListLength = nodeEntry.getChildNodes().getLength();
                for (int j = 0; j < innerNodeListLength; j++) {
                    updateDependencies(nodeEntry.getChildNodes().item(j).getChildNodes());
                }
            }
        }
    }

    private void updatePom(NodeList nodeList, int nodeListLength, boolean updateDependencies) {
        String moduleGroupId = null;
        String moduleArtifactId = null;
        boolean parentChecked = false;
        for (int i = 0; i < nodeListLength; i++) {
            Node nodeEntry = nodeList.item(i);
            switch (nodeEntry.getNodeName()) {
                case XML_TAG_GROUP_ID:
                    moduleGroupId = nodeEntry.getTextContent();
                    break;
                case XML_TAG_ARTIFACT_ID:
                    moduleArtifactId = nodeEntry.getTextContent();
                    break;
                case XML_TAG_PARENT:
                    parentChecked = true;
                    if (updateDependencies) {
                        NodeList parentNodeList = nodeEntry.getChildNodes();
                        updatePom(parentNodeList, parentNodeList.getLength(), updateDependencies);
                    }
                    break;
            }

            if (moduleGroupId != null && moduleArtifactId != null && parentChecked) {
                break;
            }
        }
        Module module = getModule(getModuleInformation(), moduleGroupId, moduleArtifactId);
        if (module != null && (module.isUpdatePomHeader() || updateDependencies)) {
            for (int i = 0; i < nodeListLength; i++) {
                Node nodeEntry = nodeList.item(i);
                if (XML_TAG_VERSION.equals(nodeEntry.getNodeName())) {
                    nodeEntry.setTextContent(module.getVersion());
                    break;
                }
            }
        }
    }

    private void updateDependencies(NodeList childNodes) {
        int nodeListLength = childNodes.getLength();
        updatePom(childNodes, nodeListLength, true);
    }

    private Module getModule(List<Module> moduleInformation, String groupIdNodeValue, String artifactIdNodeValue) {
        for (Module module : moduleInformation) {
            if (module.getGroupId().equals(groupIdNodeValue) && module.getArtifactId().equals(artifactIdNodeValue))
                return module;
        }
        return null;
    }

    private BranchInformation getGitBranchName(String baseFilePath) {
        try (Git git = Git.open(new File(baseFilePath))) {
            Repository repository = git.getRepository();
            String gitBranchName =  repository.getBranch();
            BranchInformation branchInformation = new BranchInformation();
            if (gitBranchName != null) {
                if (getGitBranchMaster().equals(gitBranchName)) {
                    gitBranchName = null;
                    branchInformation.setMasterBranch(true);
                } else if (getGitBranchDevelop().equals(gitBranchName)) {
                    gitBranchName = null;
                    branchInformation.setDevelopBranch(true);
                } else {
                    if (gitBranchName.startsWith(GIT_FEATURE_BRANCH_PREFIX))
                        gitBranchName = gitBranchName.replace(GIT_FEATURE_BRANCH_PREFIX, "");
                    String developBranchName = GIT_BRANCH_LIST_PREFIX + getGitBranchDevelop();
                    Optional<Ref> masterRef = git.branchList().call().stream().filter(ref ->
                            developBranchName.equals(ref.getName())).findFirst();
                    if (masterRef.isPresent()) {
                        RevWalk walk = new RevWalk(repository);
                        RevCommit commit = walk.parseCommit(masterRef.get().getObjectId());
                        Document doc = XmlUtils.readXmlString(getContent(commit, FILE_NAME_POM_XML, repository));
                        assert doc != null;
                        Module developVersionInformation = readModuleInformation(new Module(), doc);
                        String developVersion = developVersionInformation.getVersion();
                        if (developVersion != null && developVersion.contains(GIT_SNAPSHOT_SUFFIX))
                            developVersion = developVersion.replace(GIT_SNAPSHOT_SUFFIX, "");
                        branchInformation.setDevelopVersion(developVersion);
                    }
                }
            }
            branchInformation.setBranchName(gitBranchName);
            return branchInformation;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Module getNewModule(String baseFilePath, String gitModuleVersion) {
        Module module = new Module();
        module.setPath(baseFilePath);
        module.setGitModuleVersion(gitModuleVersion);
        return module;
    }

    private static String getPom(Module module) {
        return module.getPath() + PATH_SLASH + FILE_NAME_POM_XML;
    }

    private String getContent(RevCommit commit, String path, Repository repository) throws Exception {
        RevTree tree = commit.getTree();
        try (TreeWalk treeWalk = TreeWalk.forPath(repository, path, commit.getTree())) {
            ObjectId blobId = treeWalk.getObjectId(0);
            try (ObjectReader objectReader = repository.newObjectReader()) {
                ObjectLoader objectLoader = objectReader.open(blobId);
                byte[] bytes = objectLoader.getBytes();
                return new String(bytes, StandardCharsets.UTF_8);
            }
        }
    }

    private Module readModuleInformation(Module module, Document doc) {
        NodeList nodeList = doc.getElementsByTagName(XML_TAG_PROJECT);
        int nodeListLength = nodeList.item(0).getChildNodes().getLength();
        for (int i = 0; i < nodeListLength; i++) {
            Node nodeEntry = nodeList.item(0).getChildNodes().item(i);
            switch (nodeEntry.getNodeName()) {
                case XML_TAG_GROUP_ID:
                    module.setGroupId(nodeEntry.getTextContent());
                    break;
                case XML_TAG_ARTIFACT_ID:
                    module.setArtifactId(nodeEntry.getTextContent());
                    break;
                case XML_TAG_VERSION:
                    module.setVersion(nodeEntry.getTextContent());
                    break;
            }

            if (module.getGroupId() != null && module.getArtifactId() != null && module.getVersion() != null) {
                break;
            }
        }
        return module;
    }
}
