package se.gu.defectpred;

import com.google.common.base.Stopwatch;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.lang3.StringUtils;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.concurrent.TimeUnit;

public class CrossPredictionHandler implements Runnable {
    public CrossPredictionHandler(String line, ResultsToFileWriter writer, File resultsFile, String category, boolean normalize, boolean balance, boolean isCommitBased, String crossProjectCommitFolder, String crossProjectReleaseFolder, String projectCombinationsFile) {
        this.writer = writer;
        this.resultsFile = resultsFile;
        this.line = line;
        this.category = category;
        this.normalize = normalize;
        this.balance = balance;
        this.isCommitBased = isCommitBased;
        this.crossProjectCommitFolder = crossProjectCommitFolder;
        this.crossProjectReleaseFolder = crossProjectReleaseFolder;
        this.projectCombinationsFile = projectCombinationsFile;
    }

    private ResultsToFileWriter writer;
    private String line;
    private File resultsFile;
    private String  category;
    private boolean normalize, balance, isCommitBased;
    private String crossProjectCommitFolder, crossProjectReleaseFolder, projectCombinationsFile;

    public CrossPredictionHandler() {
    }

    @Override
    public void run() {
        try {
            if(StringUtils.isBlank(line)){
                return;
            }
            String[] items = line.split(";");
            String ratio = items[0];
            String trainSet = items[1];
            String testProjects = items[2];
            String shortTrainFileName = items[3];
            if (!shortTrainFileName.contains(".arff")) {
                return;
            }
            String trainingFile = String.format("%s/%s", isCommitBased ? crossProjectCommitFolder : crossProjectReleaseFolder, shortTrainFileName);
            File tFile = new File (trainingFile);
            if(!tFile.exists()){
                return;
            }
            String[] testProjectList = testProjects.split(",");
            int listSize = testProjectList.length;
            RandomForest randomForest = new RandomForest();


            //training
            Instances trainingData = DPUtilities.getTrainingDataset(trainingFile, normalize, balance);

            //build
            randomForest.buildClassifier(trainingData);
            Evaluation eval = new Evaluation(trainingData);

            try (ProgressBar pb = new ProgressBar(String.format("%s-%s", shortTrainFileName, category), listSize)) {
                for (String testProject : testProjectList) {
                    pb.step();

                    String testFile = String.format("%s/%s.arff", isCommitBased ? crossProjectCommitFolder : crossProjectReleaseFolder, testProject);

                    try {
                        Stopwatch stopwatch = Stopwatch.createStarted();


                        //test
                        Instances testData = new Instances(new BufferedReader(new FileReader(testFile)));
                        testData.setClassIndex(testData.numAttributes() - 1);

                        eval.evaluateModel(randomForest, testData);
                        stopwatch.stop();
                        double value = eval.areaUnderROC(0);
                        if(Double.isNaN(value)){
                            continue;
                        }
                        writer.writeResult(String.format("%s;%s;%s;%.5f;%s", ratio, trainSet, testProject, value, stopwatch.elapsed(TimeUnit.SECONDS)),resultsFile);

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }


                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
