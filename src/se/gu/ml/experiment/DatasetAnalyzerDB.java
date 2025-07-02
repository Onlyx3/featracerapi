package se.gu.ml.experiment;

import mulan.data.InvalidDataFormatException;
import mulan.data.MultiLabelInstances;
import se.gu.assets.DataSetRecord;
import se.gu.main.ProjectData;
import se.gu.ml.preprocessing.DatasetStatistic;
import se.gu.ml.preprocessing.ImbalancedStatistics;
import se.gu.utils.Utilities;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DatasetAnalyzerDB implements Serializable {
    private static final long serialVersionUID = -1474796050146587302L;
    private ProjectData projectData;
    List<DataSetRecord> typeDataSetRecords;

    public DatasetAnalyzerDB(List<DataSetRecord> typeDataSetRecords) {
        this.typeDataSetRecords = typeDataSetRecords;
    }

    private String outputFile;


    public List<DatasetStatistic> createDatasetAnalytics() throws InvalidDataFormatException {
        List<DatasetStatistic> datasetStatistics = new ArrayList<>();
        System.out.println("GENERATING Dataset statistics...");
        for (DataSetRecord record : typeDataSetRecords) {
            try {
                datasetStatistics.add(doAnalyseDataSets(record));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return datasetStatistics;

    }

    private DatasetStatistic doAnalyseDataSets(DataSetRecord dsr) throws InvalidDataFormatException {
        DatasetStatistic dataSetStatistic = new DatasetStatistic();
        dataSetStatistic.setCommitHash(dsr.getCommitHash());
        dataSetStatistic.setCommitIndex(dsr.getCommitIdex());
        dataSetStatistic.setProject(dsr.getProject());
        dataSetStatistic.setLevel(dsr.getAssetType());
        dataSetStatistic.setArrfFile(new File(dsr.getTrainingFile()));
        dataSetStatistic.setXmlFile(new File(dsr.getTrainingXMLFile()));

        //now create

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
        return dataSetStatistic;


    }
}
