package se.gu.api;

import org.apache.lucene.queryparser.classic.ParseException;
import org.repodriller.RepoDriller;
import se.gu.main.Configuration;
import se.gu.main.ProjectData;
import se.gu.main.ProjectReader;
import se.gu.main.ProjectReaderWithDriller;
import se.gu.metrics.MetricCalculatorDB;
import se.gu.ml.experiment.ExperimentRunnerDB;
import se.gu.ml.preprocessing.DataGeneratorDB;
import se.gu.utils.Utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Properties;

public class FeatRacerAPI {

    /**
     * You need to give featracer the path to your project
     *
     * @param projectPath Path to the project e.g., C:\\users\\gto\\repos\\MyApplication
     *                    THis method will create an analysis folder called FeatRacerAnalysis in your user home directory
     */
    public void initializeProject(String projectPath, int startingCommitIndex) throws IOException, SQLException, ClassNotFoundException, ParseException {
        //create analysis folder
        Path analysisFolder = Utilities.createOutputDirectory("FeatRacerAnalysis");
        File analysisDirectory = analysisFolder.toFile();
        //Read properties file
        Properties properties = new Properties();
        InputStream inputStream = new FileInputStream("config.properties");
        properties.load(inputStream);

        //set configuration
        Configuration configuration = Utilities.getConfiguration(properties, analysisDirectory);

        //instantiate project data
        ProjectData projectData = new ProjectData(configuration);

        //now generate assets at the current state of the project
        ProjectReader projectReader = new ProjectReader(projectData);
        projectReader.createAnnotationsWithoutCommits();
        // TODO: This is option AWC but during testing we used option D, change to D -> GM -> GDT
    }


    public void invokeFeatRacer(String commitHash) {
        //TODO: What is the output format /// Which of the options is this?
    }

    public void updateDataset() {
        //TODO: Whats the input parameter(s), Whats the option?
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
    private void EDB(ProjectData projectData) throws SQLException, IOException, ClassNotFoundException {
        ExperimentRunnerDB experimentRunnerDB = new ExperimentRunnerDB(projectData);
        experimentRunnerDB.runExperiment();
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


    private void EDBsingle(ProjectData projectData) throws SQLException, IOException, ClassNotFoundException {

    }
}
