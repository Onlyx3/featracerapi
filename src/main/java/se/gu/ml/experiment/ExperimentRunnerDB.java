package se.gu.ml.experiment;

import com.google.common.base.Stopwatch;
import se.gu.ml.alg.*;
import me.tongfei.progressbar.ProgressBar;
import mulan.classifier.MultiLabelLearnerBase;
import mulan.classifier.MultiLabelOutput;
import mulan.classifier.lazy.IBLR_ML;
import mulan.classifier.lazy.MLkNN;
import mulan.classifier.meta.RAkEL;
import mulan.classifier.meta.RAkELd;
import mulan.classifier.neural.BPMLL;
import mulan.classifier.transformation.*;
import mulan.data.MultiLabelInstances;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import se.gu.assets.DataSetRecord;
import se.gu.data.DataController;
import se.gu.main.Configuration;
import se.gu.main.ProjectData;
import se.gu.ml.alg.*;
import se.gu.utils.Utilities;
import weka.classifiers.trees.J48;
import weka.core.Instance;
import weka.core.Instances;

import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ExperimentRunnerDB {
    private ProjectData projectData;
    private String outputDirectory;
    private boolean printPredictionSummaryHeader;

    public ExperimentRunnerDB(ProjectData projectData) {
        this.projectData = projectData;
        printPredictionSummaryHeader = true;

    }

    public void runExperimentForTopNFeatures() throws SQLException, ClassNotFoundException, IOException {
        File analysisDirectory = projectData.getConfiguration().getAnalysisDirectory();
        Configuration config = projectData.getConfiguration();
        DataController dataController = new DataController(config);
        String projectName = config.getProjectRepository().getName();
        boolean trainOnAll = config.isTrainingDataIncludesUnMappedAssets();
        int topNFeatures = (int) config.getTopNFeatures();
        List<String> features = projectData.getMLFeaturesNames();
        //get datasets first
        List<DataSetRecord> dataSetRecords = dataController.getAllDataSetsForProject(projectName);
        String[] assetTypes = config.getAssetTypes();
        List<String> assetTypesToPredict = config.getAssetTypesToPredict();
        for (String assetType : assetTypes) {
            if (!assetTypesToPredict.contains(assetType)) {
                continue;
            }
            System.out.printf("===========%s=============\n", assetType);
            //set output directory
            outputDirectory = String.format("%s/%s/%s/%s/%s/%dfeatures/%s", config.getAnalysisDirectory(), "ExperimentData", config.getProjectRepository().getName(),
                    assetType,
                    new File(config.getDataFilesSubDirectory()).getName(),topNFeatures , trainOnAll ? "trainingOnAllAssets" : "trainingOnMappedAssets");
            Utilities.createOutputDirectory(outputDirectory, true);
            //========set output summary file
            String predictionResultsSummaryOutputFile = String.format("%s/%s_%s_ps_im_reg_diff_%d_%s.csv", outputDirectory, projectName, assetType.toLowerCase(),topNFeatures, trainOnAll ? "ALLAssets" : "MappedOnly");// + FilenameUtils.removeExtension(FilenameUtils.getName(datasetFile)) + ".csv";
            PrintWriter predictionResultsPrintWriter = new PrintWriter(new FileWriter(predictionResultsSummaryOutputFile, true));
            String predictionMeasuresResultsSummaryOutputFile = String.format("%s/%s_%s_combinedmeasures_ps_im_reg_diff_%d_%s.csv", outputDirectory, projectName, assetType.toLowerCase(),topNFeatures, trainOnAll ? "ALLAssets" : "MappedOnly");// + FilenameUtils.removeExtension(FilenameUtils.getName(datasetFile)) + ".csv";
            PrintWriter predictionMeasuresResultsPrintWriter = new PrintWriter(new FileWriter(predictionMeasuresResultsSummaryOutputFile, true));
            printPredictionMeasuresSummaryHeader(predictionMeasuresResultsPrintWriter);
            //print header
            printPredictionSummaryHeader(predictionResultsPrintWriter);
            List<PredictionResultSummary> predictionResultSummaries = new ArrayList<>();
            //==========set instanceSummryOutput
            String predictionResultsInstanceSummaryOutputFile = String.format("%s/%s_%s_instance_ps_im_reg_diff_%d_%s.csv", outputDirectory, projectName, assetType.toLowerCase(),topNFeatures, trainOnAll ? "ALLAssets" : "MappedOnly"); // + FilenameUtils.removeExtension(FilenameUtils.getName(datasetFile)) + ".csv";
            PrintWriter predictionResultsInstancePrintWriter = new PrintWriter(new FileWriter(predictionResultsInstanceSummaryOutputFile, true));
            printResultsInstanceOutputHeader(predictionResultsInstancePrintWriter);
            String predictionResultsInstanceMeasureSummaryOutputFile = String.format("%s/%s_%s_instanceMeasure_ps_im_reg_diff_%d_%s.csv", outputDirectory, projectName, assetType.toLowerCase(),topNFeatures, trainOnAll ? "ALLAssets" : "MappedOnly"); // + FilenameUtils.removeExtension(FilenameUtils.getName(datasetFile)) + ".csv";
            PrintWriter predictionResultsInstanceMeasurePrintWriter = new PrintWriter(new FileWriter(predictionResultsInstanceMeasureSummaryOutputFile, true));
            printResultsInstanceMeasureOutputHeader(predictionResultsInstanceMeasurePrintWriter);

            //get dataset records for specific type e.gf., FOLDER, FILE or LOC
            List<DataSetRecord> typeDataSetRecords = dataSetRecords.parallelStream().filter(d -> d.getAssetType().equalsIgnoreCase(assetType)).collect(Collectors.toList());
            String projectFeatureSelectionSummaryFile = String.format("%s/FeatureSelection/%s_%s_SUMMARY.csv", analysisDirectory, projectName, assetType);
            File featureSelctionSummaryFile = new File(projectFeatureSelectionSummaryFile);
            //Get ranking of features based on selected criteria
            int[] featureIndices = null;
            if(featureSelctionSummaryFile.exists()) {
                featureIndices = getFeatureIndices(projectFeatureSelectionSummaryFile);
            }
            //go through each dataset record and run predictions
            try (ProgressBar cb = new ProgressBar("Running PREDICTIONS:", typeDataSetRecords.size())) {
                for (DataSetRecord dataSetRecord : typeDataSetRecords) {
                    //skip if no testFile exists; it's possible that a testComnit ay exist but maybe no files of interest were changed in that commit. e.e., maybe only readme or ignre file was changed
                    cb.setExtraMessage(dataSetRecord.getCommitHash());
                    cb.step();
                    if (StringUtils.isBlank(dataSetRecord.getTestFile())) {
                        continue;
                    }
//                    if (dataSetRecord.getCommitIdex()!=99) {
//                        continue;
//                    }
//                    if(dataSetRecord.getCommitIdex()==99&&assetType.equalsIgnoreCase("LOC")){
//                        System.out.println("HERE");
//                    }
                    System.out.println("SELCTED FEATURES");
                    MultiLabelInstances trainingDataSet = null;
                    try {
                        trainingDataSet = new MultiLabelInstances(dataSetRecord.getTrainingFile(), dataSetRecord.getTrainingXMLFile());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        continue;
                    }
//reduce dataset
                    if (topNFeatures < features.size()&&featureSelctionSummaryFile.exists()&&featureIndices!=null) {
                        trainingDataSet =  FeatureSelector.buildReducedMultiLabelDataset(featureIndices, trainingDataSet);
                    }
                    trainingDataSet.getFeatureAttributes().stream().forEach(a->{System.out.printf("%s ",a.name());});
                    System.out.println();
                    //
                    Instances unlabledData = getUnlabledData(dataSetRecord.getTestFile());
                    //reduced features dataset
                    if (topNFeatures < features.size()&&featureSelctionSummaryFile.exists()&&featureIndices!=null) {
                        MultiLabelInstances testDataset =  new MultiLabelInstances(unlabledData, dataSetRecord.getTestXMLFile());
                        testDataset =  FeatureSelector.buildReducedMultiLabelDataset(featureIndices, testDataset);
                        unlabledData = testDataset.getDataSet();
                    }



                    Map<String, ClassifierRecord> classifiers = createClassifiersList(ModelGroup.PREDICTION,  trainingDataSet);
                    int numInstances = unlabledData.numInstances();
                    File assetMappingsFile = new File(dataSetRecord.getTestCSVFile());
                    if (!assetMappingsFile.exists()) {//if no mappinds exist, skip
                        continue;
                    }




                    List<String> assetMappings = FileUtils.readLines(assetMappingsFile, config.getTextEncoding());
                    PrintWriter predictionsPrintWriter = null;
                    String predictionsOutputFile = String.format("%s/%d_%s_%s_ps_im_reg_diff_11.csv", outputDirectory, dataSetRecord.getCommitIdex(), dataSetRecord.getCommitHash(), assetType.toLowerCase());
                    try (ProgressBar pb = new ProgressBar("Classifier:", classifiers.size())) {
                        try {

                            if (config.isPrintDetailedPredictionResults()) {
                                predictionsPrintWriter = new PrintWriter(new FileWriter(predictionsOutputFile, true));
                                printMultiLabelOutputHeader(predictionsPrintWriter, trainingDataSet);
                            }

                            List<String> instanceNames = assetMappings.parallelStream().map(m -> m.split(";")[0]).collect(Collectors.toList());

                            for (String classifierName : classifiers.keySet()) {
                                pb.step();
                                pb.setExtraMessage(classifierName);
                                //System.out.println(classifierName);

                                ClassifierRecord classifierRecord = classifiers.get(classifierName);
                                List<MultiLabelLearnerBase> leaners = classifierRecord.getClassifierList();

                                int learnerIndex = 0;
                                int lastLearnerIndex = leaners.size() - 1;
                                long trainTime = 0, testTime = 0;
                                for (MultiLabelLearnerBase learner : leaners) {
                                    try {
                                        Stopwatch stopwatch = Stopwatch.createStarted();
                                        learner.build(classifierRecord.getTrainingSet());
                                        stopwatch.stop();
                                        trainTime += stopwatch.elapsed(TimeUnit.SECONDS);
                                        //now predict
                                        long testMilliseconds = 0;
                                        for (int i = 0; i < numInstances; i++) {
                                            Stopwatch testWatch = Stopwatch.createStarted();
                                            Instance instance = unlabledData.instance(i);
                                            MultiLabelOutput output = learner.makePrediction(instance);
                                            testWatch.stop();
                                            testMilliseconds += testWatch.elapsed(TimeUnit.MILLISECONDS);

                                            if (learnerIndex == lastLearnerIndex) {//only print for the last learner

                                                testTime = TimeUnit.MILLISECONDS.toSeconds(testMilliseconds);
                                                printMultiLabelOutput(classifierRecord, output, instanceNames.get(i), assetMappings, predictionsPrintWriter, dataSetRecord, predictionResultSummaries, predictionResultsInstancePrintWriter, predictionResultsInstanceMeasurePrintWriter,trainTime,testTime);

                                            }
                                        }
                                        learnerIndex++;
                                    } catch (Exception e1) {
                                        e1.printStackTrace();
                                    }
                                }


                            }
                            //at the end of all classifiers, print results summary
                            printPredictionResultSummary(predictionResultsPrintWriter, dataSetRecord, predictionResultSummaries, predictionMeasuresResultsPrintWriter);


                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            if (predictionsPrintWriter != null) {
                                predictionsPrintWriter.close();
                            }

                        }
                    }


                }//close printer here
            } catch (Exception ex) {

            } finally {
                if (predictionResultsPrintWriter != null) {
                    predictionResultsPrintWriter.close();
                }
                if (predictionResultsInstancePrintWriter != null) {
                    predictionResultsInstancePrintWriter.close();
                }
                if (predictionResultsInstanceMeasurePrintWriter != null) {
                    predictionResultsInstanceMeasurePrintWriter.close();
                }
                if (predictionMeasuresResultsPrintWriter != null) {
                    predictionMeasuresResultsPrintWriter.close();
                }
                //copy sumary file
                if (projectData.getConfiguration().isCopyExpResultsToRFolder()) {

                    File psFile = new File(predictionResultsSummaryOutputFile);
                    if (psFile.exists()) {
                        FileUtils.copyFileToDirectory(psFile, projectData.getConfiguration().getrDataFolder());//copy output to the Rfolder
                    }
                    File psInstanceFile = new File(predictionResultsInstanceSummaryOutputFile);
                    if (psInstanceFile.exists()) {
                        FileUtils.copyFileToDirectory(psInstanceFile, projectData.getConfiguration().getrDataFolder());//copy output to the Rfolder
                    }
//                    File psInstanceMeasureFile = new File(predictionResultsInstanceMeasureSummaryOutputFile);
//                    if (psInstanceMeasureFile.exists()) {
//                        FileUtils.copyToDirectory(psInstanceMeasureFile, projectData.getConfiguration().getrDataFolder());//copy output to the Rfolder
//                    }
                    File psMeasuresSummaryFile = new File(predictionMeasuresResultsSummaryOutputFile);
                    if (psMeasuresSummaryFile.exists()) {
                        FileUtils.copyFileToDirectory(psMeasuresSummaryFile, projectData.getConfiguration().getrDataFolder());//copy output to the Rfolder
                    }

                }
            }
        }

    }

    public void runExperiment() throws SQLException, ClassNotFoundException, IOException {
        Configuration config = projectData.getConfiguration();
        DataController dataController = new DataController(config);
        String projectName = config.getProjectRepository().getName();
        boolean trainOnAll = config.isTrainingDataIncludesUnMappedAssets();
        //get datasets first
        List<DataSetRecord> dataSetRecords = dataController.getAllDataSetsForProject(projectName);
        String[] assetTypes = config.getAssetTypes();
        List<String> assetTypesToPredict = config.getAssetTypesToPredict();
        for (String assetType : assetTypes) {
            if (!assetTypesToPredict.contains(assetType)) {
                continue;
            }
            System.out.printf("===========%s=============\n", assetType);
            //set output directory
            outputDirectory = String.format("%s/%s/%s/%s/%s/%dfeatures/%s", config.getAnalysisDirectory(), "ExperimentData", config.getProjectRepository().getName(),
                    assetType,
                    new File(config.getDataFilesSubDirectory()).getName(), (int) config.getTopNFeatures(), trainOnAll ? "trainingOnAllAssets" : "trainingOnMappedAssets");
            Utilities.createOutputDirectory(outputDirectory, true);
            //========set output summary file
            String predictionResultsSummaryOutputFile = String.format("%s/%s_%s_ps_im_reg_diff_11_%s.csv", outputDirectory, projectName, assetType.toLowerCase(), trainOnAll ? "ALLAssets" : "MappedOnly");// + FilenameUtils.removeExtension(FilenameUtils.getName(datasetFile)) + ".csv";
            PrintWriter predictionResultsPrintWriter = new PrintWriter(new FileWriter(predictionResultsSummaryOutputFile, true));
            String predictionMeasuresResultsSummaryOutputFile = String.format("%s/%s_%s_combinedmeasures_ps_im_reg_diff_11_%s.csv", outputDirectory, projectName, assetType.toLowerCase(), trainOnAll ? "ALLAssets" : "MappedOnly");// + FilenameUtils.removeExtension(FilenameUtils.getName(datasetFile)) + ".csv";
            PrintWriter predictionMeasuresResultsPrintWriter = new PrintWriter(new FileWriter(predictionMeasuresResultsSummaryOutputFile, true));
            printPredictionMeasuresSummaryHeader(predictionMeasuresResultsPrintWriter);
            //print header
            printPredictionSummaryHeader(predictionResultsPrintWriter);
            List<PredictionResultSummary> predictionResultSummaries = new ArrayList<>();
            //==========set instanceSummryOutput
            String predictionResultsInstanceSummaryOutputFile = String.format("%s/%s_%s_instance_ps_im_reg_diff_11_%s.csv", outputDirectory, projectName, assetType.toLowerCase(), trainOnAll ? "ALLAssets" : "MappedOnly"); // + FilenameUtils.removeExtension(FilenameUtils.getName(datasetFile)) + ".csv";
            PrintWriter predictionResultsInstancePrintWriter = new PrintWriter(new FileWriter(predictionResultsInstanceSummaryOutputFile, true));
            printResultsInstanceOutputHeader(predictionResultsInstancePrintWriter);
            String predictionResultsInstanceMeasureSummaryOutputFile = String.format("%s/%s_%s_instanceMeasure_ps_im_reg_diff_11_%s.csv", outputDirectory, projectName, assetType.toLowerCase(), trainOnAll ? "ALLAssets" : "MappedOnly"); // + FilenameUtils.removeExtension(FilenameUtils.getName(datasetFile)) + ".csv";
            PrintWriter predictionResultsInstanceMeasurePrintWriter = new PrintWriter(new FileWriter(predictionResultsInstanceMeasureSummaryOutputFile, true));
            printResultsInstanceMeasureOutputHeader(predictionResultsInstanceMeasurePrintWriter);

            //get dataset records for specific type e.gf., FOLDER, FILE or LOC
            List<DataSetRecord> typeDataSetRecords = dataSetRecords.parallelStream().filter(d -> d.getAssetType().equalsIgnoreCase(assetType)).collect(Collectors.toList());
            //go through each dataset record and run predictions
            try (ProgressBar cb = new ProgressBar("Running PREDICTIONS:", typeDataSetRecords.size())) {
                for (DataSetRecord dataSetRecord : typeDataSetRecords) {
                    //skip if no testFile exists; it's possible that a testComnit ay exist but maybe no files of interest were changed in that commit. e.e., maybe only readme or ignre file was changed
                    cb.setExtraMessage(dataSetRecord.getCommitHash());
                    cb.step();
                    if (StringUtils.isBlank(dataSetRecord.getTestFile())) {
                        continue;
                    }
//                    if (dataSetRecord.getCommitIdex()!=99) {
//                        continue;
//                    }
//                    if(dataSetRecord.getCommitIdex()==99&&assetType.equalsIgnoreCase("LOC")){
//                        System.out.println("HERE");
//                    }

                    MultiLabelInstances trainingDataSet = null;
                    try {
                        trainingDataSet = new MultiLabelInstances(dataSetRecord.getTrainingFile(), dataSetRecord.getTrainingXMLFile());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        continue;
                    }

                    Instances unlabledData = getUnlabledData(dataSetRecord.getTestFile());
                    Map<String, ClassifierRecord> classifiers = createClassifiersList(ModelGroup.PREDICTION, trainingDataSet);
                    int numInstances = unlabledData.numInstances();
                    File assetMappingsFile = new File(dataSetRecord.getTestCSVFile());
                    if (!assetMappingsFile.exists()) {//if no mappinds exist, skip
                        continue;
                    }
                    List<String> assetMappings = FileUtils.readLines(assetMappingsFile, config.getTextEncoding());
                    PrintWriter predictionsPrintWriter = null;
                    String predictionsOutputFile = String.format("%s/%d_%s_%s_ps_im_reg_diff_11.csv", outputDirectory, dataSetRecord.getCommitIdex(), dataSetRecord.getCommitHash(), assetType.toLowerCase());
                    try (ProgressBar pb = new ProgressBar("Classifier:", classifiers.size())) {
                        try {

                            if (config.isPrintDetailedPredictionResults()) {
                                predictionsPrintWriter = new PrintWriter(new FileWriter(predictionsOutputFile, true));
                                printMultiLabelOutputHeader(predictionsPrintWriter, trainingDataSet);
                            }

                            List<String> instanceNames = assetMappings.parallelStream().map(m -> m.split(";")[0]).collect(Collectors.toList());

                            for (String classifierName : classifiers.keySet()) {
                                pb.step();
                                pb.setExtraMessage(classifierName);
                                //System.out.println(classifierName);

                                ClassifierRecord classifierRecord = classifiers.get(classifierName);
                                List<MultiLabelLearnerBase> leaners = classifierRecord.getClassifierList();

                                int learnerIndex = 0;
                                int lastLearnerIndex = leaners.size() - 1;
                                long trainTime = 0, testTime = 0;
                                for (MultiLabelLearnerBase learner : leaners) {
                                    try {
                                        Stopwatch stopwatch = Stopwatch.createStarted();
                                        learner.build(classifierRecord.getTrainingSet());
                                        stopwatch.stop();
                                        trainTime += stopwatch.elapsed(TimeUnit.SECONDS);
                                        //now predict
                                        long testMilliseconds = 0;
                                        for (int i = 0; i < numInstances; i++) {
                                            Stopwatch testWatch = Stopwatch.createStarted();
                                            Instance instance = unlabledData.instance(i);
                                            MultiLabelOutput output = learner.makePrediction(instance);
                                            testWatch.stop();
                                            testMilliseconds += testWatch.elapsed(TimeUnit.MILLISECONDS);

                                            if (learnerIndex == lastLearnerIndex) {//only print for the last learner

                                                testTime = TimeUnit.MILLISECONDS.toSeconds(testMilliseconds);
                                                printMultiLabelOutput(classifierRecord, output, instanceNames.get(i), assetMappings, predictionsPrintWriter, dataSetRecord, predictionResultSummaries, predictionResultsInstancePrintWriter, predictionResultsInstanceMeasurePrintWriter,trainTime,testTime);

                                            }
                                        }
                                        learnerIndex++;
                                    } catch (Exception e1) {
                                        e1.printStackTrace();
                                    }
                                }


                            }
                            //at the end of all classifiers, print results summary
                            printPredictionResultSummary(predictionResultsPrintWriter, dataSetRecord, predictionResultSummaries, predictionMeasuresResultsPrintWriter);


                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            if (predictionsPrintWriter != null) {
                                predictionsPrintWriter.close();
                            }

                        }
                    }


                }//close printer here
            } catch (Exception ex) {

            } finally {
                if (predictionResultsPrintWriter != null) {
                    predictionResultsPrintWriter.close();
                }
                if (predictionResultsInstancePrintWriter != null) {
                    predictionResultsInstancePrintWriter.close();
                }
                if (predictionResultsInstanceMeasurePrintWriter != null) {
                    predictionResultsInstanceMeasurePrintWriter.close();
                }
                if (predictionMeasuresResultsPrintWriter != null) {
                    predictionMeasuresResultsPrintWriter.close();
                }
                //copy sumary file
                if (projectData.getConfiguration().isCopyExpResultsToRFolder()) {

                    File psFile = new File(predictionResultsSummaryOutputFile);
                    if (psFile.exists()) {
                        FileUtils.copyFileToDirectory(psFile, projectData.getConfiguration().getrDataFolder());//copy output to the Rfolder
                    }
                    File psInstanceFile = new File(predictionResultsInstanceSummaryOutputFile);
                    if (psInstanceFile.exists()) {
                        FileUtils.copyFileToDirectory(psInstanceFile, projectData.getConfiguration().getrDataFolder());//copy output to the Rfolder
                    }
//                    File psInstanceMeasureFile = new File(predictionResultsInstanceMeasureSummaryOutputFile);
//                    if (psInstanceMeasureFile.exists()) {
//                        FileUtils.copyToDirectory(psInstanceMeasureFile, projectData.getConfiguration().getrDataFolder());//copy output to the Rfolder
//                    }
                    File psMeasuresSummaryFile = new File(predictionMeasuresResultsSummaryOutputFile);
                    if (psMeasuresSummaryFile.exists()) {
                        FileUtils.copyFileToDirectory(psMeasuresSummaryFile, projectData.getConfiguration().getrDataFolder());//copy output to the Rfolder
                    }

                }
            }
        }

    }

    private void printPredictionSummaryHeader(PrintWriter predictionResultsPrintWriter) {

        predictionResultsPrintWriter.print("commitIndex;");
        predictionResultsPrintWriter.print("commit;");
        predictionResultsPrintWriter.print("DataSet;");
        predictionResultsPrintWriter.print("Classifier;");
        predictionResultsPrintWriter.print("TotalInstances;");
        predictionResultsPrintWriter.print("InstancesWithRankedLabels;");
        predictionResultsPrintWriter.print("PercentageOfInstancesWithRankedLabels;");
        predictionResultsPrintWriter.print("InstancesWithPrecisionNull;");
        predictionResultsPrintWriter.print("PercentageOfInstancesWithPrecisionNull;");
        predictionResultsPrintWriter.print("InstancesWithPrecisionZero;");
        predictionResultsPrintWriter.print("PercentageOfInstancesWithPrecisionZero;");
        predictionResultsPrintWriter.print("InstancesWithPrecisionNonZero;");
        predictionResultsPrintWriter.print("PercentageOfInstancesWithPrecisionNonZero;");
        predictionResultsPrintWriter.print("InstancesWithPrecisionOne;");
        predictionResultsPrintWriter.print("PercentageOfInstancesWithPrecisionOne;");
        predictionResultsPrintWriter.print("InstancesWithRecallZero;");
        predictionResultsPrintWriter.print("PercentageOfInstancesWithRecallZero;");
        predictionResultsPrintWriter.print("InstancesWithRecallOne;");
        predictionResultsPrintWriter.print("PercentageOfInstancesWithRecallOne;");
        predictionResultsPrintWriter.print("AveragePrecisionForAllNonNullPrecision;");
        predictionResultsPrintWriter.print("AveragePrecisionForNonZeroPrecision;");
        predictionResultsPrintWriter.print("AverageRecallForAllNonNullPrecision;");
        predictionResultsPrintWriter.print("AverageRecallForAllNonZeroPrecision;");
        predictionResultsPrintWriter.print("AverageFScoreForAllNonNullPrecision;");
        predictionResultsPrintWriter.print("AverageFScoreForAllNonZeroPrecision;");
        predictionResultsPrintWriter.print("TrainTime;");
        predictionResultsPrintWriter.print("TestTime");
        predictionResultsPrintWriter.println();


    }

    private void printPredictionMeasuresSummaryHeader(PrintWriter predictionMeasuresResultsPrintWriter) {

        predictionMeasuresResultsPrintWriter.println("commitIndex;commit;classifier;measure;measureValue;trainTime;testTime");
    }

    private void printPredictionMeasuresSummaryDetail(PrintWriter predictionMeasuresResultsPrintWriter, DataSetRecord dr, ResultSummary rs, String classifier) {

        predictionMeasuresResultsPrintWriter.printf("%d;%s;%s;precision;%.3f;%.3f;%.3f\n", dr.getCommitIdex(), dr.getCommitHash(), classifier, rs.getAveragePrecisionForAllNonNullPrecision(),rs.getTrainTime(),rs.getTestTime());
        predictionMeasuresResultsPrintWriter.printf("%d;%s;%s;recall;%.3f;%.3f;%.3f\n", dr.getCommitIdex(), dr.getCommitHash(), classifier, rs.getAverageRecallForAllNonNullPrecision(),rs.getTrainTime(),rs.getTestTime());
        predictionMeasuresResultsPrintWriter.printf("%d;%s;%s;fscore;%.3f;%.3f;%.3f\n", dr.getCommitIdex(), dr.getCommitHash(), classifier, rs.getAverageFScoreForAllNonNullPrecision(),rs.getTrainTime(),rs.getTestTime());
    }

    private void printPredictionResultSummary(PrintWriter predictionResultsPrintWriter, DataSetRecord trainingRecord, List<PredictionResultSummary> predictionResultSummaries, PrintWriter predictionMeasuresResultsPrintWriter) {
        //print header


        //print rows
        //List<String> datasetNames = predictionResultSummaries.parallelStream().map(PredictionResultSummary::getCommit).distinct().collect(Collectors.toList());

        List<String> classifiers = predictionResultSummaries.parallelStream().map(PredictionResultSummary::getClassifier).distinct().collect(Collectors.toList());


        for (String classifier : classifiers) {
            ResultSummary resultSummary = getResultSummary(trainingRecord.getCommitHash(), classifier, predictionResultSummaries);
            predictionResultsPrintWriter.print(trainingRecord.getCommitIdex() + ";");
            predictionResultsPrintWriter.print(trainingRecord.getCommitHash() + ";");
            predictionResultsPrintWriter.print(trainingRecord.getTrainingFile() + ";");
            predictionResultsPrintWriter.print(classifier + ";");
            predictionResultsPrintWriter.print(resultSummary.getTotalInstances() + ";");
            predictionResultsPrintWriter.print(resultSummary.getInstancesWithRankedLabels() + ";");
            predictionResultsPrintWriter.print(resultSummary.getPercentageOfInstancesWithRankedLabels() + ";");
            predictionResultsPrintWriter.print(resultSummary.getInstancesWithPrecisionNull() + ";");
            predictionResultsPrintWriter.print(resultSummary.getPercentageOfInstancesWithPrecisionNull() + ";");
            predictionResultsPrintWriter.print(resultSummary.getInstancesWithPrecisionZero() + ";");
            predictionResultsPrintWriter.print(resultSummary.getPercentageOfInstancesWithPrecisionZero() + ";");
            predictionResultsPrintWriter.print(resultSummary.getInstancesWithPrecisionNonZero() + ";");
            predictionResultsPrintWriter.print(resultSummary.getPercentageOfInstancesWithPrecisionNonZero() + ";");
            predictionResultsPrintWriter.print(resultSummary.getInstancesWithPrecisionOne() + ";");
            predictionResultsPrintWriter.print(resultSummary.getPercentageOfInstancesWithPrecisionOne() + ";");
            predictionResultsPrintWriter.print(resultSummary.getInstancesWithRecallZero() + ";");
            predictionResultsPrintWriter.print(resultSummary.getPercentageOfInstancesWithRecallZero() + ";");
            predictionResultsPrintWriter.print(resultSummary.getInstancesWithRecallOne() + ";");
            predictionResultsPrintWriter.print(resultSummary.getPercentageOfInstancesWithRecallOne() + ";");
            predictionResultsPrintWriter.print(resultSummary.getAveragePrecisionForAllNonNullPrecision() + ";");
            predictionResultsPrintWriter.print(resultSummary.getAveragePrecisionForNonZeroPrecision() + ";");
            predictionResultsPrintWriter.print(resultSummary.getAverageRecallForAllNonNullPrecision() + ";");
            predictionResultsPrintWriter.print(resultSummary.getAverageRecallForAllNonZeroPrecision() + ";");
            predictionResultsPrintWriter.print(resultSummary.getAverageFScoreForAllNonNullPrecision() + ";");
            predictionResultsPrintWriter.print(resultSummary.getAverageFScoreForAllNonZeroPrecision() + ";");
            predictionResultsPrintWriter.print(resultSummary.getTrainTime() + ";");
            predictionResultsPrintWriter.print(resultSummary.getTestTime());
            predictionResultsPrintWriter.println();
            printPredictionMeasuresSummaryDetail(predictionMeasuresResultsPrintWriter, trainingRecord, resultSummary, classifier);
        }


    }

    private ResultSummary getResultSummary(String dataSetName, String classifierName, List<PredictionResultSummary> predictionResultSummaries) {
        ResultSummary resultSummary = new ResultSummary();
        OptionalDouble value;
        //TotalInstances
        resultSummary.setTotalInstances(
                ((Long) predictionResultSummaries.stream()
                        .filter(p -> p.getCommit().equalsIgnoreCase(dataSetName) && p.getClassifier().equalsIgnoreCase(classifierName))
                        .count()).doubleValue()
        );
        //InstancesWithRankedLabels
        resultSummary.setInstancesWithRankedLabels(
                predictionResultSummaries.stream().filter(p -> p.getCommit().equalsIgnoreCase(dataSetName) && p.getClassifier().equalsIgnoreCase(classifierName))
                        .map(PredictionResultSummary::getRankedInstance)
                        .mapToDouble(Double::doubleValue)
                        .sum()
        );
        //PercentageOfInstancesWithRankedLabels
        resultSummary.setPercentageOfInstancesWithRankedLabels(resultSummary.getInstancesWithRankedLabels() / resultSummary.getTotalInstances());
        //InstancesWithPrecisionNull
        resultSummary.setInstancesWithPrecisionNull(
                ((Long) predictionResultSummaries.stream()
                        .filter(p -> p.getCommit().equalsIgnoreCase(dataSetName) && p.getClassifier().equalsIgnoreCase(classifierName) && (p.getPrecision() == null || p.getPrecision().isNaN()))
                        .count()).doubleValue()
        );
        //PercentageOfInstancesWithPrecisionNull
        resultSummary.setPercentageOfInstancesWithPrecisionNull(resultSummary.getInstancesWithPrecisionNull() / resultSummary.getTotalInstances());
        //InstancesWithPrecisionZero
        resultSummary.setInstancesWithPrecisionZero(
                ((Long) predictionResultSummaries.stream()
                        .filter(p -> p.getCommit().equalsIgnoreCase(dataSetName) && p.getClassifier().equalsIgnoreCase(classifierName) && p.getPrecision().doubleValue() == 0.0)
                        .count()).doubleValue()
        );
        //PercentageOfInstancesWithPrecisionZero
        resultSummary.setPercentageOfInstancesWithPrecisionZero(resultSummary.getInstancesWithPrecisionZero() / resultSummary.getTotalInstances());
        //InstancesWithPrecisionNonZero
        resultSummary.setInstancesWithPrecisionNonZero(
                ((Long) predictionResultSummaries.stream()
                        .filter(p -> p.getCommit().equalsIgnoreCase(dataSetName) && p.getClassifier().equalsIgnoreCase(classifierName) && p.getPrecision() != null && !p.getPrecision().isNaN() && p.getPrecision().doubleValue() > 0.0)
                        .count()).doubleValue()
        );
        //PercentageOfInstancesWithPrecisionNonZero
        resultSummary.setPercentageOfInstancesWithPrecisionNonZero(resultSummary.getInstancesWithPrecisionNonZero() / resultSummary.getTotalInstances());
        //InstancesWithPrecisionOne
        resultSummary.setInstancesWithPrecisionOne(
                ((Long) predictionResultSummaries.stream()
                        .filter(p -> p.getCommit().equalsIgnoreCase(dataSetName) && p.getClassifier().equalsIgnoreCase(classifierName) && p.getPrecision().doubleValue() == 1.0)
                        .count()).doubleValue()
        );
        //PercentageOfInstancesWithPrecisionOne
        resultSummary.setPercentageOfInstancesWithPrecisionOne(resultSummary.getInstancesWithPrecisionOne() / resultSummary.getTotalInstances());
        //InstancesWithRecallZero
        resultSummary.setInstancesWithRecallZero(
                ((Long) predictionResultSummaries.stream()
                        .filter(p -> p.getCommit().equalsIgnoreCase(dataSetName) && p.getClassifier().equalsIgnoreCase(classifierName) && p.getRecall().doubleValue() == 0.0)
                        .count()).doubleValue()
        );
        //PercentageOfInstancesWithRecallZero
        resultSummary.setPercentageOfInstancesWithRecallZero(resultSummary.getInstancesWithRecallZero() / resultSummary.getTotalInstances());
        //InstancesWithRecallOne
        resultSummary.setInstancesWithRecallOne(
                ((Long) predictionResultSummaries.stream()
                        .filter(p -> p.getCommit().equalsIgnoreCase(dataSetName) && p.getClassifier().equalsIgnoreCase(classifierName) && p.getRecall().doubleValue() == 1.0)
                        .count()).doubleValue()
        );
        //PercentageOfInstancesWithRecallOne
        resultSummary.setPercentageOfInstancesWithRecallOne(resultSummary.getInstancesWithRecallOne() / resultSummary.getTotalInstances());
        //AveragePrecisionForAllNonNullPrecision
        value = predictionResultSummaries.stream()
                .filter(p -> p.getCommit().equalsIgnoreCase(dataSetName) && p.getClassifier().equalsIgnoreCase(classifierName) && !p.getPrecision().isNaN())
                .map(PredictionResultSummary::getPrecision)
                .mapToDouble(Double::doubleValue).average();
        resultSummary.setAveragePrecisionForAllNonNullPrecision(
                value.isPresent() ? value.getAsDouble() : 0.0
        );
        //AveragePrecisionForNonZeroPrecision
        value = predictionResultSummaries.stream()
                .filter(p -> p.getCommit().equalsIgnoreCase(dataSetName) && p.getClassifier().equalsIgnoreCase(classifierName) && !p.getPrecision().isNaN() && p.getPrecision().doubleValue() > 0.0)
                .map(PredictionResultSummary::getPrecision)
                .mapToDouble(Double::doubleValue).average();
        resultSummary.setAveragePrecisionForNonZeroPrecision(value.isPresent() ? value.getAsDouble() : 0.0

        );
        //AverageRecallForAllNonNullPrecision
        value = predictionResultSummaries.stream()
                .filter(p -> p.getCommit().equalsIgnoreCase(dataSetName) && p.getClassifier().equalsIgnoreCase(classifierName) && !p.getPrecision().isNaN())
                .map(PredictionResultSummary::getRecall)
                .mapToDouble(Double::doubleValue).average();
        resultSummary.setAverageRecallForAllNonNullPrecision(
                value.isPresent() ? value.getAsDouble() : 0.0
        );
        //AverageRecallForAllNonZeroPrecision
        value = predictionResultSummaries.stream()
                .filter(p -> p.getCommit().equalsIgnoreCase(dataSetName) && p.getClassifier().equalsIgnoreCase(classifierName) && !p.getPrecision().isNaN() && p.getPrecision().doubleValue() > 0.0)
                .map(PredictionResultSummary::getRecall)
                .mapToDouble(Double::doubleValue).average();
        resultSummary.setAverageRecallForAllNonZeroPrecision(
                value.isPresent() ? value.getAsDouble() : 0.0
        );
        //AverageFScoreForAllNonNullPrecision
        value = predictionResultSummaries.stream()
                .filter(p -> p.getCommit().equalsIgnoreCase(dataSetName) && p.getClassifier().equalsIgnoreCase(classifierName) && !p.getPrecision().isNaN())
                .map(PredictionResultSummary::getFscore)
                .mapToDouble(d -> d.isNaN() ? 0.0 : d.doubleValue()).average();
        resultSummary.setAverageFScoreForAllNonNullPrecision(
                value.isPresent() ? value.getAsDouble() : 0.0
        );
        //AverageFScoreForAllNonZeroPrecision
        value = predictionResultSummaries.stream()
                .filter(p -> p.getCommit().equalsIgnoreCase(dataSetName) && p.getClassifier().equalsIgnoreCase(classifierName) && !p.getPrecision().isNaN() && p.getPrecision().doubleValue() != 0.0)
                .map(PredictionResultSummary::getFscore)
                .mapToDouble(d -> d.isNaN() ? 0.0 : d.doubleValue()).average();
        resultSummary.setAverageFScoreForAllNonZeroPrecision(
                value.isPresent() ? value.getAsDouble() : 0.0
        );
        //Train time
        value =predictionResultSummaries.stream()
                .filter(p -> p.getCommit().equalsIgnoreCase(dataSetName) && p.getClassifier().equalsIgnoreCase(classifierName))
                .map(PredictionResultSummary::getTrainTime).mapToDouble(Long::doubleValue).average();
        resultSummary.setTrainTime(value.isPresent() ? value.getAsDouble() : 0.0);
        //Test time
        value =predictionResultSummaries.stream()
                .filter(p -> p.getCommit().equalsIgnoreCase(dataSetName) && p.getClassifier().equalsIgnoreCase(classifierName))
                .map(PredictionResultSummary::getTestTime).mapToDouble(Long::doubleValue).average();
        resultSummary.setTestTime(value.isPresent() ? value.getAsDouble() : 0.0);
        return resultSummary;

    }

    private List<String> getRetrievedLabels(List<String> dataSetLabels, boolean[] bipartitions) {
        List<String> retrievedLabels = new ArrayList<>();
        for (int i = 0; i < bipartitions.length; i++) {
            if (bipartitions[i]) {
                retrievedLabels.add(dataSetLabels.get(i));
            }
        }

        return retrievedLabels;
    }

    private PredictionMeasure getPredictionMeasure(List<String> retrievedLabels, List<String> mappedLabels) {

        //out of all the retrieved instances, how mnay are actaul labels mapped to this instance
        double present = Utilities.getIntersection(retrievedLabels, mappedLabels);
        double totalRetrieved = retrievedLabels.size();
        double totalRelevant = mappedLabels.size();
        //if mapped labels and retrived labels are zero, then precision is 100%, if retrived labels is zero
        double precision = totalRelevant == 0 && totalRetrieved == 0 ? 1.0 : totalRetrieved == 0 && totalRelevant > 0 ? 0.0 : present / totalRetrieved;
        double recall = totalRelevant == 0 && totalRetrieved == 0 ? 1.0 : present / totalRelevant;
        double fMeasure = 2 * (precision * recall) / (precision + recall);
        return new PredictionMeasure(Double.isNaN(precision) ? 0.0 : precision, Double.isNaN(recall) ? 0.0 : recall, Double.isNaN(fMeasure) ? 0.0 : fMeasure);
    }

    /**
     * Given a list labels i.e. feature names e.g., server, client, bubbleGraph, etc, we want to return a list of ranked labels based on their numerical ranked list positions
     * Additionally, if the option RankRelevantLabelsOnly is set to true then we only want to return Labels marked as Relavnt by the bipartitioning. Relevant labels are marked TRUE
     * in the array of bi[artitions
     *
     * @param labels
     * @param ranking
     * @param bipartitions
     * @return
     */
    private List<String> getLabelNameRanking(List<String> labels, int[] ranking, boolean bipartitions[]) {
        List<String> rankedLabels = new ArrayList<>();
        Arrays.stream(ranking).forEach(r -> {
            if (projectData.getConfiguration().isRankRelevantLabelsOnly()) {
                if (bipartitions[r - 1]) {//check that the bipartition value for this rankindex is true; ranks are counted from 1 hence the reason for the subtraction of 1
                    rankedLabels.add(labels.get(r - 1));
                }
            } else {
                rankedLabels.add(labels.get(r - 1));
            }
        });
        return rankedLabels;
    }

    private void printMultiLabelOutput(ClassifierRecord classifierRecord, MultiLabelOutput multiLabelOutput, String instanceName, List<String> assetMappings, PrintWriter predictionsPrintWriter, DataSetRecord dataSetRecord, List<PredictionResultSummary> predictionResultSummaries, PrintWriter instanceWriter, PrintWriter instanceMeasureWriter,long trainTime,long testTime) {


        boolean[] bipartitions = multiLabelOutput.hasBipartition() ? multiLabelOutput.getBipartition() : null;
        double[] confidences = multiLabelOutput.hasConfidences() ? multiLabelOutput.getConfidences() : null;
        double[] pValues = multiLabelOutput.hasPvalues() ? multiLabelOutput.getPvalues() : null;
        int[] ranking = multiLabelOutput.hasRanking() ? multiLabelOutput.getRanking() : null;

        String regex = "(.*)\\d+-\\d+(.*)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(instanceName);
        boolean isFragment = matcher.find();
        List<String> actualLabelsList = getMappedLabels(instanceName, assetMappings);
        String actualLabels = actualLabelsList.parallelStream().collect(Collectors.joining(","));
        actualLabels = StringUtils.isBlank(actualLabels) ? null : actualLabels;

        PredictionResultSummary predictionResultSummary = new PredictionResultSummary();
        List<String> datasetLabels = Arrays.asList(classifierRecord.getTrainingSet().getLabelNames());
        List<String> retrievedLabels = getRetrievedLabels(datasetLabels, bipartitions);
        List<String> rankedLabels = getLabelNameRanking(datasetLabels, ranking, bipartitions);
        String rLabels = rankedLabels.parallelStream().collect(Collectors.joining(","));

        PredictionMeasure predictionMeasure = getPredictionMeasure(retrievedLabels, actualLabelsList);
        //bipartition
        if (projectData.getConfiguration().isPrintDetailedPredictionResults()) {


            predictionsPrintWriter.print(instanceName + ";");
            predictionsPrintWriter.print(classifierRecord.getClassifierName() + ";");
            predictionsPrintWriter.print("BIPARTITION;");
            if (bipartitions != null) {
                for (boolean b : bipartitions) {
                    predictionsPrintWriter.print(b + ";");
                }
            }

            predictionsPrintWriter.print(rLabels + ";");
            predictionsPrintWriter.print(actualLabels + ";");

            //precision
            predictionsPrintWriter.print(predictionMeasure == null ? "" : predictionMeasure.getPrecision() + ";");
            //recall
            predictionsPrintWriter.print(predictionMeasure == null ? "" : predictionMeasure.getRecall() + ";");
            //f-measure
            predictionsPrintWriter.print(predictionMeasure == null ? "" : predictionMeasure.getFmeasure() + ";");
            predictionsPrintWriter.println();
            //confidences
            printOptionalDoubleOutput(confidences, "CONFIDENCES", predictionsPrintWriter);
            //pValues
            printOptionalDoubleOutput(pValues, "PVALUES", predictionsPrintWriter);
        }
        printResultsInstanceOutputDetail(instanceWriter, dataSetRecord, instanceName, classifierRecord.getClassifierName(), rLabels, actualLabels, predictionMeasure.getPrecision(), predictionMeasure.getRecall(), predictionMeasure.getFmeasure());
        //printResultsInstanceMeasureOutputDetail(instanceMeasureWriter, dataSetRecord, instanceName, classifierRecord.getClassifierName(), rLabels, actualLabels, predictionMeasure.getPrecision(), predictionMeasure.getRecall(), predictionMeasure.getFmeasure());

        //set prediction results

        //if (!StringUtils.isBlank(actualLabels)) {//only add instances that have actual labels (features that we can use to measure precision and recall)

        predictionResultSummary.setClassifier(classifierRecord.getClassifierName());
        predictionResultSummary.setCommit(dataSetRecord.getCommitHash());
        predictionResultSummary.setLabeldInstance(!StringUtils.isBlank(actualLabels) ? 1.0 : 0.0);
        predictionResultSummary.setInstances(1.0);
        predictionResultSummary.setRankedInstance(ranking != null && ranking.length > 0.0 ? 1.0 : 0.0);
        predictionResultSummary.setPrecision(predictionMeasure == null ? Double.NaN : predictionMeasure.getPrecision());
        predictionResultSummary.setRecall(predictionMeasure == null ? Double.NaN : predictionMeasure.getRecall());
        predictionResultSummary.setFscore(predictionMeasure == null ? Double.NaN : predictionMeasure.getFmeasure());
        predictionResultSummary.setTrainTime(trainTime);
        predictionResultSummary.setTestTime(testTime);

        predictionResultSummaries.add(predictionResultSummary);
        //}
    }

    private void printOptionalDoubleOutput(double[] confidences, String outputName, PrintWriter predictionsPrintWriter) {
        if (confidences != null) {
            predictionsPrintWriter.print(";");//instance name is already printed out
            predictionsPrintWriter.print(";");//classifier name already printed out
            predictionsPrintWriter.print(outputName + ";");

            for (double b : confidences) {
                predictionsPrintWriter.print(b + ";");
            }

            predictionsPrintWriter.print(" ;");
            predictionsPrintWriter.print(" ;");
            predictionsPrintWriter.print(" ;");
            predictionsPrintWriter.print(" ;");
            predictionsPrintWriter.println();
        }
    }

    private List<String> getMappedLabels(String instanceName, List<String> assetMappings) {

        List<String> mappedLabels = new ArrayList<>();
        List<String> instanceMappings = assetMappings.stream().filter(a -> a.split(";")[0].equalsIgnoreCase(instanceName)).collect(Collectors.toList());
        for (String mapping : instanceMappings) {
            String parts[] = mapping.split(";");
            if (parts.length == 2) {
                mappedLabels.addAll(Arrays.stream(parts[1].split(",")).map(a -> a.trim()).collect(Collectors.toList()));
            }
        }
        return mappedLabels.parallelStream().distinct().collect(Collectors.toList());

    }

    private void printMultiLabelOutputHeader(PrintWriter predictionsPrintWriter, MultiLabelInstances traininfDataSet) {

        //Print header
        predictionsPrintWriter.print("InstanceName;Classifier;OutputName;");

        //labels
        String[] labelNames = traininfDataSet.getLabelNames();
        for (String labelName : labelNames) {
            predictionsPrintWriter.print(labelName + ";");

        }
        predictionsPrintWriter.print("Ranking;ActualLabels;Precision;Recall;F-Measure");

        predictionsPrintWriter.println();

    }

    private void printResultsInstanceOutputHeader(PrintWriter predictionsInstancePrintWriter) {
        //Print header
        predictionsInstancePrintWriter.println("commitIndex;commitHash;trainingFile;testFile;testCommitIndex;testCommitHash;project;instanceName;classifier;Ranking;ActualLabels;precision;recall;fscore");
    }

    private void printResultsInstanceOutputDetail(PrintWriter predictionsInstancePrintWriter, DataSetRecord dr, String instance, String classifier, String rankedLabels, String actualLabels, double precision, double recall, double fscore) {
        //Print header
        predictionsInstancePrintWriter.printf("%d;%s;%s;%s;%d;%s;%s;%s;%s;%s;%s;%.4f;%.4f;%.4f\n",
                dr.getCommitIdex(), dr.getCommitHash(), dr.getTrainingFile(), dr.getTestFile(), dr.getTestCommitIndex(), dr.getTestCommitHash(), dr.getProject(), instance, classifier, rankedLabels, actualLabels, precision, recall, fscore);
    }

    private void printResultsInstanceMeasureOutputHeader(PrintWriter predictionsInstanceMeasurePrintWriter) {
        //Print header
        predictionsInstanceMeasurePrintWriter.println("commitIndex;commitHash;trainingFile;testFile;testCommitIndex;testCommitHash;project;instanceName;classifier;Ranking;ActualLabels;measure;measureValue");
    }

    private void printResultsInstanceMeasureOutputDetail(PrintWriter predictionsInstancePrintWriter, DataSetRecord dr, String instance, String classifier, String rankedLabels, String actualLabels, double precision, double recall, double fscore) {
        //Print header
        predictionsInstancePrintWriter.printf("%d;%s;%s;%s;%d;%s;%s;%s;%s;%s;%s;precision;%.4f\n",
                dr.getCommitIdex(), dr.getCommitHash(), dr.getTrainingFile(), dr.getTestFile(), dr.getTestCommitIndex(), dr.getTestCommitHash(), dr.getProject(), instance, classifier, rankedLabels, actualLabels, precision);
        predictionsInstancePrintWriter.printf("%d;%s;%s;%s;%d;%s;%s;%s;%s;%s;%s;recall;%.4f\n",
                dr.getCommitIdex(), dr.getCommitHash(), dr.getTrainingFile(), dr.getTestFile(), dr.getTestCommitIndex(), dr.getTestCommitHash(), dr.getProject(), instance, classifier, rankedLabels, actualLabels, recall);

        predictionsInstancePrintWriter.printf("%d;%s;%s;%s;%d;%s;%s;%s;%s;%s;%s;fscore;%.4f\n",
                dr.getCommitIdex(), dr.getCommitHash(), dr.getTrainingFile(), dr.getTestFile(), dr.getTestCommitIndex(), dr.getTestCommitHash(), dr.getProject(), instance, classifier, rankedLabels, actualLabels, fscore);

    }

    public Instances getUnlabledData(String testFile) throws IOException {
        Instances unlabledData = null;
        if (testFile != null) {
            FileReader reader = new FileReader(testFile);
            unlabledData = new Instances(reader);
        }
        return unlabledData;
    }

    private boolean isModelIncluded(ModelGroup modelGroup, String modelName) {
        return (modelGroup == ModelGroup.EVALUATION && projectData.getConfiguration().getEvaluationModels().contains(modelName)) ||
                (modelGroup == ModelGroup.PREDICTION && projectData.getConfiguration().getPredictionModels().contains(modelName)) ||
                (modelGroup == ModelGroup.FINAL_PREDICTION && projectData.getConfiguration().getFinalPredictionModels().contains(modelName));
    }

    private void addClassifierToList(String classifierName, List<MultiLabelLearnerBase> leanersList, Map<String, ClassifierRecord> classifiers, MultiLabelInstances trainingSet) {
        ClassifierRecord classifierRecord = prepareExecution(classifierName, trainingSet);
        classifierRecord.setClassifierList(classifierName.equalsIgnoreCase("CC") ? getClassifierChains(classifierRecord) : leanersList);
        classifiers.put(classifierName, classifierRecord);
    }

    public ClassifierRecord prepareExecution(String classifierName, MultiLabelInstances trainingSet) {
        ClassifierRecord classifierRecord = new ClassifierRecord();
        try {
            classifierRecord.setClassifierName(classifierName);
            classifierRecord.setTrainingSet(trainingSet);


        } catch (Exception e) {
            e.printStackTrace();
        }
        return classifierRecord;
    }

    private List<MultiLabelLearnerBase> getClassifierChains(ClassifierRecord classifierRecord) {
        int chainedIterations = projectData.getConfiguration().getChainIterations();
        List<MultiLabelLearnerBase> classifierChainsList = new ArrayList<>();

        /* The seeds are 10, 20, 30, ... */
        for (int i = 1; i <= chainedIterations; i++) {
            //time_in = System.currentTimeMillis();
            Random rand = new Random(i * 10);

            //Get random chain
            int[] chain = new int[classifierRecord.getTrainingSet().getNumLabels()];
            for (int c = 0; c < classifierRecord.getTrainingSet().getNumLabels(); c++) {
                chain[c] = c;
            }

            for (int c = 0; c < classifierRecord.getTrainingSet().getNumLabels(); c++) {
                int r = rand.nextInt(classifierRecord.getTrainingSet().getNumLabels());
                int swap = chain[c];
                chain[c] = chain[r];
                chain[r] = swap;
            }

            ClassifierChain learner = new ClassifierChain(new J48(), chain);
            classifierChainsList.add(learner);
        }
        return classifierChainsList;
    }

    private List<MultiLabelLearnerBase> getEnsembleChains(EnsembleChain ensembleChain) {
        int chainedIterations = projectData.getConfiguration().getChainIterations();
        List<MultiLabelLearnerBase> classifierChainsList = new ArrayList<>();
        /* The seeds are 10, 20, 30, ... */
        for (int i = 1; i <= chainedIterations; i++) {

            MultiLabelLearnerBase learner = null;
            if (ensembleChain == EnsembleChain.BPMLL) {
                learner = new BPMLL(i * 10);
            } else if (ensembleChain == EnsembleChain.EBR) {
                learner = new EBR(i * 10);
            } else if (ensembleChain == EnsembleChain.ECC) {
                learner = new ECC(i * 10);
            } else if (ensembleChain == EnsembleChain.EPS) {
                learner = new EPS(i * 10);
            } else if (ensembleChain == EnsembleChain.RAkEL) {
                RAkEL rAkEL = new RAkEL(new LabelPowerset(new J48()));
                rAkEL.setSeed(i * 10);
                learner = rAkEL;

            } else if (ensembleChain == EnsembleChain.RAkELd) {
                RAkELd rAkEL = new RAkELd(new LabelPowerset(new J48()));
                rAkEL.setSeed(i * 10);
                learner = rAkEL;

            }
            classifierChainsList.add(learner);
        }
        return classifierChainsList;
    }

    private Map<String, ClassifierRecord> createClassifiersList(ModelGroup modelGroup, MultiLabelInstances trainingSet) {
        Map<String, ClassifierRecord> classifiers = new LinkedHashMap<>();


        if (isModelIncluded(modelGroup, "BR")) {
            //Binary relevance
            List<MultiLabelLearnerBase> binaryRelevanceList = new ArrayList<>();
            binaryRelevanceList.add(new BinaryRelevance(new J48()));
            addClassifierToList("BR", binaryRelevanceList, classifiers, trainingSet);
        }

        if (isModelIncluded(modelGroup, "CC")) {
            //classificer chains
            addClassifierToList("CC", null, classifiers, trainingSet);//for classifier chains we use a different method to get list of learners so we don;t need t precreate the list here
        }
        //AdaBoost.MH
        if (isModelIncluded(modelGroup, "AdaBoostMH")) {
            List<MultiLabelLearnerBase> adaBoostList = new ArrayList<>();
            adaBoostList.add(new AdaBoostMH());
            addClassifierToList("AdaBoostMH", adaBoostList, classifiers, trainingSet);
        }
        //Calibrated Label Ranking
        if (isModelIncluded(modelGroup, "CLR")) {
            List<MultiLabelLearnerBase> clrList = new ArrayList<>();
            clrList.add(new CalibratedLabelRanking(new J48()));
            addClassifierToList("CLR", clrList, classifiers, trainingSet);
        }
        //EBR--enseble of BR
        if (isModelIncluded(modelGroup, "EBR")) {
            addClassifierToList("EBR", getEnsembleChains(EnsembleChain.EBR), classifiers, trainingSet);
        }
        //ECC--enseble of CC
        if (isModelIncluded(modelGroup, "ECC")) {
            addClassifierToList("ECC", getEnsembleChains(EnsembleChain.ECC), classifiers, trainingSet);
        }
        //EPS--enseble of Pruned sets
        if (isModelIncluded(modelGroup, "EPS")) {
            addClassifierToList("EPS", getEnsembleChains(EnsembleChain.EPS), classifiers, trainingSet);
        }
        //IBLR-Instanc based logistic regression
        if (isModelIncluded(modelGroup, "IBLR")) {
            List<MultiLabelLearnerBase> iblrList = new ArrayList<>();
            iblrList.add(new IBLR_ML());
            addClassifierToList("IBLR", iblrList, classifiers, trainingSet);
        }
        //LP-Label Poweset
        if (isModelIncluded(modelGroup, "LP")) {
            List<MultiLabelLearnerBase> lpList = new ArrayList<>();
            lpList.add(new LabelPowerset(new J48()));
            addClassifierToList("LP", lpList, classifiers, trainingSet);
        }
        //MLkNN-MultiLabel kNearest Neighbors
        if (isModelIncluded(modelGroup, "MLkNN")) {
            List<MultiLabelLearnerBase> mlkNNList = new ArrayList<>();
            mlkNNList.add(new MLkNN());
            addClassifierToList("MLkNN", mlkNNList, classifiers, trainingSet);
        }
        //MLS-MultiLabel Stacking
        if (isModelIncluded(modelGroup, "MLS")) {
            List<MultiLabelLearnerBase> mlsList = new ArrayList<>();
            mlsList.add(new MultiLabelStacking());
            addClassifierToList("MLS", mlsList, classifiers, trainingSet);
        }

        //PS-PrunedSets
        if (isModelIncluded(modelGroup, "PS")) {
            List<MultiLabelLearnerBase> psList = new ArrayList<>();
            psList.add(new PrunedSets());
            addClassifierToList("PS", psList, classifiers, trainingSet);
        }

        //RAkEL--Random k-label sets
        if (isModelIncluded(modelGroup, "RAkEL")) {
            addClassifierToList("RAkEL", getEnsembleChains(EnsembleChain.RAkEL), classifiers, trainingSet);
        }
        //RAkELd--Random k-label sets
        if (isModelIncluded(modelGroup, "RAkELd")) {
            addClassifierToList("RAkELd", getEnsembleChains(EnsembleChain.RAkELd), classifiers, trainingSet);
        }

        return classifiers;

    }


    private int[] getFeatureIndices(String featureSelectionSummaryFile) throws IOException {
        File summaryFile = new File(featureSelectionSummaryFile);
        double topNfeatures = projectData.getConfiguration().getTopNFeatures();
        //if summary file is missing, use all features
        List<String> features = projectData.getMLFeaturesNames();
        int[] indices = null;
        if (!summaryFile.exists() || topNfeatures == features.size()) {
            indices = new int[features.size()];
            for (int i = 0; i < features.size(); i++) {
                indices[i] = i;
            }
            return indices;
        } else {
            //read file
            List<String> lines = FileUtils.readLines(summaryFile, projectData.getConfiguration().getTextEncoding());
            if(lines.size()<=1){
                return null;//now feature selection for this level
            }
            List<FeatureSelectionResultSummary> featureSelectionResultSummaries = new ArrayList<>();
            List<String> headerParts = Arrays.asList(lines.get(0).split(";"));
            for (int i = 1; i < lines.size(); i++) {
                String[] parts = lines.get(i).split(";");
                if(parts.length<10){
                    continue;//skip those lines that have no weighted metrics for the feature selection
                }
                FeatureSelectionResultSummary fs = new FeatureSelectionResultSummary();
                fs.setIndex(Integer.parseInt(parts[0]));
                fs.setAttribute(parts[1]);
                fs.setProject(parts[2]);
                fs.setIs_balanced(parts[3]);
                fs.setIs_normalized(parts[4]);
                if (headerParts.contains("IG_BR_rank")) {
                    fs.setIG_BR_rank(Double.parseDouble(parts[headerParts.indexOf("IG_BR_rank")]));
                }
                if (headerParts.contains("IG_BR_weight")) {
                    fs.setIG_BR_weight(Double.parseDouble(parts[headerParts.indexOf("IG_BR_weight")]));
                }
                if (headerParts.contains("IG_LP_rank")) {
                    fs.setIG_LP_rank(Double.parseDouble(parts[headerParts.indexOf("IG_LP_rank")]));
                }
                if (headerParts.contains("IG_LP_weight")) {
                    fs.setIG_LP_weight(Double.parseDouble(parts[headerParts.indexOf("IG_LP_weight")]));
                }
                if (headerParts.contains("IG_rank")) {
                    fs.setIG_rank(Double.parseDouble(parts[headerParts.indexOf("IG_rank")]));
                }
                if (headerParts.contains("IG_weight")) {
                    fs.setIG_weight(Double.parseDouble(parts[headerParts.indexOf("IG_weight")]));
                }
                if (headerParts.contains("RF_BR_rank")) {
                    fs.setRF_BR_rank(Double.parseDouble(parts[headerParts.indexOf("RF_BR_rank")]));
                }
                if (headerParts.contains("RF_BR_weight")) {
                    fs.setRF_BR_weight(Double.parseDouble(parts[headerParts.indexOf("RF_BR_weight")]));
                }
                if (headerParts.contains("RF_LP_rank")) {
                    fs.setRF_LP_rank(Double.parseDouble(parts[headerParts.indexOf("RF_LP_rank")]));
                }
                if (headerParts.contains("RF_LP_weight")) {
                    fs.setRF_LP_weight(Double.parseDouble(parts[headerParts.indexOf("RF_LP_weight")]));
                }
                if (headerParts.contains("RF_rank")) {
                    fs.setRF_rank(Double.parseDouble(parts[headerParts.indexOf("RF_rank")]));
                }
                if (headerParts.contains("RF_weight")) {
                    fs.setRF_weight(Double.parseDouble(parts[headerParts.indexOf("RF_weight")]));
                }
                if (headerParts.contains("overall_rank")) {
                    fs.setOverall_rank(Double.parseDouble(parts[headerParts.indexOf("overall_rank")]));
                }
                if (headerParts.contains("overall_weight")) {
                    fs.setOverall_weight(Double.parseDouble(parts[headerParts.indexOf("overall_weight")]));
                }
                featureSelectionResultSummaries.add(fs);
            }
            FeatureSelectionRank selectionRank = projectData.getConfiguration().getFeatureSelectionRank();
            TopNFeaturesMethod topNFeaturesMethod = projectData.getConfiguration().getTopNFeaturesMethod();

            //rank results
            List<FeatureSelectionResultSummary> filteredResults = featureSelectionResultSummaries.stream().filter(r -> r.getProject().equalsIgnoreCase(projectData.getConfiguration().getProjectRepository().getName()) &&
                    r.getIs_balanced().equalsIgnoreCase(projectData.getConfiguration().getBalancedDataSetName()) &&
                    r.getIs_normalized().equalsIgnoreCase(projectData.getConfiguration().getNormalizedDataSetName())).collect(Collectors.toList());
            FeastureSelectionComparator feastureSelectionComparator = new FeastureSelectionComparator(selectionRank, topNFeaturesMethod);
            Collections.sort(filteredResults, feastureSelectionComparator);


            if (topNFeaturesMethod == TopNFeaturesMethod.Percent) {
                int thresholdIndex = (int) (filteredResults.size() * (topNfeatures > 1 ? topNfeatures / 100 : topNfeatures));
                List<Integer> indexes = filteredResults.stream().filter(r -> filteredResults.indexOf(r) < thresholdIndex).map(FeatureSelectionResultSummary::getIndex).collect(Collectors.toList());
                indices = getIndices(indexes, indices);

            }
            if (topNFeaturesMethod == TopNFeaturesMethod.Threshold) {
                List<Integer> indexes = filteredResults.stream().filter(r ->
                        (selectionRank == FeatureSelectionRank.IG_rank ? r.getIG_weight() >= topNfeatures :
                                (selectionRank == FeatureSelectionRank.IG_BR_rank ? r.getIG_BR_weight() >= topNfeatures :
                                        (selectionRank == FeatureSelectionRank.IG_LP_rank ? r.getIG_LP_weight() >= topNfeatures :
                                                (selectionRank == FeatureSelectionRank.RF_rank ? r.getRF_weight() >= topNfeatures :
                                                        (selectionRank == FeatureSelectionRank.RF_BR_rank ? r.getRF_BR_weight() >= topNfeatures :
                                                                (selectionRank == FeatureSelectionRank.RF_LP_rank ? r.getRF_LP_weight() >= topNfeatures : r.getOverall_weight() >= topNfeatures)))))))
                        .map(FeatureSelectionResultSummary::getIndex).collect(Collectors.toList());
                indices = getIndices(indexes, indices);
            } else {

                List<Integer> indexes = filteredResults.stream().filter(r -> filteredResults.indexOf(r) < topNfeatures).map(FeatureSelectionResultSummary::getIndex).collect(Collectors.toList());
                indices = getIndices(indexes, indices);
            }


        }

        return indices;
    }

    private int[] getIndices(List<Integer> indexes, int[] indices) {
        indices = new int[indexes.size()];
        for (int i = 0; i < indexes.size(); i++) {
            indices[i] = indexes.get(i);
        }
        return indices;
    }
}
