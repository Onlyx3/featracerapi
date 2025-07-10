package se.gu.defectpred;

import com.google.common.base.Stopwatch;
import me.tongfei.progressbar.ProgressBar;
import se.gu.ml.preprocessing.MetricValue;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;
import weka.filters.Filter;
import weka.filters.supervised.instance.SMOTE;
import weka.filters.unsupervised.attribute.Normalize;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class ClassificationTaskHandler implements Runnable, Callable<MetricValue>, Serializable {

    private static final long serialVersionUID = -1616233607508591775L;

    private List<Integer> ids;
    private List<String> hashes;
    private PrintWriter writer;
    private String project, category, commitParentFolder, releaseParentFolder;
    private boolean normalize, balance, isCommitBased;

    public ClassificationTaskHandler(List<Integer> ids, List<String> hashes, PrintWriter writer, String project, String category, String commitParentFolder, String releaseParentFolder, boolean normalize, boolean balance, boolean isCommitBased) {
        this.ids = ids;
        this.hashes = hashes;
        this.writer = writer;
        this.project = project;
        this.category = category;
        this.commitParentFolder = commitParentFolder;
        this.releaseParentFolder = releaseParentFolder;
        this.normalize = normalize;
        this.balance = balance;
        this.isCommitBased = isCommitBased;
    }

    @Override
    public void run() {
        classify();
    }

    @Override
    public MetricValue call() throws Exception {
        return null;
    }

    private Instances getTrainingDataset(String trainingFile, boolean normalize, boolean balance) throws Exception {
        ConverterUtils.DataSource source = new ConverterUtils.DataSource(trainingFile);
        Instances trainingData = source.getDataSet();
        trainingData.setClassIndex(trainingData.numAttributes() - 1);
        //if normalization or balancing failes, it shouldn't affect the whole prediction.
        if (normalize) {
            try {
                Normalize normalizer = new Normalize();
                normalizer.setInputFormat(trainingData);
                trainingData = Filter.useFilter(trainingData, normalizer);
            } catch (Exception ex) {

            }
        }
        if (balance) {
            try {
                SMOTE smote = new SMOTE();
                smote.setInputFormat(trainingData);
                trainingData = Filter.useFilter(trainingData, smote);
            } catch (Exception ex) {

            }
        }
        return trainingData;

    }

    public void classify() {
        //LocalExecutionRunner runner =new LocalExecutionRunner();
        int listSize = ids.size();
        try (ProgressBar pb = new ProgressBar(String.format("cc:%s-%s", project, category), listSize)) {
            for (int c = 0; c < listSize; c++) {
                pb.step();

                String trainingFile = String.format("%s/%s/%d_%s.arff", isCommitBased ? commitParentFolder : releaseParentFolder, project, ids.get(c), hashes.get(c));
                if (listSize > c + 1) {
                    int testID = ids.get(c + 1);
                    String testCommit = hashes.get(c + 1);
                    pb.setExtraMessage(testCommit);
                    String testFile = String.format("%s/%s/%d_%s.arff", isCommitBased ? commitParentFolder : releaseParentFolder, project, testID, testCommit);
                    //clasify
                    try {
                        Stopwatch stopwatch = Stopwatch.createStarted();
                        RandomForest randomForest = new RandomForest();

                        //training
                        Instances trainingData = getTrainingDataset(trainingFile, normalize, balance);

                        //test
                        Instances testData = new Instances(new BufferedReader(new FileReader(testFile)));
                        testData.setClassIndex(testData.numAttributes() - 1);
                        //build
                        randomForest.buildClassifier(trainingData);
                        Evaluation eval = new Evaluation(trainingData);
                        eval.evaluateModel(randomForest, testData);
                        stopwatch.stop();
//                        String text = String.format();
//                        System.out.println(text);
                        writer.printf("%d;%s;%s;%.5f;%s\n", testID, project, testCommit, eval.areaUnderROC(0), stopwatch.elapsed(TimeUnit.SECONDS));


                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
//                    Runnable commitTaskHandler = new CommitTaskHandler(trainingFile,testFile,project,testCommit,testID,normalize,balance,writer);
//                    runner.addFuture((Future<?>) runner.submit(commitTaskHandler));

                }
            }
        }
//        runner.waitForTaskToFinish();
//        runner.shutdown();
    }

}
