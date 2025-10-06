package se.gu.api;

import se.gu.api.classifier.RecommendationService;
import se.gu.main.Configuration;
import se.gu.main.ProjectData;
import se.gu.utils.Utilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;

public class Test {

    public static void main(String[] args) {
        test();
    }

    public static void test() {
        FeatRacerAPI api = new  FeatRacerAPI();
        Map<String, List<String>> result;

        try {
            result = invokeFeatRacer("/home/only/Workspaces/Bachelor/ideprojects/HAnS/.git",
                    "/home/only/Workspaces/Bachelor/ideprojects/HAnS/build/idea-sandbox/IC-2025.1/system/featracer-analysis",
                    ".js,.c,.cpp,.h,.cc,.y,.py,.java");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        for (Map.Entry<String, List<String>> entry : result.entrySet()) {
            System.out.println(entry.getKey() + " :::: "  + entry.getValue().toString());
        }
    }

    public static Map<String, List<String>> invokeFeatRacer(String projectPath, String analysisPath, String allowedFileExtensions) throws Exception {
        //Read properties file
        Properties properties = new Properties();
        InputStream inputStream = FeatRacerAPI.class.getResourceAsStream("/config.properties");
        if (inputStream == null) throw new FileNotFoundException("property file 'config.properties' not found in resources");
        properties.load(inputStream);
        // Set properties values
        properties.setProperty("ProjectRepository", projectPath);
        properties.setProperty("AnalysisDirectory",  analysisPath);
        properties.setProperty("AllowedFileExtensions", allowedFileExtensions);
        // properties.setProperty("StartingCommitIndex", String.valueOf(startingCommitIndex));

        //create analysis folder
        Path analysisFolder = Utilities.createOutputDirectory(properties.getProperty("AnalysisDirectory"));
        File analysisDirectory = analysisFolder.toFile();
        System.out.println("Analysis Directory is: " + analysisDirectory.getAbsolutePath());

        //set configuration
        Configuration configuration = Utilities.getConfiguration(properties, analysisDirectory);

        File[] clones = configuration.getClonedRepositories();
        configuration.setCopiedGitRepositories(Arrays.asList(clones));
        configuration.setProjectShortNameMap(properties.getProperty("ProjectShortNames"));
        File repo = configuration.getCopiedGitRepositories().get(0);
        configuration.setProjectRepository(repo);

        //instantiate project data
        ProjectData projectData = new ProjectData(configuration);

        RecommendationService recommendationService = new RecommendationService(projectData);
        return recommendationService.runClassifierAll();
    }

    public static void testSingle() {
        FeatRacerAPI api = new  FeatRacerAPI();
        Map<String, List<String>> result;

        try {
            result = api.initializeProject("/home/only/Workspaces/Bachelor/ideprojects/HAnS/.git",
                    1,
                    "/home/only/Workspaces/Bachelor/ideprojects/HAnS/build/idea-sandbox/IC-2025.1/system/featracer-analysis",
                    ".js,.c,.cpp,.h,.cc,.y,.py,.java");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        for (Map.Entry<String, List<String>> entry : result.entrySet()) {
            System.out.println(entry.getKey() + " :::: "  + entry.getValue().toString());
        }
    }
}
