package se.gu.api;

import org.apache.lucene.queryparser.classic.ParseException;
import org.repodriller.RepoDriller;
import se.gu.api.classifier.RecommendationService;
import se.gu.main.Configuration;
import se.gu.main.ProjectData;
import se.gu.main.ProjectReader;
import se.gu.main.ProjectReaderWithDriller;
import se.gu.metrics.MetricCalculatorDB;
import se.gu.ml.experiment.ExperimentRunnerDB;
import se.gu.ml.preprocessing.DataGeneratorDB;
import se.gu.utils.Utilities;

import java.io.*;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class FeatRacerAPI {

    /**
     *
     * @param projectPath The path to the project that will be analyzed
     * @param startingCommitIndex The commit to start analyzing from
     * @param analysisPath Path for creation of temporary copy
     * @param allowedFileExtensions Fileextensions to be considered. Format: .java,.cpp,.js
     * @return A list of recommendations, if any
     * @throws Exception
     */
    public Map<String, List<String>> initializeProject(String projectPath, int startingCommitIndex, String analysisPath, String allowedFileExtensions) throws Exception {
        //Read properties file
        Properties properties = new Properties();
        InputStream inputStream = new FileInputStream("config.properties");
        properties.load(inputStream);
        // Set properties values
        properties.setProperty("ProjectRepository", projectPath);
        properties.setProperty("AnalysisDirectory",  analysisPath);
        properties.setProperty("AllowedFileExtensions", allowedFileExtensions);
        properties.setProperty("StartingCommitIndex", String.valueOf(startingCommitIndex));

        //create analysis folder
        Path analysisFolder = Utilities.createOutputDirectory(properties.getProperty("AnalysisDirectory"));
        File analysisDirectory = analysisFolder.toFile();

        //set configuration
        Configuration configuration = Utilities.getConfiguration(properties, analysisDirectory);

        File[] clones = configuration.getClonedRepositories();
        configuration.setCopiedGitRepositories(Arrays.asList(clones));
        configuration.setProjectShortNameMap(properties.getProperty("ProjectShortNames"));
        File repo = configuration.getCopiedGitRepositories().get(0);
        configuration.setProjectRepository(repo);

        //instantiate project data
        ProjectData projectData = new ProjectData(configuration);

        D(projectData);
        GM(projectData);
        GDT(projectData);
        return EDB(projectData);
    }


    public Map<String, List<String>> invokeFeatRacer(String projectPath, int startingCommitIndex, String analysisPath, String allowedFileExtensions, String commitHash) throws Exception {
        //Read properties file
        Properties properties = new Properties();
        InputStream inputStream = new FileInputStream("config.properties");
        properties.load(inputStream);
        // Set properties values
        properties.setProperty("ProjectRepository", projectPath);
        properties.setProperty("AnalysisDirectory",  analysisPath);
        properties.setProperty("AllowedFileExtensions", allowedFileExtensions);
        properties.setProperty("StartingCommitIndex", String.valueOf(startingCommitIndex));

        //create analysis folder
        Path analysisFolder = Utilities.createOutputDirectory(properties.getProperty("AnalysisDirectory"));
        File analysisDirectory = analysisFolder.toFile();

        //set configuration
        Configuration configuration = Utilities.getConfiguration(properties, analysisDirectory);

        //instantiate project data
        ProjectData projectData = new ProjectData(configuration);

        Dsingle(projectData, commitHash);
        GMsingle(projectData, commitHash);
        GDT(projectData);
        return EDB(projectData);
    }

    public void updateDataset() {
        //TODO: Whats the input parameter(s), Whats the option?
        throw new UnsupportedOperationException("Not supported yet.");
    }

    // Generate Data
    private void D(ProjectData projectData) {
        RepoDriller repoDriller = new RepoDriller();
        repoDriller.start(new ProjectReaderWithDriller(projectData));
    }

    //Generate Metrics
    private void GM(ProjectData projectData) throws SQLException, ClassNotFoundException {
        MetricCalculatorDB metricCalculatordb = new MetricCalculatorDB(projectData);
        metricCalculatordb.calculateMetricsALLASSETSLOADED();
    }

    // Generate ARFF dataset
    private void GDT(ProjectData projectData) throws Exception {
        DataGeneratorDB dataGeneratorDB = new DataGeneratorDB(projectData);
        dataGeneratorDB.createDataSets();
    }

    // Run classifiers on those ARFF datasets
    private Map<String, List<String>> EDB(ProjectData projectData) throws Exception {
        RecommendationService recommendationService = new RecommendationService(projectData);
        return recommendationService.runClassifier();
    }

    // This theoretically should work with single commits now
    private void Dsingle(ProjectData projectData, String commitHash) throws SQLException, IOException, ClassNotFoundException {
        RepoDriller repoDriller = new RepoDriller();
        repoDriller.start(new CommitReaderWithDriller(projectData, commitHash));
    }

    private void GMsingle(ProjectData projectData, String commitHash) throws SQLException, IOException, ClassNotFoundException {
        MetricCalculatorDB metricCalculatordb = new MetricCalculatorDB(projectData);
        metricCalculatordb.calculateMetricsSingle(commitHash);
    }

}
