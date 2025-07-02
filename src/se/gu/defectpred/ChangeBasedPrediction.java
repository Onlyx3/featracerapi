package se.gu.defectpred;

import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.io.FileUtils;
import se.gu.main.Configuration;
import se.gu.utils.LocalExecutionRunner;
import se.gu.utils.Utilities;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;
import weka.filters.Filter;
import weka.filters.supervised.instance.SMOTE;
import weka.filters.unsupervised.attribute.Normalize;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class ChangeBasedPrediction {
    private Configuration configuration;
    private String[] projectShortNames;
    String commitParentFolder, releaseParentFolder, commitFilesParentFolder;
    File commitResultsFolder;

    public ChangeBasedPrediction(Configuration configuration) throws IOException {
        this.configuration = configuration;
        projectShortNames = configuration.getProjectShortNames().split(",");
        commitParentFolder = configuration.getCommitDataSetParentFolder();
        releaseParentFolder = configuration.getReleaseDataSetParentFolder();
        commitFilesParentFolder = configuration.getCommitFilesParentFolder();
        commitResultsFolder = Utilities.createOutputDirectory(String.format("%s/results", new File(commitParentFolder).getParent()), false);
    }

    private void createTaskHandler(List<Integer> ids, List<String> hashes, PrintWriter writer, String project, String category, String commitParentFolder, String releaseParentFolder, boolean normalize, boolean balance, boolean isCommitBased, LocalExecutionRunner runner) {
        ClassificationTaskHandler taskHandler = new ClassificationTaskHandler(ids, hashes, writer, project, category, commitParentFolder, releaseParentFolder, normalize, balance, isCommitBased);
        createFuture(taskHandler, runner);
    }

    private void createFuture(Runnable taskHandler, LocalExecutionRunner runner) {
        runner.addFuture((Future<?>) runner.submit(taskHandler));
    }

    private void deleteFile(File file) throws IOException {
        if (file.exists()) {
            FileUtils.forceDelete(file);
        }
    }

    public void createCommitLevelDataSets() throws IOException {
        //create files
        String header = "id;project;commit;ROC;TimeElapsed";
        File imreg = new File(String.format("%s/commits_imbalanced_regular.csv", commitResultsFolder));
        deleteFile(imreg);
        PrintWriter imregWriter = new PrintWriter(new FileWriter(imreg, true));
        imregWriter.println(header);

        File imnorm = new File(String.format("%s/commits_imbalanced_normalized.csv", commitResultsFolder));
        deleteFile(imnorm);
        PrintWriter imnormWriter = new PrintWriter(new FileWriter(imnorm, true));
        imnormWriter.println(header);

        File breg = new File(String.format("%s/commits_balanced_regular.csv", commitResultsFolder));
        deleteFile(breg);
        PrintWriter bregWriter = new PrintWriter(new FileWriter(breg, true));
        bregWriter.println(header);

        File bnorm = new File(String.format("%s/commits_balanced_normalized.csv", commitResultsFolder));
        deleteFile(bnorm);
        PrintWriter bnormWriter = new PrintWriter(new FileWriter(bnorm, true));
        bnormWriter.println(header);


        for (String project : projectShortNames) {

            //get all commits and their ids
            List<String> fileLines = FileUtils.readLines(new File(String.format("%s/cc_commits_%s.csv", commitFilesParentFolder, project)), configuration.getTextEncoding());
            List<Integer> commitIds = fileLines.stream().map(l -> Integer.parseInt(l.split(";")[0])).collect(Collectors.toList());
            List<String> commitHashes = fileLines.stream().map(l -> l.split(";")[1]).collect(Collectors.toList());
            //imreg
            LocalExecutionRunner runner = new LocalExecutionRunner();
            createTaskHandler(commitIds, commitHashes, imregWriter, project, "imreg", commitParentFolder, releaseParentFolder, false, false, true, runner);
            createTaskHandler(commitIds, commitHashes, imnormWriter, project, "imnorm", commitParentFolder, releaseParentFolder, true, false, true, runner);
            createTaskHandler(commitIds, commitHashes, bregWriter, project, "breg", commitParentFolder, releaseParentFolder, false, true, true, runner);
            createTaskHandler(commitIds, commitHashes, bnormWriter, project, "bnorm", commitParentFolder, releaseParentFolder, true, true, true, runner);

//            classify(commitIds,commitHashes,imregWriter,project,false,false,"imreg",true);
//            classify(commitIds,commitHashes,imnormWriter,project,true,false,"imnorm",true);
//            classify(commitIds,commitHashes,bregWriter,project,false,true,"breg",true);
//            classify(commitIds,commitHashes,bnormWriter,project,true,true,"bnorm",true);
            runner.waitForTaskToFinish();
            runner.shutdown();
        }
        imregWriter.close();
        imnormWriter.close();
        bregWriter.close();
        bnormWriter.close();

    }

    public void createReleaseLevelDataSets() throws IOException {
        //create files
        String header = "id;project;release;ROC;TimeElapsed";
        File imreg = new File(String.format("%s/releases_imbalanced_regular.csv", commitResultsFolder));
        deleteFile(imreg);
        PrintWriter imregWriter = new PrintWriter(new FileWriter(imreg, true));
        imregWriter.println(header);

        File imnorm = new File(String.format("%s/releases_imbalanced_normalized.csv", commitResultsFolder));
        deleteFile(imnorm);
        PrintWriter imnormWriter = new PrintWriter(new FileWriter(imnorm, true));
        imnormWriter.println(header);

        File breg = new File(String.format("%s/releases_balanced_regular.csv", commitResultsFolder));
        deleteFile(breg);
        PrintWriter bregWriter = new PrintWriter(new FileWriter(breg, true));
        bregWriter.println(header);

        File bnorm = new File(String.format("%s/releases_balanced_normalized.csv", commitResultsFolder));
        deleteFile(bnorm);
        PrintWriter bnormWriter = new PrintWriter(new FileWriter(bnorm, true));
        bnormWriter.println(header);
        //create print writers

        for (String project : projectShortNames) {
            //get all commits and their ids
            List<String> fileLines = FileUtils.readLines(new File(String.format("%s/cc_releases_%s.csv", commitFilesParentFolder, project)), configuration.getTextEncoding());
            List<Integer> releaseIds = fileLines.stream().map(l -> Integer.parseInt(l.split(";")[0])).collect(Collectors.toList());
            List<String> releases = fileLines.stream().map(l -> l.split(";")[1]).collect(Collectors.toList());
            LocalExecutionRunner runner = new LocalExecutionRunner();
            createTaskHandler(releaseIds, releases, imregWriter, project, "imreg", commitParentFolder, releaseParentFolder, false, false, false, runner);
            createTaskHandler(releaseIds, releases, imnormWriter, project, "imnorm", commitParentFolder, releaseParentFolder, true, false, false, runner);
            createTaskHandler(releaseIds, releases, bregWriter, project, "breg", commitParentFolder, releaseParentFolder, false, true, false, runner);
            createTaskHandler(releaseIds, releases, bnormWriter, project, "bnorm", commitParentFolder, releaseParentFolder, true, true, false, runner);
            runner.waitForTaskToFinish();
            runner.shutdown();
//            classify(releaseIds, releases, imregWriter, project, false, false, "imreg", false);
//            classify(releaseIds, releases, imnormWriter, project, true, false, "imnorm", false);
//            classify(releaseIds, releases, bregWriter, project, false, true, "breg", false);
//            classify(releaseIds, releases, bnormWriter, project, true, true, "bnorm", false);
        }
        imregWriter.close();
        imnormWriter.close();
        bregWriter.close();
        bnormWriter.close();

    }



    public void classify(List<Integer> ids, List<String> hashes, PrintWriter writer, String project, boolean normalize, boolean balance, String category, boolean isCommitBased) {
        int listSize = ids.size();
        LocalExecutionRunner runner = new LocalExecutionRunner();
        int maxFutures = configuration.getNumberOfThreads();
        int futureCount = 0;
        try (ProgressBar pb = new ProgressBar(String.format("cc:%s-%s", project, category), listSize)) {
            for (int c = 0; c < listSize; c++) {
                pb.step();

                String trainingFile = String.format("%s/%s/%d_%s.arff", isCommitBased ? commitParentFolder : releaseParentFolder, project, ids.get(c), hashes.get(c));
                if (listSize > c + 1) {
                    int testID = ids.get(c + 1);
                    String testCommit = hashes.get(c + 1);
                    pb.setExtraMessage(testCommit);
                    String testFile = String.format("%s/%s/%d_%s.arff", isCommitBased ? commitParentFolder : releaseParentFolder, project, testID, testCommit);

                    if (futureCount < maxFutures) {
                        Runnable commitTaskHandler = new CommitTaskHandler(trainingFile, testFile, project, testCommit, testID, normalize, balance, writer);
                        runner.addFuture((Future<?>) runner.submit(commitTaskHandler));
                        futureCount++;
                    }
                    if (futureCount == maxFutures) {
                        runner.waitForTaskToFinish();
                        futureCount = 0;
                    }
//                    Runnable commitTaskHandler = new CommitTaskHandler(trainingFile,testFile,project,testCommit,testID,normalize,balance,writer);
//                    runner.addFuture((Future<?>) runner.submit(commitTaskHandler));

                    //                    try {
//                        Stopwatch stopwatch = Stopwatch.createStarted();
//                        RandomForest randomForest = new RandomForest();
//
//                        //training
//                        Instances trainingData = getTrainingDataset(trainingFile, normalize, balance);
//
//                        //test
//                        Instances testData = new Instances(new BufferedReader(new FileReader(testFile)));
//                        testData.setClassIndex(testData.numAttributes() - 1);
//                        //build
//                        randomForest.buildClassifier(trainingData);
//                        Evaluation eval = new Evaluation(trainingData);
//                        eval.evaluateModel(randomForest, testData);
//                        stopwatch.stop();
//                        writer.printf("%d;%s;%s;%.5f;%s\n", testID, project, testCommit, eval.areaUnderROC(0), stopwatch.elapsed(TimeUnit.SECONDS));
//
//                    } catch (Exception ex) {
//                        ex.printStackTrace();
//                    }

                }
            }
            runner.waitForTaskToFinish();
            runner.shutdown();
        }
    }


}
