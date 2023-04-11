package home.barcooda.dlt.utils.maven;

public class Constants {

	private Constants() {

	}
	public static final String REST_CONFIG_FILE_NAME = "rest_config.json";
	public static final String REST_YAML_HEADER_FILE_NAME = "header.yaml";
	public static final String REST_YAML_FILE_NAME = "swagger.yaml";

	public static final String RUNTIME_EXCEPTION_MISSING_FILE = "missing .dltenv file in directory";

	public static final String DLT_ENVIRONMENT_FILE = ".dltenv";
	public static final String DLT_ENVIRONMENT_BASE_PATH = "dlt_environment_base_path";

	public static final String DLT_GROUP_MAVEN_UTILS = "home.barcooda";
	public static final String DLT_ARTIFACT_MAVEN_UTILS = "dltMavenUtils";

	public static final String PATH_SLASH = "/";

	public static final String XML_XSLT_FILE_NAME = "xmlFormat.xslt";

	public static final String ENVIRONMENT_BASE_PATH = "{" + DLT_ENVIRONMENT_BASE_PATH + "}";

	public static final String MAVEN_UPDATE_INFORMATION = "mavenUpdateInformation";
	
}
