package se.gu.defectpred;

import com.google.common.base.Stopwatch;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import se.gu.main.Configuration;
import se.gu.utils.LocalExecutionRunner;
import se.gu.utils.Utilities;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;

import java.io.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CrossProjectPrediction {
    private Configuration configuration;
    private String[] projectShortNames;
    private String crossProjectCommitFolder, crossProjectReleaseFolder, projectCombinationsFile;
    private File commitResultsFolder;

    public CrossProjectPrediction(Configuration configuration) throws IOException {
        this.configuration = configuration;
        this.projectShortNames = configuration.getProjectShortNames().split(",");
        this.crossProjectCommitFolder = configuration.getCrossProjectCommitFolder();
        this.crossProjectReleaseFolder = configuration.getCrossProjectReleaseFolder();
        this.projectCombinationsFile = configuration.getProjectCombinationsFile();
        commitResultsFolder = Utilities.createOutputDirectory(String.format("%s/results", new File(crossProjectCommitFolder).getParent()), false);
    }

    public void createCommitLevelDataSets(boolean isCommitBased)  {
        PrintWriter imregWriter=null,imnormWriter=null,bregWriter=null,bnormWriter=null;
        try {
            //create files
            String header = "ratio;train;test;ROC;TimeElapsed";
            File imreg = new File(String.format("%s/xp_%s_imbalanced_regular.csv", commitResultsFolder,isCommitBased?"commits":"releases"));
            Utilities.deleteFile(imreg.getAbsolutePath());

             imregWriter = new PrintWriter(new FileWriter(imreg, true));
            imregWriter.println(header);

            File imnorm = new File(String.format("%s/xp_%s_imbalanced_normalized.csv", commitResultsFolder,isCommitBased?"commits":"releases"));
            Utilities.deleteFile(imnorm.getAbsolutePath());
             imnormWriter = new PrintWriter(new FileWriter(imnorm, true));
            imnormWriter.println(header);

            File breg = new File(String.format("%s/xp_%s_balanced_regular.csv", commitResultsFolder,isCommitBased?"commits":"releases"));
            Utilities.deleteFile(breg.getAbsolutePath());
             bregWriter = new PrintWriter(new FileWriter(breg, true));
            bregWriter.println(header);
            bregWriter.close();

            File bnorm = new File(String.format("%s/xp_%s_balanced_normalized.csv", commitResultsFolder,isCommitBased?"commits":"releases"));
            Utilities.deleteFile(bnorm.getAbsolutePath());
             bnormWriter = new PrintWriter(new FileWriter(bnorm, true));
            bnormWriter.println(header);

            LocalExecutionRunner executionRunner = new LocalExecutionRunner();
            int maxFutures = configuration.getNumberOfThreads();
            ResultsToFileWriter resultsToFileWriter = new ResultsToFileWriter();
            //read project combinations
            List<String> fileLines = FileUtils.readLines(new File(projectCombinationsFile), configuration.getTextEncoding());
            int futureCount=0;
            for (String line : fileLines) {
               if(futureCount>maxFutures){
                   executionRunner.waitForTaskToFinish();
                   futureCount=0;
               }else {
                   CrossPredictionHandler crossPredictionHandler = new CrossPredictionHandler(line, resultsToFileWriter, breg, "breg", false, true, isCommitBased, crossProjectCommitFolder, crossProjectReleaseFolder, projectCombinationsFile);
                   Utilities.createFuture(crossPredictionHandler, executionRunner);
                   futureCount++;
               }
                //classify(imregWriter, ratio, train, trainFile, testProjects, trainingFile, false, false, "imreg", isCommitBased);
                //classify(imnormWriter, ratio,train,trainFile,testProjects,trainingFile, true, false, "imnorm", isCommitBased);
                //classify(bregWriter, line, false, true, "breg", isCommitBased);
                //classify(bnormWriter, ratio,train,trainFile,testProjects,trainingFile, true, true, "bnorm", isCommitBased);

            }
            executionRunner.waitForTaskToFinish();
            executionRunner.shutdown();
        }catch (Exception ex){

        }finally {
            if(imregWriter!=null){
                imregWriter.close();
            }
            if(imnormWriter!=null){
                imnormWriter.close();
            }
            if(bregWriter!=null){
                bregWriter.close();
            }
            if(bnormWriter!=null){
                bnormWriter.close();
            }




        }



    }

    public void classify(PrintWriter writer,String line, boolean normalize, boolean balance, String category, boolean isCommitBased) {
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
                        writer.printf("%s;%s;%s;%.5f;%s\n", ratio, trainSet, testProject, value, stopwatch.elapsed(TimeUnit.SECONDS));

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
