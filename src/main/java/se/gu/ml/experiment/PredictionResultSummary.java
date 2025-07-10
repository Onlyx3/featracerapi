package se.gu.ml.experiment;

import java.io.Serializable;

public class PredictionResultSummary  implements Serializable {
    private static final long serialVersionUID = -7932867979500274597L;
    private String commit;

    public long getTrainTime() {
        return trainTime;
    }

    public void setTrainTime(long trainTime) {
        this.trainTime = trainTime;
    }

    public long getTestTime() {
        return testTime;
    }

    public void setTestTime(long testTime) {
        this.testTime = testTime;
    }

    private long trainTime,testTime;
    public String getCommit() {
        return commit;
    }

    public void setCommit(String commit) {
        this.commit = commit;
    }

    private String classifier;
    private Double instances;
    private Double labeldInstance;

    public Double getLabeldInstance() {
        return labeldInstance;
    }

    public void setLabeldInstance(Double labeldInstance) {
        this.labeldInstance = labeldInstance;
    }

    private Double rankedInstance;
    private Double precision;
    private Double recall;
    private Double fscore;

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public Double getInstances() {
        return instances;
    }

    public void setInstances(Double instances) {
        this.instances = instances;
    }

    public Double getRankedInstance() {
        return rankedInstance;
    }

    public void setRankedInstance(Double rankedInstance) {
        this.rankedInstance = rankedInstance;
    }

    public Double getPrecision() {
        return precision;
    }

    public void setPrecision(Double precision) {
        this.precision = precision;
    }

    public Double getRecall() {
        return recall;
    }

    public void setRecall(Double recall) {
        this.recall = recall;
    }

    public Double getFscore() {
        return fscore;
    }

    public void setFscore(Double fscore) {
        this.fscore = fscore;
    }
}
