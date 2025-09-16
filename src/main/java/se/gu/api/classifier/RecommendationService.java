package se.gu.api.classifier;

import mulan.classifier.MultiLabelOutput;
import mulan.classifier.meta.RAkELd;
import mulan.classifier.transformation.LabelPowerset;
import mulan.data.InvalidDataFormatException;
import mulan.data.MultiLabelInstances;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import se.gu.assets.DataSetRecord;
import se.gu.data.DataController;
import se.gu.git.Commit;
import se.gu.main.Configuration;
import se.gu.main.ProjectData;
import se.gu.ml.experiment.ClassifierRecord;
import weka.classifiers.trees.J48;
import weka.core.Instance;
import weka.core.Instances;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class RecommendationService {

    private ProjectData projectData;

    public RecommendationService(ProjectData projectData) {
        this.projectData = projectData;
    }


    public Map<String, List<String>> runClassifier() throws Exception {
        Map<String, List<String>> allRecommendations = new HashMap<>();

         File analysisDirectory = projectData.getConfiguration().getAnalysisDirectory();
         Configuration config = projectData.getConfiguration();
         DataController dataController = new DataController(config);
         String projectName = config.getProjectRepository().getName();

        List<DataSetRecord> dataSetRecords = dataController.getAllDataSetsForProject(projectName);
        String[] assetTypes = config.getAssetTypes();
        List<String> assetTypesToPredict = config.getAssetTypesToPredict();


        List<Commit> fullCommitList = dataController.getAllCommits(projectName);
        if(fullCommitList.isEmpty()){
            System.err.println("No commits found for project");
            return new HashMap<>();
        }

        for(int i = fullCommitList.size() - 1; i >= 0; i--){
            Commit relevantCommit = fullCommitList.get(i);

            for(String assetType : assetTypes){
                if(!assetTypesToPredict.contains(assetType)) continue;



                //get datasetrecord for specific type of type (folder,file, loc)
                Optional<DataSetRecord> recordOptional = dataSetRecords.parallelStream()
                        .filter(d -> d.getAssetType().equalsIgnoreCase(assetType) &&
                                d.getCommitHash().equals(relevantCommit.getCommitHash()))
                        .findFirst();
                if(recordOptional.isEmpty()) continue;
                DataSetRecord dataSetRecord = recordOptional.get();

                //go through each record and run predictions
                //for(DataSetRecord dataSetRecord : typeDataSetRecords){
                    //if(StringUtils.isBlank(dataSetRecord.getTestFile())) continue;

                    MultiLabelInstances trainingDataSet = null;
                    try{
                        trainingDataSet = new MultiLabelInstances(dataSetRecord.getTrainingFile(), dataSetRecord.getTrainingXMLFile());
                    } catch (InvalidDataFormatException e) {
                        e.printStackTrace();
                        continue;
                    }

                    Instances unlabeledData = getUnlabeledData(dataSetRecord.getTestFile());
                    int numInstances = unlabeledData.numInstances();
                    File assetMappingsFile = new File(dataSetRecord.getTestCSVFile());
                    if(!assetMappingsFile.exists()) continue;

                    List<String> assetMappings = FileUtils.readLines(assetMappingsFile, config.getTextEncoding());
                    List<String> instanceNames = assetMappings.parallelStream().map(m -> m.split(";")[0]).collect(Collectors.toList());

                    RAkELd learner = new  RAkELd(new LabelPowerset(new J48()));

                    ClassifierRecord classifierRecord = new ClassifierRecord();
                    classifierRecord.setClassifierName("RAkELd");
                    classifierRecord.setTrainingSet(trainingDataSet);

                    learner.build(classifierRecord.getTrainingSet());
                    for(int j = 0; j < numInstances; j++) {
                        Instance instance = unlabeledData.instance(i);
                        String instanceName = instanceNames.get(i);
                        MultiLabelOutput output = learner.makePrediction(instance);
                        List<String> predictedFeatures = getRetrievedFeatures(List.of(trainingDataSet.getLabelNames()), output.getBipartition());

                        if(!predictedFeatures.isEmpty()) allRecommendations.put(instanceName, predictedFeatures);
                    }

            }
            if(allRecommendations.isEmpty()){ continue;}
            return allRecommendations;
        }
        return new HashMap<>();
    }

    private List<String> getRetrievedFeatures(List<String> features, boolean[] bipartition) {
        List<String> retrievedFeatures = new ArrayList<>();
        if(bipartition != null) {
            for (int i = 0; i < bipartition.length; i++) {
                if(bipartition[i]) retrievedFeatures.add(features.get(i));
            }
        }
        return retrievedFeatures;
    }


    private Instances getUnlabeledData(String testFile) throws IOException {
        Instances unlabeledData = null;
        if (testFile != null) {
            FileReader reader = new FileReader(testFile);
            unlabeledData = new Instances(reader);
        }
        return unlabeledData;
    }
}
