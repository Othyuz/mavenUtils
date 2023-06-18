package home.barcooda.dlt.utils.maven;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import home.barcooda.dlt.utils.maven.objects.MavenSearchCriteria;
import home.barcooda.dlt.utils.maven.objects.Module;
import home.barcooda.dlt.utils.maven.objects.UpdateError;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {
        MavenUtilsMain.runMain();
    }
}
