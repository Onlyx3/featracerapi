package se.gu.ml.preprocessing;

import mulan.data.InvalidDataFormatException;
import mulan.data.MultiLabelInstances;
import se.gu.main.ProjectData;
import se.gu.ml.experiment.FeatureSelector;
import se.gu.utils.Utilities;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DatasetAnalyzer  implements Serializable {
    private static final long serialVersionUID = -1474796050146587302L;
    private ProjectData projectData;
    private File dataFilesFolder;

    public DatasetAnalyzer(ProjectData projectData, String outputFile) {
        this.projectData = projectData;
        dataFilesFolder = new File(projectData.getConfiguration().getDataFilesSubDirectory());

        this.outputFile = outputFile;
    }

    private String outputFile;


    public void createDatasetAnalytics() throws InvalidDataFormatException {
        List<DatasetStatistic> datasetStatistics = new ArrayList<>();
        System.out.println("GENERATING Dataset statistics...");
        doAnalyseDataSets(dataFilesFolder, datasetStatistics);
        //print
        //print header
        System.out.println("SAVING Dataset statistics...");
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(outputFile, true));
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
            writer.print("Scumble;");
            writer.println();
            //now print records
            for (DatasetStatistic statistic : datasetStatistics) {
                writer.print(statistic.getDatasetName().split("_")[0] + ";");
                writer.print(statistic.getDatasetName() + ";");
                writer.print(statistic.getNumOfInstances() + ";");
                writer.print(statistic.getNumOfLabels() + ";");
                writer.print(statistic.getCardinality() + ";");
                writer.print(statistic.getMeanIR() + ";");
                writer.print(statistic.getCVIR() + ";");
                writer.print(statistic.getMaxIRLb() + ";");
                writer.print(statistic.getMinIRLb() + ";");
                writer.print(statistic.getMaxImR() + ";");
                writer.print(statistic.getMinImR() + ";");
                writer.print(statistic.getScumble() + ";");
                writer.println();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
        System.out.println("Dataset statistics DONE!");
    }

    private void doAnalyseDataSets(File folder, List<DatasetStatistic> datasetStatistics) throws InvalidDataFormatException {
        DatasetStatistic dataSetStatistic = null;
        if (!folder.getAbsolutePath().equalsIgnoreCase(dataFilesFolder.getAbsolutePath())) {
            dataSetStatistic = new DatasetStatistic();
        }
        boolean isUseDataBalancer = projectData.getConfiguration().isUseDataBalancer();
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                doAnalyseDataSets(file, datasetStatistics);
            } else {
                if (dataSetStatistic == null) {
                    continue;
                }

                if (Utilities.isXMLFile(file) && file.getName().contains("FLAT")) {
                    dataSetStatistic.setXmlFile(file);

                } else if (Utilities.isARFFFile(file) && !file.getName().contains("PREDICT")) {
                    dataSetStatistic.setArrfFile(file);
                }

            }
        }
        //now create
        if (dataSetStatistic != null) {
            MultiLabelInstances mlData = new MultiLabelInstances(dataSetStatistic.getArrfFile().getAbsolutePath(), dataSetStatistic.getXmlFile().getAbsolutePath());
            //get reduced Dataset
            //FeatureSelector.
            ImbalancedStatistics statistics = new ImbalancedStatistics();
            statistics.calculateImSta(mlData);
            dataSetStatistic.setDatasetName(dataSetStatistic.getArrfFile().getName());
            dataSetStatistic.setNumOfInstances(mlData.getNumInstances());
            dataSetStatistic.setNumOfLabels(mlData.getNumLabels());
            dataSetStatistic.setCardinality(mlData.getCardinality());
            dataSetStatistic.setMeanIR(statistics.getMeanIR());
            dataSetStatistic.setCVIR(statistics.getCVIR());
            dataSetStatistic.setMaxIRLb(statistics.getMaxIRLb());
            dataSetStatistic.setMinIRLb(statistics.getMinIRLb());
            dataSetStatistic.setMaxImR(statistics.getMaxImR());
            dataSetStatistic.setMinImR(statistics.getMinImR());
            dataSetStatistic.setScumble(statistics.getSCUMBLE());

            datasetStatistics.add(dataSetStatistic);


        }
    }
}
