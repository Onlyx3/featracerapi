package se.gu.defectpred;

import com.google.common.base.Stopwatch;
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
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class CommitTaskHandler  implements Runnable, Callable<MetricValue>, Serializable {
    private static final long serialVersionUID = -8320846132544425720L;
    private String trainingFile,testFile,project,testCommit;
    private int testID;
    private boolean normalize, balance;
    private PrintWriter writer;

    public CommitTaskHandler(String trainingFile, String testFile, String project, String testCommit, int testID, boolean normalize, boolean balance, PrintWriter writer) {
        this.trainingFile = trainingFile;
        this.testFile = testFile;
        this.project = project;
        this.testCommit = testCommit;
        this.testID = testID;
        this.normalize = normalize;
        this.balance = balance;
        this.writer = writer;
    }

    @Override
    public void run() {
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
            String text = String.format("%d;%s;%s;%.5f;%s\n", testID, project, testCommit, eval.areaUnderROC(0), stopwatch.elapsed(TimeUnit.SECONDS));
            System.out.println(text);
            writer.printf(text);


        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
}
