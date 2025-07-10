package se.gu.main;

import org.apache.commons.io.FileUtils;
import se.gu.data.DataController;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class ReportGenerator {
    private DataController dataController;
    private Configuration configuration;

    public ReportGenerator(Configuration configuration) throws SQLException, ClassNotFoundException {
        this.configuration = configuration;
        this.dataController = new DataController(configuration);
    }

    public void generateAssetCountPerCommitPerProject() throws IOException, SQLException {
        File resultsFolder = configuration.getrDataFolder();
        File outputFile = new File(String.format("%s/assetCountAllProjects.csv",resultsFolder.getAbsolutePath()));
        if(outputFile.exists()){
            FileUtils.forceDelete(outputFile);
        }
        //header
        PrintWriter writer = new PrintWriter(new FileWriter(outputFile,true));
        writer.println("commitIndex;project;assetCount");
        Map<String,String> mappedProjectNames = configuration.getMappedProjectNames();


        //results
        ResultSet records = dataController.getAssetCountPerCOmmitAllProjects();
        while(records.next()){
            String longName = records.getString("project");
            String shortName = mappedProjectNames.get(longName);
            writer.printf("%d;%s;%d\n",records.getInt("commitIndex"),shortName==null?longName:shortName  ,records.getInt("assets"));
            System.out.println(longName);
        }
        records.close();
    }
}
