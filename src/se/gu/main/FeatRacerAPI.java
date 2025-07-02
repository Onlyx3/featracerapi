package se.gu.main;

import org.apache.lucene.queryparser.classic.ParseException;
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
    public void InitializeProject(String projectPath) throws IOException, SQLException, ClassNotFoundException, ParseException {
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

    }
}
