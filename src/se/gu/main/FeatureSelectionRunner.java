package se.gu.main;

import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.io.FileUtils;
import se.gu.assets.DataSetRecord;
import se.gu.data.DataController;
import se.gu.ml.experiment.FeatureSelectionResult;
import se.gu.ml.experiment.FeatureSelector;
import se.gu.utils.Utilities;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FeatureSelectionRunner {
    public FeatureSelectionRunner(ProjectData projectData) throws SQLException, ClassNotFoundException {
        this.projectData = projectData;
        dataController = new DataController(projectData.getConfiguration());
    }

    private ProjectData projectData;
    private DataController dataController;

    public void selectFeatures() throws Exception {

        File analysisDirectory = projectData.getConfiguration().getAnalysisDirectory();
        Utilities.createOutputDirectory(String.format("%s/FeatureSelection", analysisDirectory), false);


        //get all projects
        String[] projectNameList = projectData.getConfiguration().getProjectNamesList();
        String[] projectShortNames = projectData.getConfiguration().getProjectShortNameList();

        String[] assetTypes = projectData.getConfiguration().getAssetTypes();


        for (int i = 0; i < projectNameList.length; i++) {
            String projectName = projectNameList[i];
            //String projectShortName = projectShortNames[i];
            List<DataSetRecord> dataSetRecords = dataController.getAllDataSetsForProject(projectName);
            for (String assetType : assetTypes) {
                List<DataSetRecord> typeDataSetRecords = dataSetRecords.parallelStream().filter(d -> d.getAssetType().equalsIgnoreCase(assetType)).collect(Collectors.toList());
                performSelection(projectName, analysisDirectory.getAbsolutePath(), assetType, typeDataSetRecords);
            }
        }

        //File featureSelectionFileAllProjects = new File(String.format("%s/featureSelectionAllProjects.csv", projectData.getConfiguration().getrDataFolder()));


    }

    private void performSelection(String projectName, String analysisDirectory, String assetType, List<DataSetRecord> typeDataSetRecords) throws Exception {
        //&begin [ML::Experiment::FeatureSelection]
        //feature selection
        List<FeatureSelectionResult> allFeatureSelectionResults = new ArrayList<>();
        FeatureSelector featureSelector = new FeatureSelector(projectData);

        String balanced = projectData.getConfiguration().getBalancedDataSetName();
        String normalized = projectData.getConfiguration().getNormalizedDataSetName();
        String fragmentThreshold = projectData.getConfiguration().getFragmentThresholdDataSetName();
        String projectFeatureSelectionFile = String.format("%s/FeatureSelection/%s_%s.csv", analysisDirectory, projectName, assetType);

        String projectFeatureSelectionSummaryFile = String.format("%s/FeatureSelection/%s_%s_SUMMARY.csv", analysisDirectory, projectName, assetType);

        Utilities.deleteFile(projectFeatureSelectionFile);
        Utilities.deleteFile(projectFeatureSelectionSummaryFile);
        PrintWriter printWriter = new PrintWriter(new FileWriter(projectFeatureSelectionFile, true));
        printWriter.println("commitIndex;commit;dataset;project;level;attribute;rank;weight;evalmethod;evalmethodParent;isBalanced;isNormalized;fragmentThreshold");
        printWriter.close();
        int kthDataset = projectData.getConfiguration().getPerformFeatureSelectionForEveryKDataset();

        System.out.println(projectName + " " + assetType);

        for (int k = 0; k < typeDataSetRecords.size(); k += kthDataset) {
            PrintWriter pw = new PrintWriter(new FileWriter(projectFeatureSelectionFile, true));
            try {
                DataSetRecord dsr = typeDataSetRecords.get(k);
                List<FeatureSelectionResult> featureSelectionResults = featureSelector.performSelection(dsr.getTrainingFile(), dsr.getTrainingXMLFile());
                for (FeatureSelectionResult result : featureSelectionResults) {
                    allFeatureSelectionResults.add(result);
                    printFeatureSelectionRecord(pw, dsr.getCommitIdex(), dsr.getCommitHash(), dsr.getTrainingFile(), dsr.getProject(), dsr.getAssetType(), result, balanced, normalized, fragmentThreshold);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                pw.close();
            }
        }

        //print all feature selection results
        printAllFeatureSelectionResults(projectName, projectFeatureSelectionSummaryFile, balanced, normalized, fragmentThreshold, allFeatureSelectionResults);
        //&end [ML::Experiment::FeatureSelection]
    }

    private void printFeatureSelectionRecord(PrintWriter pw, int commitIndex, String commit, String dataSetName, String project, String level, FeatureSelectionResult result, String isBalanced, String isNormalized, String fragmentThreshold) {
        pw.printf("%d;%s;%s;%s;%s;%s;%d;%f;%s;%s;%s;%s;%s\n", commitIndex, commit, dataSetName, project, level, result.getAttribute(), result.getRank(), result.getWeight(), result.getEvalMethod(), result.getEvalMethodParent(), isBalanced, isNormalized, fragmentThreshold);
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

            List<String> attributes = projectData.getMLFeaturesNames();
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

    public void combineSUmmaryFiles() throws IOException {

        File analysisDirectory = projectData.getConfiguration().getAnalysisDirectory();
        String[]projectNameList = projectData.getConfiguration().getProjectNamesList();

        String[] assetTypes = projectData.getConfiguration().getAssetTypes();
        for(String assetType:assetTypes) {
            File typeCombinedFile = new File(String.format("%s/FeatureSelection/allprograms_featureSelection_%s.csv",analysisDirectory, assetType));
            Utilities.deleteFile(typeCombinedFile.getAbsolutePath());
            PrintWriter writer = new PrintWriter(new FileWriter(typeCombinedFile,true));
            writer.println("index;attribute;project;is_balanced;is_normalized;fragmentThreshold;RF_BR_rank;RF_BR_weight;RF_LP_rank;RF_LP_weight;RF_rank;RF_weight;overall_rank;overall_weight");
            writer.close();

            for (int i = 0; i < projectNameList.length; i++) {
                PrintWriter pw = new PrintWriter(new FileWriter(typeCombinedFile,true));
                try {
                    String projectName = projectNameList[i];
                    File projectFeatureSelectionSummaryFile = new File(String.format("%s/FeatureSelection/%s_%s_SUMMARY.csv", analysisDirectory, projectName, assetType));
                    List<String> lines = FileUtils.readLines(projectFeatureSelectionSummaryFile, projectData.getConfiguration().getTextEncoding());
                    for(int j=1;j<lines.size();j++){//skip header
                        pw.println(lines.get(j));
                    }
                }catch (Exception ex){
                    ex.printStackTrace();
                }finally {
                    pw.close();
                }

            }
        }

    }

}
