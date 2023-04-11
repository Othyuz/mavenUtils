package home.barcooda.dlt.utils.maven;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class DltEnvironment {

    private static DltEnvironment instance;

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private Map<String, String> properties;

    protected DltEnvironment() {
        String configPath = null;
        try {
            configPath = new File(".").getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (configPath != null) {
            checkForEnvironmentFile(configPath);
        } else {
            throw new RuntimeException(Constants.RUNTIME_EXCEPTION_MISSING_FILE);
        }
    }

    private void checkForEnvironmentFile(String configPath) {
        String dltEnvironmentFileName = configPath + Constants.PATH_SLASH + Constants.DLT_ENVIRONMENT_FILE;
        File dltEnvironmentFile = new File(dltEnvironmentFileName);
        if (dltEnvironmentFile.exists() && !dltEnvironmentFile.isDirectory()) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(dltEnvironmentFileName);
                ResourceBundle resourceBundle = new PropertyResourceBundle(fis);
                Enumeration<String> keys = resourceBundle.getKeys();
                Map<String, String> properties = new HashMap<>();
                while (keys.hasMoreElements()) {
                    String key = keys.nextElement();
                    properties.put(key, resourceBundle.getString(key));
                }
                properties.put(Constants.DLT_ENVIRONMENT_BASE_PATH, configPath);
                setProperties(properties);
            } catch (IOException ioe) {
                throw new RuntimeException(Constants.RUNTIME_EXCEPTION_MISSING_FILE);
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (Exception e) {
                        // ignore exception
                    }
                }
            }
        } else {
            File directory = new File(configPath);
            File parentDirectory = directory.getParentFile();
            if (parentDirectory != null)
                checkForEnvironmentFile(parentDirectory.getAbsolutePath());
            else
                throw new RuntimeException(Constants.RUNTIME_EXCEPTION_MISSING_FILE);
        }
    }

    public String replaceDltEnvironmentProperties(String path) {
        if (path != null) {
            if (path.contains(Constants.ENVIRONMENT_BASE_PATH))
                path = path.replace(Constants.ENVIRONMENT_BASE_PATH, getProperty(Constants.DLT_ENVIRONMENT_BASE_PATH));
        }

        return path;
    }

    public String getProperty(String key) {
        if (getProperties().containsKey(key))
            return getProperties().get(key);
        return null;
    }

    public List<String> getPropertyKeys() {
        List<String> keyList = new ArrayList<>();
        Iterator it = getProperties().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> pair = (Map.Entry<String, String>)it.next();
            keyList.add(pair.getKey());
        }
        return keyList;
    }

    public static synchronized DltEnvironment getInstance() {
        if (DltEnvironment.instance == null) {
            DltEnvironment.instance = new DltEnvironment();
        }
        return DltEnvironment.instance;
    }
}
