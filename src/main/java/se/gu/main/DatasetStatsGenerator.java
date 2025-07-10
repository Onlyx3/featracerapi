package se.gu.main;

import mulan.data.InvalidDataFormatException;
import org.apache.commons.io.FileUtils;
import se.gu.assets.DataSetRecord;
import se.gu.data.DataController;
import se.gu.ml.experiment.DatasetAnalyzerDB;
import se.gu.ml.preprocessing.DatasetStatistic;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class DatasetStatsGenerator {
    public DatasetStatsGenerator(ProjectData projectData) throws IOException {
        this.projectData = projectData;

    }

    private ProjectData projectData;

    public void generateDatasetStats() throws InvalidDataFormatException, IOException, SQLException, ClassNotFoundException {
        File analysisDirectory = projectData.getConfiguration().getAnalysisDirectory();

        DataController dataController = new DataController(projectData.getConfiguration());


        //get all projects
        String[]projectNameList = projectData.getConfiguration().getProjectNamesList();
        String[] projectShortNames = projectData.getConfiguration().getProjectShortNameList();
        int nFeatures = (int) projectData.getConfiguration().getTopNFeatures();

        File datasetStatFile = new File(String.format("%s/datasetSTATS.csv",projectData.getConfiguration().getrDataFolder()));
        if(datasetStatFile.exists()){
            FileUtils.forceDelete(datasetStatFile);
        }

        //create header
        PrintWriter writer = new PrintWriter(new FileWriter(datasetStatFile,true));
        writer.print("project;");
        writer.print("level;");
        writer.print("commitIndex;");
        writer.print("commit;");
        writer.print("Dataset;");
        writer.print("NumOfInstances;");
        writer.print("NumOfLabels;");
        writer.print("Cardinality;");
        writer.print("MeanIR;");
        writer.print("CVIR;");
        writer.print("MaxIRLb;");
        writer.print("MinIRLb;");
        writer.print("MaxImR;");
        writer.print("MinImR;");
        writer.print("Scumble");
        writer.println();
        writer.close();
        //asset types
        String[] assetTypes = projectData.getConfiguration().getAssetTypes();

        for(int i=0; i<projectNameList.length;i++){
            String projectName = projectNameList[i];
            String projectShortName = projectShortNames[i];
            //==========create output folders==========
            List<DataSetRecord> dataSetRecords = dataController.getAllDataSetsForProject(projectName);
            for(String assetType:assetTypes){
                System.out.println(projectShortName+" "+assetType);
                List<DataSetRecord> typeDataSetRecords = dataSetRecords.parallelStream().filter(d -> d.getAssetType().equalsIgnoreCase(assetType)).collect(Collectors.toList());
                PrintWriter pw = null;
                try {
                    pw = new PrintWriter(new FileWriter(datasetStatFile,true));
                    //for each project and assettype, read all training data files and analyse them

                    DatasetAnalyzerDB datasetAnalyzer = new DatasetAnalyzerDB(typeDataSetRecords);
                    List<DatasetStatistic> stats = datasetAnalyzer.createDatasetAnalytics();
                    //now print records
                    for (DatasetStatistic statistic : stats) {
                        pw.print(statistic.getProject()+";");
                        pw.print(statistic.getLevel()+";");
                        pw.print(statistic.getCommitIndex()+";");
                        pw.print(statistic.getCommitHash() + ";");
                        pw.print(statistic.getDatasetName() + ";");
                        pw.print(statistic.getNumOfInstances() + ";");
                        pw.print(statistic.getNumOfLabels() + ";");
                        pw.print(statistic.getCardinality() + ";");
                        pw.print(statistic.getMeanIR() + ";");
                        pw.print(statistic.getCVIR() + ";");
                        pw.print(statistic.getMaxIRLb() + ";");
                        pw.print(statistic.getMinIRLb() + ";");
                        pw.print(statistic.getMaxImR() + ";");
                        pw.print(statistic.getMinImR() + ";");
                        pw.print(statistic.getScumble() );
                        pw.println();
                    }
                }catch (Exception ex){
                    ex.printStackTrace();
                }finally {
                    if(pw!=null) {
                        pw.close();
                    }
                }
            }

        }


    }
}
