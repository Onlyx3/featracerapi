package se.gu.ml.experiment;

import java.io.Serializable;

public class ResultSummary  implements Serializable {
    private static final long serialVersionUID = 2282124700857948740L;
    private String dataSet,classifier;
    private Double totalInstances;
    private Double instancesWithRankedLabels;
    private Double percentageOfInstancesWithRankedLabels;
    private Double instancesWithPrecisionNull;
    private Double percentageOfInstancesWithPrecisionNull;
    private Double instancesWithPrecisionZero;
    private Double percentageOfInstancesWithPrecisionZero;
    private Double instancesWithPrecisionNonZero;
    private Double percentageOfInstancesWithPrecisionNonZero;
    private Double instancesWithPrecisionOne;
    private Double percentageOfInstancesWithPrecisionOne;
    private Double instancesWithRecallZero;
    private Double percentageOfInstancesWithRecallZero;
    private Double instancesWithRecallOne;
    private Double percentageOfInstancesWithRecallOne;
    private Double averagePrecisionForAllNonNullPrecision;
    private Double averagePrecisionForNonZeroPrecision;
    private Double averageRecallForAllNonNullPrecision;
    private Double averageRecallForAllNonZeroPrecision;
    private Double averageFScoreForAllNonNullPrecision;
    private Double averageFScoreForAllNonZeroPrecision;

    public Double getTrainTime() {
        return trainTime;
    }

    public void setTrainTime(Double trainTime) {
        this.trainTime = trainTime;
    }

    public Double getTestTime() {
        return testTime;
    }

    public void setTestTime(Double testTime) {
        this.testTime = testTime;
    }

    private Double trainTime,testTime;

    public String getDataSet() {
        return dataSet;
    }

    public void setDataSet(String dataSet) {
        this.dataSet = dataSet;
    }

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public Double getTotalInstances() {
        return totalInstances;
    }

    public void setTotalInstances(Double totalInstances) {
        this.totalInstances = totalInstances;
    }

    public Double getInstancesWithRankedLabels() {
        return instancesWithRankedLabels;
    }

    public void setInstancesWithRankedLabels(Double instancesWithRankedLabels) {
        this.instancesWithRankedLabels = instancesWithRankedLabels;
    }

    public Double getInstancesWithPrecisionNull() {
        return instancesWithPrecisionNull;
    }

    public void setInstancesWithPrecisionNull(Double instancesWithPrecisionNull) {
        this.instancesWithPrecisionNull = instancesWithPrecisionNull;
    }

    public Double getInstancesWithPrecisionZero() {
        return instancesWithPrecisionZero;
    }

    public void setInstancesWithPrecisionZero(Double instancesWithPrecisionZero) {
        this.instancesWithPrecisionZero = instancesWithPrecisionZero;
    }

    public Double getInstancesWithPrecisionNonZero() {
        return instancesWithPrecisionNonZero;
    }

    public void setInstancesWithPrecisionNonZero(Double instancesWithPrecisionNonZero) {
        this.instancesWithPrecisionNonZero = instancesWithPrecisionNonZero;
    }

    public Double getInstancesWithPrecisionOne() {
        return instancesWithPrecisionOne;
    }

    public void setInstancesWithPrecisionOne(Double instancesWithPrecisionOne) {
        this.instancesWithPrecisionOne = instancesWithPrecisionOne;
    }

    public Double getInstancesWithRecallZero() {
        return instancesWithRecallZero;
    }

    public void setInstancesWithRecallZero(Double instancesWithRecallZero) {
        this.instancesWithRecallZero = instancesWithRecallZero;
    }

    public Double getInstancesWithRecallOne() {
        return instancesWithRecallOne;
    }

    public void setInstancesWithRecallOne(Double instancesWithRecallOne) {
        this.instancesWithRecallOne = instancesWithRecallOne;
    }

    public Double getAveragePrecisionForAllNonNullPrecision() {
        return averagePrecisionForAllNonNullPrecision;
    }

    public void setAveragePrecisionForAllNonNullPrecision(Double averagePrecisionForAllNonNullPrecision) {
        this.averagePrecisionForAllNonNullPrecision = averagePrecisionForAllNonNullPrecision;
    }

    public Double getAveragePrecisionForNonZeroPrecision() {
        return averagePrecisionForNonZeroPrecision;
    }

    public void setAveragePrecisionForNonZeroPrecision(Double averagePrecisionForNonZeroPrecision) {
        this.averagePrecisionForNonZeroPrecision = averagePrecisionForNonZeroPrecision;
    }

    public Double getAverageRecallForAllNonNullPrecision() {
        return averageRecallForAllNonNullPrecision;
    }

    public void setAverageRecallForAllNonNullPrecision(Double averageRecallForAllNonNullPrecision) {
        this.averageRecallForAllNonNullPrecision = averageRecallForAllNonNullPrecision;
    }

    public Double getAverageRecallForAllNonZeroPrecision() {
        return averageRecallForAllNonZeroPrecision;
    }

    public void setAverageRecallForAllNonZeroPrecision(Double averageRecallForAllNonZeroPrecision) {
        this.averageRecallForAllNonZeroPrecision = averageRecallForAllNonZeroPrecision;
    }

    public Double getAverageFScoreForAllNonNullPrecision() {
        return averageFScoreForAllNonNullPrecision;
    }

    public void setAverageFScoreForAllNonNullPrecision(Double averageFScoreForAllNonNullPrecision) {
        this.averageFScoreForAllNonNullPrecision = averageFScoreForAllNonNullPrecision;
    }

    public Double getAverageFScoreForAllNonZeroPrecision() {
        return averageFScoreForAllNonZeroPrecision;
    }

    public void setAverageFScoreForAllNonZeroPrecision(Double averageFScoreForAllNonZeroPrecision) {
        this.averageFScoreForAllNonZeroPrecision = averageFScoreForAllNonZeroPrecision;
    }
    public Double getPercentageOfInstancesWithRankedLabels() {
        return percentageOfInstancesWithRankedLabels;
    }

    public void setPercentageOfInstancesWithRankedLabels(Double percentageOfInstancesWithRankedLabels) {
        this.percentageOfInstancesWithRankedLabels = percentageOfInstancesWithRankedLabels;
    }

    public Double getPercentageOfInstancesWithPrecisionNull() {
        return percentageOfInstancesWithPrecisionNull;
    }

    public void setPercentageOfInstancesWithPrecisionNull(Double percentageOfInstancesWithPrecisionNull) {
        this.percentageOfInstancesWithPrecisionNull = percentageOfInstancesWithPrecisionNull;
    }

    public Double getPercentageOfInstancesWithPrecisionZero() {
        return percentageOfInstancesWithPrecisionZero;
    }

    public void setPercentageOfInstancesWithPrecisionZero(Double percentageOfInstancesWithPrecisionZero) {
        this.percentageOfInstancesWithPrecisionZero = percentageOfInstancesWithPrecisionZero;
    }

    public Double getPercentageOfInstancesWithPrecisionNonZero() {
        return percentageOfInstancesWithPrecisionNonZero;
    }

    public void setPercentageOfInstancesWithPrecisionNonZero(Double percentageOfInstancesWithPrecisionNonZero) {
        this.percentageOfInstancesWithPrecisionNonZero = percentageOfInstancesWithPrecisionNonZero;
    }

    public Double getPercentageOfInstancesWithPrecisionOne() {
        return percentageOfInstancesWithPrecisionOne;
    }

    public void setPercentageOfInstancesWithPrecisionOne(Double percentageOfInstancesWithPrecisionOne) {
        this.percentageOfInstancesWithPrecisionOne = percentageOfInstancesWithPrecisionOne;
    }

    public Double getPercentageOfInstancesWithRecallZero() {
        return percentageOfInstancesWithRecallZero;
    }

    public void setPercentageOfInstancesWithRecallZero(Double percentageOfInstancesWithRecallZero) {
        this.percentageOfInstancesWithRecallZero = percentageOfInstancesWithRecallZero;
    }

    public Double getPercentageOfInstancesWithRecallOne() {
        return percentageOfInstancesWithRecallOne;
    }

    public void setPercentageOfInstancesWithRecallOne(Double percentageOfInstancesWithRecallOne) {
        this.percentageOfInstancesWithRecallOne = percentageOfInstancesWithRecallOne;
    }


}
