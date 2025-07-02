package se.gu.main;

import com.google.common.math.Quantiles;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.io.FileUtils;
import se.gu.data.DataController;
import se.gu.main.Configuration;
import se.gu.ml.experiment.PredictionResult;
import se.gu.utils.Utilities;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ResultsUpdater {
    private DataController dataController;
    private Configuration configuration;
    private String[] levels, projects, projectShortNameList;
    String classifier;
    String parentFolder, resultsFolder;

    public ResultsUpdater(Configuration configuration) throws SQLException, ClassNotFoundException {
        this.configuration = configuration;
        this.dataController = new DataController(configuration);
        levels = configuration.getAssetLevels();
        classifier = configuration.getClassifierForCombinedResults();
        resultsFolder = parentFolder = configuration.getrDataFolder().getAbsolutePath();
        projects = configuration.getProjectNamesList();
        projectShortNameList = configuration.getProjectShortNameList();
    }

    /**
     * Generates mapping between commit metrics and prediction results
     * This should be executed only after @updateResulsFromCSVs is called
     */
    public void generatePracticePredictionResultsMapping() throws IOException, SQLException {

        String resultsFolder = String.format("%s/commitmetrics/", configuration.getrDataFolder().getAbsolutePath());
        Utilities.createOutputDirectory(resultsFolder, false);

        int metric = 11;//(int)configuration.getTopNFeatures();
        //generate mapping for all projects
        ResultSet rs = dataController.getCommitMetricsResultsMappigAll();
        String mappingsFile = String.format("%s/practices_allprojects_all_mappings.csv", resultsFolder);
        printMappings(rs, mappingsFile);
        System.out.println(mappingsFile);
        //generate for all projects per level
        for (String level : levels) {
            mappingsFile = String.format("%s/practices_allprojects_%s_mappings.csv", resultsFolder, level);
            rs = dataController.getCommitMetricsResultsMappigByLevel(level);
            printMappings(rs, mappingsFile);
            System.out.println(mappingsFile);

        }
        //generate for each project and level
        for (String project : projects) {
            for (String level : levels) {
                mappingsFile = String.format("%s/practices_%s_%s_mappings.csv", resultsFolder, project, level);
                rs = dataController.getCommitMetricsResultsMappigByProjectLevel(project, level);
                printMappings(rs, mappingsFile);
                System.out.println(mappingsFile);
            }
        }


    }

    private void printMappings(ResultSet rs, String mappingsFile) throws IOException, SQLException {
        File allMappingsFile = new File(mappingsFile);
        String header = "tLA;tLR;aLA;aLR;mLA;mLR;tFC;tFA;tFM;tFD;tFRe;tH;aH;mHS;aHS;tCh;aCh;tAA;tUA;rAA;rUA;tAFo;tAFi;tAFra;tALoc;rAFo;rAFi;rAFra;rALoc;project;commit;commitIndex;level;metrics;classifier;precision;recall;fscore";
        if (allMappingsFile.exists()) {
            FileUtils.forceDelete(allMappingsFile);
        }
        PrintWriter writer = new PrintWriter(new FileWriter(allMappingsFile, true));
        writer.println(header);
        while (rs.next()) {
            writer.printf("%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s\n",
                    rs.getString("tLA"), rs.getString("tLR"), rs.getString("aLA"), rs.getString("aLR"), rs.getString("mLA"), rs.getString("mLR"), rs.getString("tFC"), rs.getString("tFA"), rs.getString("tFM"), rs.getString("tFD"), rs.getString("tFRe"), rs.getString("tH"), rs.getString("aH"), rs.getString("mHS"), rs.getString("aHS"), rs.getString("tCh"), rs.getString("aCh"), rs.getString("tAA"), rs.getString("tUA"), rs.getString("rAA"), rs.getString("rUA"), rs.getString("tAFo"), rs.getString("tAFi"), rs.getString("tAFra"), rs.getString("tALoc"), rs.getString("rAFo"), rs.getString("rAFi"), rs.getString("rAFra"), rs.getString("rALoc"), rs.getString("project"), rs.getString("commit"), rs.getInt("commitIndex"), rs.getString("level"), rs.getInt("metrics"), rs.getString("classifier"), rs.getString("precision"), rs.getString("recall"), rs.getString("fscore"));

        }
        writer.close();
    }

    public void updateResulsFromCSVs() throws IOException, SQLException {
        //delete existin results
        dataController.deleteAllResults();

        int[] metrics = new int[]{11, 8, 6, 4};
        for (String project : projects) {
            for (String level : levels) {
                for (int metric : metrics) {
                    File resultsFile = new File(String.format("%s/%s_%s_combinedmeasures_ps_im_reg_diff_%d_ALLAssets.csv", resultsFolder, project, level, metric));
                    if (resultsFile.exists()) {

                        //insert
                        List<String> lines = FileUtils.readLines(resultsFile, configuration.getTextEncoding());
                        int size = lines.size();
                        System.out.println(resultsFile);
                        try (ProgressBar pb = new ProgressBar("Inserting results:", size)) {
                            for (int i = 1; i < size; i++) {
                                //commitIndex;commit;classifier;measure;measureValue
                                pb.step();
                                String[] items = lines.get(i).split(";");
                                int commitIndex = Integer.parseInt(items[0]);
                                String commitHash = items[1];
                                String classifier = items[2];
                                String measure = items[3];
                                double measureValue = Double.parseDouble(items[4]);
                                dataController.resultsSummaryInsert(project, commitHash, commitIndex, level, metric, classifier, measure, measureValue);
                            }
                        }
                    }
                }
            }
        }

    }

    public void updatePSResulsFromCSVs() throws IOException, SQLException {
        //delete existin results
        dataController.deleteAllPSResults();

        int[] metrics = new int[]{11, 8, 6, 4};
        for (String project : projects) {
            for (String level : levels) {
                for (int metric : metrics) {
                    File resultsFile = new File(String.format("%s/%s_%s_ps_im_reg_diff_%d_ALLAssets.csv", resultsFolder, project, level, metric));
                    if (resultsFile.exists()) {

                        //insert
                        List<String> lines = FileUtils.readLines(resultsFile, configuration.getTextEncoding());
                        int size = lines.size();
                        System.out.println(resultsFile);
                        try (ProgressBar pb = new ProgressBar("Inserting results:", size)) {
                            for (int i = 1; i < size; i++) {
                                //commitIndex;commit;DataSet;Classifier;TotalInstances;InstancesWithRankedLabels;PercentageOfInstancesWithRankedLabels;InstancesWithPrecisionNull;PercentageOfInstancesWithPrecisionNull;InstancesWithPrecisionZero;PercentageOfInstancesWithPrecisionZero;InstancesWithPrecisionNonZero;PercentageOfInstancesWithPrecisionNonZero;InstancesWithPrecisionOne;PercentageOfInstancesWithPrecisionOne;InstancesWithRecallZero;PercentageOfInstancesWithRecallZero;InstancesWithRecallOne;PercentageOfInstancesWithRecallOne;AveragePrecisionForAllNonNullPrecision;AveragePrecisionForNonZeroPrecision;AverageRecallForAllNonNullPrecision;AverageRecallForAllNonZeroPrecision;AverageFScoreForAllNonNullPrecision;AverageFScoreForAllNonZeroPrecision;
                                pb.step();
                                String[] items = lines.get(i).split(";");
                                int commitIndex = Integer.parseInt(items[0]);
                                String commitHash = items[1];
                                String dataset = items[2];
                                String classifier = items[3];
                                double totalInstances = Double.parseDouble(items[4]);
                                double precision = Double.parseDouble(items[19]);
                                double recall = Double.parseDouble(items[21]);
                                double fscore = Double.parseDouble(items[23]);
                                dataController.psresultsInsert(commitIndex, commitHash, dataset, classifier, totalInstances, precision, recall, fscore, project, level, metric);
                            }
                        }
                    }
                }
            }
        }

    }

    public void createCombinedProjectResults() {
        String[] projectShortNameList = configuration.getProjectShortNameList();


        for (String level : levels) {
            try {
                String outputFile = String.format("%s/allprojectscombinedMeasures_%s_%s.csv", parentFolder, level, classifier);
                File f = new File(outputFile);
                if (f.exists()) {
                    FileUtils.forceDelete(f);//remove existing output file
                }
                PrintWriter writer = null;
                try {
                    writer = new PrintWriter(new FileWriter(outputFile, true));
                    //write header
                    writer.println("commitIndex;commit;classifier;measure;measureValue;trainTime;testTime;project");

                    for (int p = 0; p < projects.length; p++) {
                        File dataFile = new File(String.format("%s/%s_%s_combinedmeasures_ps_im_reg_diff_11_ALLAssets.csv", parentFolder, projects[p], level));
                        if (!dataFile.exists()) {
                            continue;
                        }
                        System.out.println(dataFile);
                        List<String> lines = FileUtils.readLines(dataFile, "UTF-8");
                        //skip header
                        for (int i = 1; i < lines.size(); i++) {
                            String line = lines.get(i);
                            if (line.contains(classifier)) {
                                writer.printf("%s;%s\n", line, projectShortNameList[p]);
                            }
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    if (writer != null) {
                        writer.close();
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        //files
        //read file level

    }

    public void combineProjectResultsIntoOneFile() throws IOException {

        String outputFile = String.format("%s/allprojectscombinedMeasures_all.csv", parentFolder);
        File f = new File(outputFile);
        if (f.exists()) {
            FileUtils.forceDelete(f);//remove existing output file
        }
        PrintWriter writer = null;
        writer = new PrintWriter(new FileWriter(outputFile, true));
        //write header
        writer.println("commitIndex;commit;classifier;measure;measureValue;trainTime;testTime;project;level");
        for (String level : levels) {

            try {
                for (int p = 0; p < projects.length; p++) {
                    File dataFile = new File(String.format("%s/%s_%s_combinedmeasures_ps_im_reg_diff_11_ALLAssets.csv", parentFolder, projects[p], level));
                    if (!dataFile.exists()) {
                        continue;
                    }
                    System.out.println(dataFile);
                    List<String> lines = FileUtils.readLines(dataFile, "UTF-8");
                    //skip header
                    for (int i = 1; i < lines.size(); i++) {
                        writer.printf("%s;%s;%s\n", lines.get(i), projectShortNameList[p], level);

                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {

            }

        }
        if (writer != null) {
            writer.close();
        }

    }

    public void generateLatexResultsTable() throws IOException {

        String outputFile = String.format("%s/allprojectscombinedMeasures_all_LATEX.txt", parentFolder);
        List<String> classifiers = configuration.getPredictionModels();
        String[] meausres = new String[]{"precision", "recall", "fscore"};

        List<PredictionResult> results = new ArrayList<>();
        File f = new File(outputFile);
        if (f.exists()) {
            FileUtils.forceDelete(f);//remove existing output file
        }
        PrintWriter writer = null;
        writer = new PrintWriter(new FileWriter(outputFile, true));
        //write header
        String headerLine1 = "level;project;BR;;;;;;LP;;;;;;RAkELd;;;;;";
        for (int i = 0; i < classifiers.size(); i++) {

        }
        String headerLine2 = ";;F;;P;;R;;F;;P;;R;;F;;P;;R;";
        String headerLine3 = "level;project";
        String[] agg = new String[]{"Avg", "Med"};
        for (int i = 0; i < classifiers.size(); i++) {
            for (int j = 0; j < meausres.length; j++) {
                for (int k = 0; k < agg.length; k++) {
                    headerLine3 = String.format("%s;%s-%s-%s", headerLine3, classifiers.get(i), meausres[j], agg[k]);

                }
            }
        }
        //headerLine3 = ";;Avg;Med;Avg;Med;Avg;Med;Avg;Med;Avg;Med;Avg;Med;Avg;Med;Avg;Med;Avg;Med";
        writer.println(headerLine1);
        writer.println(headerLine2);
        writer.println(headerLine3);

        //first fill up the results
        fillUpResultsList(projectShortNameList, projects, parentFolder, levels, results);
        List<PredictionResult> predictionResults = new ArrayList<>();
        //now run through all levels and projects and fillup results table
        for (String level : levels) {

            for (String project : projectShortNameList) {//in the results table we use short project names

                for (int i = 0; i < classifiers.size(); i++) {
                    String classifier = classifiers.get(i);
                    for (int j = 0; j < meausres.length; j++) {
                        try {
                            String measure = meausres[j];
                            List<Double> measureValues = results.parallelStream().filter(r -> r.getClassifier().equalsIgnoreCase(classifier) && r.getMeasure().equalsIgnoreCase(measure)
                                    && r.getProject().equalsIgnoreCase(project) && r.getLevel().equalsIgnoreCase(level)).map(PredictionResult::getMeasureValue).collect(Collectors.toList());
                            double avg = measureValues.parallelStream().mapToDouble(Double::doubleValue).average().getAsDouble();
                            double median = Quantiles.median().compute(measureValues);
                            predictionResults.add(new PredictionResult(classifier, measure, project, level, avg, median));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }

        }

        //now print out the results summary
        for (String level : levels) {
            writer.print(level);//print the level
            for (String project : projectShortNameList) {//in the results table we use short project names
                if (predictionResults.parallelStream().filter(p -> p.getLevel().equalsIgnoreCase(level) &&
                        p.getProject().equalsIgnoreCase(project)).count() > 0) {
                    writer.print(";" + project);//print the project
                }
                for (int i = 0; i < classifiers.size(); i++) {
                    String classifier = classifiers.get(i);
                    for (int j = 0; j < meausres.length; j++) {
                        try {
                            String measure = meausres[j];
                            double avg = predictionResults.parallelStream().filter(p -> p.getLevel().equalsIgnoreCase(level) &&
                                    p.getProject().equalsIgnoreCase(project) && p.getClassifier().equalsIgnoreCase(classifier) &&
                                    p.getMeasure().equalsIgnoreCase(measure)).findFirst().get().getAvg();
                            double median = predictionResults.parallelStream().filter(p -> p.getLevel().equalsIgnoreCase(level) &&
                                    p.getProject().equalsIgnoreCase(project) && p.getClassifier().equalsIgnoreCase(classifier) &&
                                    p.getMeasure().equalsIgnoreCase(measure)).findFirst().get().getMed();
                            double maxAvgForMeasure = predictionResults.parallelStream().filter(p -> p.getLevel().equalsIgnoreCase(level) &&
                                    p.getProject().equalsIgnoreCase(project) && p.getMeasure().equalsIgnoreCase(measure)).map(PredictionResult::getAvg)
                                    .mapToDouble(Double::doubleValue).max().getAsDouble();
                            double maxMedForMeasure = predictionResults.parallelStream().filter(p -> p.getLevel().equalsIgnoreCase(level) &&
                                    p.getProject().equalsIgnoreCase(project) && p.getMeasure().equalsIgnoreCase(measure)).map(PredictionResult::getMed)
                                    .mapToDouble(Double::doubleValue).max().getAsDouble();

                            if (i == classifiers.size() - 1 && j == meausres.length - 1) {
                                if (avg == maxAvgForMeasure && maxAvgForMeasure > 0.0) {
                                    writer.printf(";\\textbf{%.2f}", avg);
                                } else {
                                    writer.printf(";%.2f", avg);
                                }

                                if (median == maxMedForMeasure && maxMedForMeasure > 0.0) {
                                    writer.printf(";\\textbf{%.2f}\\\\\n", median);
                                } else {
                                    writer.printf(";%.2f\\\\\n", median);
                                }

                            } else {
                                if (avg == maxAvgForMeasure && maxAvgForMeasure > 0.0) {
                                    writer.printf(";\\textbf{%.2f}", avg);
                                } else {
                                    writer.printf(";%.2f", avg);
                                }

                                if (median == maxMedForMeasure && maxMedForMeasure > 0.0) {
                                    writer.printf(";\\textbf{%.2f}", median);
                                } else {
                                    writer.printf(";%.2f", median);
                                }
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }

        }

        if (writer != null) {
            writer.close();
        }

    }

    private void fillUpResultsList(String[] projects, String[] projectFullNames, String parentFolder, String[] levels, List<PredictionResult> results) {
        for (String level : levels) {

            try {
                for (int p = 0; p < projectFullNames.length; p++) {
                    File dataFile = new File(String.format("%s/%s_%s_combinedmeasures_ps_im_reg_diff_11_ALLAssets.csv", parentFolder, projectFullNames[p], level));
                    System.out.println(dataFile);
                    List<String> lines = FileUtils.readLines(dataFile, "UTF-8");
                    //skip header
                    for (int i = 1; i < lines.size(); i++) {
                        results.add(new PredictionResult(lines.get(i), projects[p], level));

                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {

            }

        }
    }

    /**
     * This method generates relative commit numbers from the list of commits in each project
     */
    public void createRelativeCommits() {


        for (String level : levels) {
            PrintWriter writer = null;
            try {
                String file = String.format("%s/allprojectscombinedMeasures_%s_%s.csv", parentFolder, level, classifier);
                String fileNameWithoutExtension = file.replace(".csv", "").trim();
                File relativeCommitsFile = new File(String.format("%s_RELATIVECOMMITS.csv", fileNameWithoutExtension));
                if (relativeCommitsFile.exists()) {
                    FileUtils.forceDelete(relativeCommitsFile);
                }
                //read through all lines in file
                List<String> lines = FileUtils.readLines(new File(file), "UTF-8");
                writer = new PrintWriter(new FileWriter(relativeCommitsFile, true));
                writer.printf("rCommit;commitIndex;commit;classifier;measure;measureValue;project\n");//header: commitIndex;commit;classifier;measure;measureValue;project
                //create list of prediction results
                List<PredictionResult> results = new ArrayList<>();
                for (int i = 1; i < lines.size(); i++) {
                    results.add(new PredictionResult(lines.get(i)));
                }
                //get distnct projects from the results
                List<String> projects = results.parallelStream().map(PredictionResult::getProject).distinct().collect(Collectors.toList());
                for (String project : projects) {
                    //get distnct commit numbers
                    List<Integer> commits = results.parallelStream().filter(r -> r.getProject().equalsIgnoreCase(project)).map(PredictionResult::getCommitIndex).distinct().sorted().collect(Collectors.toList());
                    Map<Integer, Integer> rCommits = new HashMap<>();
                    double totalCommits = commits.size();
                    //assign relative comit number for each commit i.e. out 100% of commits, what number is each commit
                    for (int c = 0; c < commits.size(); c++) {
                        double rCommit = Math.round((((double) (c + 1)) / totalCommits) * 100.0);
                        rCommits.put(commits.get(c), (int) rCommit);
                    }
                    //now get all results for project and write them
                    List<PredictionResult> projectReslts = results.parallelStream().filter(r -> r.getProject().equalsIgnoreCase(project)).collect(Collectors.toList());
                    for (PredictionResult r : projectReslts) {
                        writer.printf("%d;%d;%s;%s;%s;%.3f;%s\n", rCommits.get(r.getCommitIndex()), r.getCommitIndex(), r.getCommit(), r.getClassifier(), r.getMeasure(), r.getMeasureValue(), r.getProject());
                    }
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }
        }
    }

    public void combineNTopMetricsFiles() throws IOException {
        String[] projectShortNameList = configuration.getProjectShortNameList();


        for (String level : levels) {
            try {
                String outputFile = String.format("%s/allprojectscombinedMeasures_%s_allNFeatures.csv", parentFolder, level);
                File f = new File(outputFile);
                if (f.exists()) {
                    FileUtils.forceDelete(f);//remove existing output file
                }
                PrintWriter writer = null;
                String[] nFeatures = new String[]{"11", "8", "6", "4"};
                try {
                    writer = new PrintWriter(new FileWriter(outputFile, true));
                    //write header
                    writer.println("commitIndex;commit;classifier;measure;measureValue;trainTime;testTime;project;nfeature");

                    for (int p = 0; p < projects.length; p++) {
                        for (String nFeature : nFeatures) {
                            File dataFile = new File(String.format("%s/%s_%s_combinedmeasures_ps_im_reg_diff_%s_ALLAssets.csv", parentFolder, projects[p], level, nFeature));
                            if (!dataFile.exists()) {
                                continue;
                            }
                            System.out.println(dataFile);
                            List<String> lines = FileUtils.readLines(dataFile, "UTF-8");
                            //skip header
                            for (int i = 1; i < lines.size(); i++) {
                                String line = lines.get(i);
                                writer.printf("%s;%s;%s\n", line, projectShortNameList[p],nFeature);

                            }
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    if (writer != null) {
                        writer.close();
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

    }
}
