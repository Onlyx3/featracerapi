package se.gu.ml.experiment;

import me.tongfei.progressbar.ProgressBar;
import meka.classifiers.multilabel.*;
import meka.core.OptionUtils;
import meka.events.LogEvent;
import meka.events.LogListener;
import meka.experiment.DefaultExperiment;
import meka.experiment.Experiment;
import meka.experiment.datasetproviders.LocalDatasetProvider;
import meka.experiment.evaluationstatistics.KeyValuePairs;
import meka.experiment.evaluators.CrossValidation;
import meka.experiment.evaluators.RepeatedRuns;
import meka.experiment.events.*;
import meka.experiment.statisticsexporters.*;
import mulan.data.MultiLabelInstances;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import se.gu.main.ProjectData;
import se.gu.ml.alg.MulanAlgorithmRunner;
import se.gu.ml.preprocessing.DataBalancingProcessor;
import se.gu.ml.preprocessing.DatasetAnalyzer;
import se.gu.utils.Utilities;
import weka.core.Instances;
import weka.core.Utils;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class ExperimentRunner implements Serializable {
    private static final long serialVersionUID = -7877609495320631848L;
    private ProjectData projectData;
    private String outputDirectory;
    private String predictionSummaryFile, crossValidationSummaryFile, datasetStatsSummaryFile;


    public ExperimentRunner(ProjectData projectData) throws IOException {
        this.projectData = projectData;
        outputDirectory = String.format("%s/%s/%s/%s/%s/%dfeatures", projectData.getConfiguration().getAnalysisDirectory(), "ExperimentData", projectData.getConfiguration().getProjectRepository().getName(),
                projectData.getConfiguration().getCodeAbstractionLevel(),
                new File(projectData.getConfiguration().getDataFilesSubDirectory()).getName(), (int) projectData.getConfiguration().getTopNFeatures());
        boolean cleanDirectory = projectData.getConfiguration().getExperimentStartingCommit()==0;
        Utilities.createOutputDirectory(outputDirectory, cleanDirectory);
    }

    public void runExperiment() throws Exception {
        String projectName = projectData.getConfiguration().getProjectShortName();
        String balanced = projectData.getConfiguration().getBalancedDataSetName();
        String normalised = projectData.getConfiguration().getNormalizedDataSetName();
        String fragmentThreshold = projectData.getConfiguration().getFragmentThresholdDataSetName();
        int nFeatures = (int) projectData.getConfiguration().getTopNFeatures();
        //first create balanced data sets
        if (projectData.getConfiguration().isRunDatabalancer()) {
            DataBalancingProcessor balancingProcessor = new DataBalancingProcessor(projectData);
            balancingProcessor.createBalancedDataSets();
            //Thread.sleep(3000);
            balancingProcessor.moveFiles();
        }
        //get dataset statistics
        if (projectData.getConfiguration().isGenerateDatasetStatistics()) {
            String datasetStatFile = String.format("%s/%s_%s_datasetstats_%s_%s_%s.csv", outputDirectory, projectName, projectData.getConfiguration().getCodeAbstractionLevel().toString().toLowerCase(), balanced, normalised, fragmentThreshold);
            DatasetAnalyzer datasetAnalyzer = new DatasetAnalyzer(projectData, datasetStatFile);
            datasetAnalyzer.createDatasetAnalytics();

            if (projectData.getConfiguration().isCopyExpResultsToRFolder()) {
                //copy files to the Rfolder
                FileUtils.copyToDirectory(new File(datasetStatFile), projectData.getConfiguration().getrDataFolder());//copy output to the Rfolder
            }
        }

        //now run experiment
        if (projectData.getConfiguration().isRunExperiment()) {
            if (projectData.getConfiguration().getMlMethod() == MLMethod.MEKA) {
                runMEKAExperiment();
            } else {
                runMULANExperiment();
            }
        }


    }

    private void printFeatureSelectionRecord(PrintWriter pw, int commit, String dataSetName, String project, FeatureSelectionResult result, String isBalanced, String isNormalized, String fragmentThreshold) {
        pw.printf("%d;%s;%s;%s;%d;%f;%s;%s;%s;%s;%s\n", commit, dataSetName, project, result.getAttribute(), result.getRank(), result.getWeight(), result.getEvalMethod(), result.getEvalMethodParent(), isBalanced, isNormalized, fragmentThreshold);
    }

    /**
     * @param attribute
     * @param evalFilter
     * @param level                      filter level 'm' for individual method e.g., IG-BR, 'p' for parent method e.g, IG, and 'o' for overall average of value
     * @param valueType                  what value to average from the list; 'r' for rank and 'w' for weight
     * @param allFeatureSelectionResults
     * @return
     */
    private double getValue(String attribute, String evalFilter, String level, String valueType, List<FeatureSelectionResult> allFeatureSelectionResults) {
        if (valueType.equalsIgnoreCase("r")) {
            //rank
            if (level.equalsIgnoreCase("m")) {
                return allFeatureSelectionResults.parallelStream().filter(r -> r.getAttribute().equalsIgnoreCase(attribute) && r.getEvalMethod().equalsIgnoreCase(evalFilter)).map(FeatureSelectionResult::getRank).mapToDouble(Integer::doubleValue).average().getAsDouble();
            } else if (level.equalsIgnoreCase("p")) {
                return allFeatureSelectionResults.parallelStream().filter(r -> r.getAttribute().equalsIgnoreCase(attribute) && r.getEvalMethodParent().equalsIgnoreCase(evalFilter)).map(FeatureSelectionResult::getRank).mapToDouble(Integer::doubleValue).average().getAsDouble();
            } else {
                return allFeatureSelectionResults.parallelStream().filter(r -> r.getAttribute().equalsIgnoreCase(attribute)).map(FeatureSelectionResult::getRank).mapToDouble(Integer::doubleValue).average().getAsDouble();

            }
        } else {
            //weight
            if (level.equalsIgnoreCase("m")) {
                return allFeatureSelectionResults.parallelStream().filter(r -> r.getAttribute().equalsIgnoreCase(attribute) && r.getEvalMethod().equalsIgnoreCase(evalFilter) && (Math.abs(r.getWeight()) > 0)).map(FeatureSelectionResult::getWeight).mapToDouble(Double::doubleValue).average().orElse(0.0);
            } else if (level.equalsIgnoreCase("p")) {
                return allFeatureSelectionResults.parallelStream().filter(r -> r.getAttribute().equalsIgnoreCase(attribute) && r.getEvalMethodParent().equalsIgnoreCase(evalFilter) && (Math.abs(r.getWeight()) > 0)).map(FeatureSelectionResult::getWeight).mapToDouble(Double::doubleValue).average().orElse(0.0);
            } else {
                return allFeatureSelectionResults.parallelStream().filter(r -> r.getAttribute().equalsIgnoreCase(attribute) && (Math.abs(r.getWeight()) > 0)).map(FeatureSelectionResult::getWeight).mapToDouble(Double::doubleValue).average().orElse(0.0);

            }
        }
    }

    private void printFSItem(String fsMethods, PrintWriter writer, String method, String item) {
        if (fsMethods.contains(method)) {
            writer.printf("%s", item);
        }
    }

    private void printAllFeatureSelectionResults(String projectName, String fileName, String isBalanced, String isNormalized, String fragmentThreshold, List<FeatureSelectionResult> allFeatureSelectionResults) {
        PrintWriter writer = null;
        try {

            writer = new PrintWriter(new FileWriter(fileName, true));
            String featureSelectionMethods = Arrays.stream(projectData.getConfiguration().getFeatureSelectionMethods()).collect(Collectors.joining(","));

            //print header
            writer.printf("index;attribute;project;is_balanced;is_normalized;fragmentThreshold;");
            printFSItem(featureSelectionMethods, writer, "IG-BR", "IG_BR_rank;");
            printFSItem(featureSelectionMethods, writer, "IG-BR", "IG_BR_weight;");
            printFSItem(featureSelectionMethods, writer, "IG-LP", "IG_LP_rank;");
            printFSItem(featureSelectionMethods, writer, "IG-LP", "IG_LP_weight;");
            printFSItem(featureSelectionMethods, writer, "IG", "IG_rank;");
            printFSItem(featureSelectionMethods, writer, "IG", "IG_weight;");
            printFSItem(featureSelectionMethods, writer, "RF-BR", "RF_BR_rank;");
            printFSItem(featureSelectionMethods, writer, "RF-BR", "RF_BR_weight;");
            printFSItem(featureSelectionMethods, writer, "RF-LP", "RF_LP_rank;");
            printFSItem(featureSelectionMethods, writer, "RF-LP", "RF_LP_weight;");
            printFSItem(featureSelectionMethods, writer, "RF", "RF_rank;");
            printFSItem(featureSelectionMethods, writer, "RF", "RF_weight;");
            writer.print("overall_rank;overall_weight\n");

            List<String> attributes = projectData.getMLFeatures();
            try (ProgressBar pb = new ProgressBar("Summarizing Feature Ranking:", attributes.size())) {
                for (String a : attributes) {
                    pb.step();
                    pb.setExtraMessage(a);
                    double IG_BR_rank, IG_BR_weight, IG_LP_rank, IG_LP_weight, IG_rank, IG_weight, RF_BR_rank, RF_BR_weight, RF_LP_rank, RF_LP_weight, RF_rank, RF_weight, overall_rank, overall_weight;
                    writer.printf("%d;%s;%s;%s;%s;%s", attributes.indexOf(a), a, projectName, isBalanced, isNormalized, fragmentThreshold);
                    if (featureSelectionMethods.contains("IG-BR")) {
                        IG_BR_rank = getValue(a, "IG-BR", "m", "r", allFeatureSelectionResults);
                        IG_BR_weight = getValue(a, "IG-BR", "m", "w", allFeatureSelectionResults);
                        writer.printf(";%f;%f", IG_BR_rank, IG_BR_weight);
                    }
                    if (featureSelectionMethods.contains("IG-LP")) {
                        IG_LP_rank = getValue(a, "IG-LP", "m", "r", allFeatureSelectionResults);
                        IG_LP_weight = getValue(a, "IG-LP", "m", "w", allFeatureSelectionResults);
                        writer.printf(";%f;%f", IG_LP_rank, IG_LP_weight);
                    }
                    if (featureSelectionMethods.contains("IG")) {
                        IG_rank = getValue(a, "IG", "p", "r", allFeatureSelectionResults);
                        IG_weight = getValue(a, "IG", "p", "w", allFeatureSelectionResults);
                        writer.printf(";%f;%f", IG_rank, IG_weight);
                    }
                    if (featureSelectionMethods.contains("RF-BR")) {
                        RF_BR_rank = getValue(a, "RF-BR", "m", "r", allFeatureSelectionResults);
                        RF_BR_weight = getValue(a, "RF-BR", "m", "w", allFeatureSelectionResults);
                        writer.printf(";%f;%f", RF_BR_rank, RF_BR_weight);
                    }
                    if (featureSelectionMethods.contains("RF-LP")) {
                        RF_LP_rank = getValue(a, "RF-LP", "m", "r", allFeatureSelectionResults);
                        RF_LP_weight = getValue(a, "RF-LP", "m", "w", allFeatureSelectionResults);
                        writer.printf(";%f;%f", RF_LP_rank, RF_LP_weight);
                    }
                    if (featureSelectionMethods.contains("RF")) {
                        RF_rank = getValue(a, "RF", "p", "r", allFeatureSelectionResults);
                        RF_weight = getValue(a, "RF", "p", "w", allFeatureSelectionResults);
                        writer.printf(";%f;%f", RF_rank, RF_weight);
                    }

                    overall_rank = getValue(a, "o", "o", "r", allFeatureSelectionResults);
                    overall_weight = getValue(a, "o", "o", "w", allFeatureSelectionResults);

                    writer.printf(";%f;%f\n", overall_rank, overall_weight);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }

    }

    private void runMULANExperiment() throws Exception {
        //create list of all algortihm runners
        String abstractionLevel = projectData.getConfiguration().getCodeAbstractionLevel().toString();
        double topNfeatures = projectData.getConfiguration().getTopNFeatures();
        List<String> features = projectData.getMLFeatures();
        //String outputDirectory = String.format("%s/%s/MULAN/%s", projectData.getConfiguration().getAnalysisDirectory(), "ExperimentData", abstractionLevel);


        EvaluationMethod evaluationMethod = projectData.getConfiguration().getEvaluationMethod();
        int kFolds = evaluationMethod == EvaluationMethod.CV ? projectData.getConfiguration().getkFolds() : -1;
        int tempKFolds = kFolds;
        int nIterations = projectData.getConfiguration().getChainIterations();
        //print headers
        boolean printHeader = true;
        boolean printPredictionSummaryHeader = true;
        boolean predictionsHeaderPrinted = false;
        //iterate through list of all experient files; calling classifier for each file
        List<ExperiementFileRecord> experiementFileRecords = getExperiementFileRecords();
        int count = 0;
        String projectName = projectData.getConfiguration().getProjectShortName();

        String balanced = projectData.getConfiguration().getBalancedDataSetName();
        String normalized = projectData.getConfiguration().getNormalizedDataSetName();
        String fragmentThreshold = projectData.getConfiguration().getFragmentThresholdDataSetName();

        String datasetFile;
        String testDataSetFile;
        String predictionsFile;
        String xmlFlatLabelsFile;
        String xmlMainLabelsFile;
        //&begin [ML::Experiment::FeatureSelection]
        //feature selection
        String projectFeatureSelectionFile = String.format("%s/FeatureSelection/%s_%s_%s.csv", projectData.getConfiguration().getAnalysisDirectory(), projectName, projectData.getConfiguration().getCodeAbstractionLevel().toString().toLowerCase(), fragmentThreshold);

        String projectFeatureSelectionSummaryFile = String.format("%s/FeatureSelection/%s_%s_%s_SUMMARY.csv", projectData.getConfiguration().getAnalysisDirectory(), projectName, projectData.getConfiguration().getCodeAbstractionLevel().toString().toLowerCase(), fragmentThreshold);


        List<FeatureSelectionResult> allFeatureSelectionResults = new ArrayList<>();
        FeatureSelector featureSelector = new FeatureSelector(projectData);
        int commitsToExecute = projectData.getConfiguration().getCommitsToExecute() == 0 ? experiementFileRecords.size() : projectData.getConfiguration().getCommitsToExecute();
        int kthDataset = projectData.getConfiguration().getPerformFeatureSelectionForEveryKDataset();

        if (projectData.getConfiguration().isPerformFeatureSelection()) {
            Utilities.deleteFile(projectFeatureSelectionFile);
            Utilities.deleteFile(projectFeatureSelectionSummaryFile);
            Utilities.createOutputDirectory(String.format("%s/FeatureSelection", projectData.getConfiguration().getAnalysisDirectory()), false);

            boolean printFeatureSelectionHeader = true;
            try (ProgressBar pb = new ProgressBar("Feature Selection:", commitsToExecute)) {
                for (int k=0;k< experiementFileRecords.size();k+=kthDataset) {

                    if (k > commitsToExecute) {
                        break;
                    }
                    pb.stepTo(k);
                    String dataset = experiementFileRecords.get(k).getArffFile().getAbsolutePath();
                    String xmlFile = experiementFileRecords.get(k).getFlatXMLFile().getAbsolutePath();
                    pb.setExtraMessage(StringUtils.isBlank(experiementFileRecords.get(k).getCommit()) ? "commits" : experiementFileRecords.get(k).getCommit());

                    //&begin [ML::Experiment::FeatureSelection]
                    if (projectData.getConfiguration().isPerformFeatureSelection()) {
                        //delete file if exists


                        PrintWriter printWriter = new PrintWriter(new FileWriter(projectFeatureSelectionFile, true));
                        try {
                            if (printFeatureSelectionHeader) {
                                printWriter.println("commit;dataset;project;attribute;rank;weight;evalmethod;evalmethodParent;isBalanced;isNormalized;fragmentThreshold");
                                printFeatureSelectionHeader = false;
                            }
                            int finalCount = count;
                            List<FeatureSelectionResult> featureSelectionResults = featureSelector.performSelection(dataset, xmlFile);
                            allFeatureSelectionResults.addAll(featureSelectionResults);
                            featureSelectionResults.parallelStream().forEach(result -> {
                                printFeatureSelectionRecord(printWriter, finalCount, dataset, projectName, result, balanced, normalized, fragmentThreshold);
                            });


                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            printWriter.close();
                        }

                    }
                }
            }
            //print all feature selection results
            printAllFeatureSelectionResults(projectName, projectFeatureSelectionSummaryFile, balanced, normalized, fragmentThreshold, allFeatureSelectionResults);
            //&end [ML::Experiment::FeatureSelection]
        } else {

            //Get ranking of features based on selected criteria
            int[] featureIndices = getFeatureIndices(projectFeatureSelectionSummaryFile);
            count = 0;

            int startingCommit =projectData.getConfiguration().getExperimentStartingCommit();
            List<ExperiementFileRecord> fileRecords = experiementFileRecords.subList(startingCommit,experiementFileRecords.size()-1);

            for (ExperiementFileRecord experiementFileRecord : fileRecords) {
                count++;
                if (count > commitsToExecute) {
                    break;
                }
                datasetFile = experiementFileRecord.getArffFile().getAbsolutePath();
                testDataSetFile = evaluationMethod == EvaluationMethod.CV ? null : experiementFileRecord.getTestARFFFile().getAbsolutePath();
                predictionsFile = experiementFileRecord.getPredictionFile() != null ? experiementFileRecord.getPredictionFile().getAbsolutePath() : null;
                xmlFlatLabelsFile = experiementFileRecord.getFlatXMLFile().getAbsolutePath();
                xmlMainLabelsFile = xmlFlatLabelsFile; //experiementFileRecord.getXmlFile().getAbsolutePath();

                String nFeatures = String.format("%df", (int) projectData.getConfiguration().getTopNFeatures());


                //run predictions
                String predictionsOutputFile = outputDirectory + "/" + projectName + "_" + abstractionLevel.toLowerCase() + "_predictions_" + balanced + "_" + normalized + "_" + fragmentThreshold + "_" + nFeatures + "_" + FilenameUtils.removeExtension(FilenameUtils.getName(datasetFile)) + ".csv";
                String predictionResultsSummaryOutputFile = outputDirectory + "/" + projectName + "_" + abstractionLevel.toLowerCase() + "_ps_" + balanced + "_" + normalized + "_" + fragmentThreshold + "_" + nFeatures + ".csv";// + FilenameUtils.removeExtension(FilenameUtils.getName(datasetFile)) + ".csv";
                predictionSummaryFile = predictionResultsSummaryOutputFile;
                String outputFile = outputDirectory + "/" + projectName + "_" + abstractionLevel.toLowerCase() + "_cv_" + balanced + "_" + normalized + "_" + fragmentThreshold + "_" + nFeatures + ".csv";// + FilenameUtils.removeExtension(FilenameUtils.getName(datasetFile)) + ".csv";
                crossValidationSummaryFile = outputFile;
                File instanceNameFile = experiementFileRecord.getInstanceNameFile();
                File assetMappingsFile = experiementFileRecord.getAssetMappingsFile();
                //check if number of instances is less than kFolds value
                System.out.println("SELCTED FEATURES");
//                Arrays.stream(featureIndices).forEach(a->{System.out.printf("%d ",a);});
//                System.out.println();
                MultiLabelInstances dataset = new MultiLabelInstances(datasetFile, xmlMainLabelsFile);
                //reduce dataset
                if (topNfeatures < features.size()) {
                    dataset =  FeatureSelector.buildReducedMultiLabelDataset(featureIndices, dataset);
                }
                int numOfInstances = dataset.getNumInstances();

                dataset.getFeatureAttributes().stream().forEach(a->{System.out.printf("%s ",a.name());});
                System.out.println();
//                Arrays.stream(dataset.getFeatureIndices()).forEach(a->{System.out.printf("%d ",a);});
//                System.out.println();
                //System.out.println("NUM OF INSTANCES: " + numOfInstances);
                if (numOfInstances < kFolds) {
                    kFolds = numOfInstances;
                } else {
                    kFolds = tempKFolds;
                }

                //get predictions file
                Instances unlabledData = null;
                if (predictionsFile != null) {
                    FileReader reader = new FileReader(predictionsFile);
                    unlabledData = new Instances(reader);
                }

                //reduced features dataset
                MultiLabelInstances testDataset = null;
                if (experiementFileRecord.getPredictionsXMLFile() != null) {
                    testDataset = new MultiLabelInstances(unlabledData, experiementFileRecord.getPredictionsXMLFile().getAbsolutePath());
                    testDataset =  FeatureSelector.buildReducedMultiLabelDataset(featureIndices, testDataset);
                }


                MulanAlgorithmRunner runner = new MulanAlgorithmRunner(dataset, datasetFile, testDataSetFile, xmlFlatLabelsFile, outputFile, predictionsOutputFile, instanceNameFile, assetMappingsFile, kFolds, nIterations, xmlFlatLabelsFile, testDataset, projectData.getConfiguration());
                runner.printHeader = printHeader;
                runner.printPredictionSummaryHeader = printPredictionSummaryHeader;
                runner.setPredictionResultSummaryOutputFile(predictionResultsSummaryOutputFile);

                //if(count==experiementFileRecords.size()-1||count==experiementFileRecords.size()-2){//only run cross validation for the last two commits
                if (projectData.getConfiguration().isRunCrossValidation()) {
                    runner.execute();
                }
                //}


                if (projectData.getConfiguration().isRunPredictions() && predictionsFile != null) {
                    runner.executePredictions();
                    predictionsHeaderPrinted = true;
                }

                if (printHeader == true) {
                    printHeader = false;
                }
                if (predictionsHeaderPrinted) {
                    printPredictionSummaryHeader = false;
                }


            }

            //copy sumary file
            if (projectData.getConfiguration().isCopyExpResultsToRFolder()) {
                if (projectData.getConfiguration().isRunPredictions()) {
                    File psFile = new File(predictionSummaryFile);
                    if(psFile.exists()){
                        FileUtils.copyToDirectory(psFile, projectData.getConfiguration().getrDataFolder());//copy output to the Rfolder
                    }

                }

                if (projectData.getConfiguration().isRunCrossValidation()) {
                    File cvFile = new File(crossValidationSummaryFile);
                    if(cvFile.exists()){
                        FileUtils.copyToDirectory(cvFile, projectData.getConfiguration().getrDataFolder());//copy output to the Rfolder
                    }

                }

            }
        }


    }

    private int[] getFeatureIndices(String featureSelectionSummaryFile) throws IOException {
        File summaryFile = new File(featureSelectionSummaryFile);
        double topNfeatures = projectData.getConfiguration().getTopNFeatures();
        //if summary file is missing, use all features
        List<String> features = projectData.getMLFeatures();
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
            List<FeatureSelectionResultSummary> featureSelectionResultSummaries = new ArrayList<>();
            List<String> headerParts = Arrays.asList(lines.get(0).split(";"));
            for (int i = 1; i < lines.size(); i++) {
                String[] parts = lines.get(i).split(";");
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
            List<FeatureSelectionResultSummary> filteredResults = featureSelectionResultSummaries.stream().filter(r -> r.getProject().equalsIgnoreCase(projectData.getConfiguration().getProjectShortName()) &&
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


    private void runMEKAExperiment() throws IOException {
        String tmpDir = String.format("%s/%s/MEKA", projectData.getConfiguration().getAnalysisDirectory(), "ExperimentData");// System.getProperty("java.io.tmpdir");
        String abstractionLevel = projectData.getConfiguration().getCodeAbstractionLevel().toString();
        Utilities.createOutputDirectory(tmpDir, true);
        System.out.println("Using dir: " + tmpDir);

        Experiment exp = new DefaultExperiment();
        // classifiers
        exp.setClassifiers(new MultiLabelClassifier[]{
                //Binary Relevance Methods
//                new BR(),
//                new CC(),
//                new CT(),
//                new PCC(),
//                new MCC(),
//                new BCC(),
//                new PMCC(),
                //Label Powerset Methods
//                new MajorityLabelset()
                //new PSt()
                //new LC()
                new RAkEL()


        });
        // datasets
        LocalDatasetProvider dp1 = new LocalDatasetProvider();
        dp1.setDatasets(getARFFFiles().toArray(new File[getARFFFiles().size()]));
//       LocalDatasetProvider dp2 = new LocalDatasetProvider();
//       dp2.setDatasets(new File[]{
//               new File("C:/ExperimentData/FILE/dataFiles/1bda80fa7409d6c0369990c313d38e858e10823e/1bda80fa7409d6c0369990c313d38e858e10823e-FILE.arff"),
//       });
//       MultiDatasetProvider mdp = new MultiDatasetProvider();
//       mdp.setProviders(new DatasetProvider[]{dp1, dp2});
        exp.setDatasetProvider(dp1);
        // output of metrics
        KeyValuePairs sh = new KeyValuePairs();
        sh.setFile(new File(tmpDir + "/" + abstractionLevel + "-mekaexp.txt"));
        exp.setStatisticsHandler(sh);
        // evaluation
        RepeatedRuns eval = new RepeatedRuns();
        eval.setEvaluator(new CrossValidation());
        exp.setEvaluator(eval);
        // stage
        exp.addExecutionStageListener(new ExecutionStageListener() {
            @Override
            public void experimentStage(ExecutionStageEvent e) {
                System.err.println("[STAGE] " + e.getStage());
            }
        });
        // iterations
        exp.addIterationNotificationListener(new IterationNotificationListener() {
            @Override
            public void nextIteration(IterationNotificationEvent e) {
                System.err.println("[ITERATION] " + Utils.toCommandLine(e.getClassifier()) + " --> " + e.getDataset().relationName());
            }
        });
        // statistics
        exp.addStatisticsNotificationListener(new StatisticsNotificationListener() {
            @Override
            public void statisticsAvailable(StatisticsNotificationEvent e) {
                //System.err.println("[STATISTICS] #" + e.getStatistics().size());
            }
        });
        // log events
        exp.addLogListener(new LogListener() {
            @Override
            public void logMessage(LogEvent e) {
                // System.err.println("[LOG] " + e.getSource().getClass().getName() + ": " + e.getMessage());
            }
        });
        // output options
        System.out.println("Setup:\n" + OptionUtils.toCommandLine(exp) + "\n");
        // execute
        String msg = exp.initialize();
        System.out.println("initialize: " + msg);
        if (msg != null)
            return;
        msg = exp.run();
        System.out.println("run: " + msg);
        msg = exp.finish();
        System.out.println("finish: " + msg);
        // export them
        TabSeparated tabsepAgg = new TabSeparated();
        tabsepAgg.setFile(new File(tmpDir + "/" + abstractionLevel + "-mekaexp-agg.tsv"));
        SimpleAggregate aggregate = new SimpleAggregate();
        aggregate.setSuffixMean("");
        aggregate.setSuffixStdDev(" (stdev)");
        aggregate.setSkipCount(true);
        aggregate.setSkipMean(false);
        aggregate.setSkipStdDev(false);
        aggregate.setExporter(tabsepAgg);
        TabSeparated tabsepFull = new TabSeparated();
        tabsepFull.setFile(new File(tmpDir + "/" + abstractionLevel + "-mekaexp-full.tsv"));
        TabSeparatedMeasurement tabsepHL = new TabSeparatedMeasurement();
        tabsepHL.setMeasurement("Hamming loss");
        tabsepHL.setFile(new File(tmpDir + "/" + abstractionLevel + "-mekaexp-HL.tsv"));
        TabSeparatedMeasurement tabsepZOL = new TabSeparatedMeasurement();
        tabsepZOL.setMeasurement("ZeroOne loss");
        tabsepZOL.setFile(new File(tmpDir + "/" + abstractionLevel + "-mekaexp-ZOL.tsv"));
        MultiExporter multiexp = new MultiExporter();
        multiexp.setExporters(new EvaluationStatisticsExporter[]{aggregate, tabsepFull, tabsepHL, tabsepZOL});
        multiexp.addLogListener(new LogListener() {
            @Override
            public void logMessage(LogEvent e) {
                System.err.println("[EXPORT] " + e.getSource().getClass().getName() + ": " + e.getMessage());
            }
        });
        System.out.println(OptionUtils.toCommandLine(multiexp));
        msg = multiexp.export(exp.getStatistics());
        System.out.println("export: " + msg);
    }


    private void setARFF(File folder, List<File> arffFiles) {

        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                setARFF(file, arffFiles);
            } else if (Utilities.isARFFFile(file)) {
                arffFiles.add(file);
            }
        }

    }

    private List<File> getARFFFiles() {
        List<File> arffFiles = null;
        if (projectData.getConfiguration().getExecutionMethod().equalsIgnoreCase("E")) {
            arffFiles = new ArrayList<>();
            setARFF(new File(projectData.getConfiguration().getExperimentDataFolder()), arffFiles);
            Collections.sort(arffFiles, new FileComparator());
        } else {
            arffFiles = projectData.getExperiementFileRecords().stream().map(ExperiementFileRecord::getArffFile).collect(Collectors.toList());
        }


        return arffFiles;
    }

    private void setExperiementFileRecord(File folder, List<ExperiementFileRecord> experiementFileRecords) {
        ExperiementFileRecord experiementFileRecord = null;
        File experiementFolder = new File(projectData.getConfiguration().getDataFilesSubDirectory());
        if (!folder.getAbsolutePath().equalsIgnoreCase(experiementFolder.getAbsolutePath())) {
            experiementFileRecord = new ExperiementFileRecord();
        }
        boolean isUseDataBalancer = projectData.getConfiguration().isUseDataBalancer();
        //we assume that each commit has a separate folder with data files: arff and xml
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                setExperiementFileRecord(file, experiementFileRecords);
            } else {
                if (experiementFileRecord == null) {
                    continue;
                }

                if (Utilities.isXMLFile(file) && file.getName().contains("P-XML")) {
                    experiementFileRecord.setPredictionsXMLFile(file);
                } else if (Utilities.isXMLFile(file) && file.getName().contains("FLAT")) {
                    experiementFileRecord.setFlatXMLFile(file);
                } else if (Utilities.isXMLFile(file)) {
                    experiementFileRecord.setXmlFile(file);
                } else if (Utilities.isARFFFile(file) && file.getName().contains("TEST")) {
                    experiementFileRecord.setTestARFFFile(file);
                } else if (Utilities.isARFFFile(file) && file.getName().contains("PREDICT")) {
                    experiementFileRecord.setPredictionFile(file);
                } else if (Utilities.isARFFFile(file)) {
                    experiementFileRecord.setArffFile(file);
                } else if (Utilities.isDataFile(file) && file.getName().contains("ULDMAP")) {//for asset-feature mappings for known unlabled data
                    experiementFileRecord.setAssetMappingsFile(file);
                } else if (Utilities.isDataFile(file)) {//for data files with instance names
                    experiementFileRecord.setInstanceNameFile(file);
                }

            }
        }
        if (experiementFileRecord != null) {
            experiementFileRecord.setCommitNumber(experiementFileRecord.getArffFile().getName());
            experiementFileRecords.add(experiementFileRecord);
        }


    }

    private List<ExperiementFileRecord> getExperiementFileRecords() {
        List<ExperiementFileRecord> experiementFileRecords = null;
        if (projectData.getConfiguration().getExecutionMethod().equalsIgnoreCase("E")) {
            experiementFileRecords = new ArrayList<>();
            String experimentFolder = projectData.getConfiguration().getDataFilesSubDirectory();
            setExperiementFileRecord(new File(experimentFolder), experiementFileRecords);
            Collections.sort(experiementFileRecords, new ExperiementFileRecordComparator());
        } else {
            experiementFileRecords = projectData.getExperiementFileRecords();
        }


        return experiementFileRecords;
    }

}
