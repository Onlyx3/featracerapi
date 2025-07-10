package se.gu.ml.experiment;

import java.io.Serializable;

public class ResultMeasure  implements Serializable {
    private static final long serialVersionUID = -3237075679752905384L;
    private double
            hammingLoss;
    private double subsetAccuracy;
    private double exampleBasedPrecision;
    private double exampleBasedRecall;
    private double exampleBasedFMeasure;
    private double exampleBasedAccuracy;
    private double exampleBasedSpecificity;

    public double getHammingLoss() {
        return hammingLoss;
    }

    public void setHammingLoss(double hammingLoss) {
        this.hammingLoss = hammingLoss;
    }

    public double getSubsetAccuracy() {
        return subsetAccuracy;
    }

    public void setSubsetAccuracy(double subsetAccuracy) {
        this.subsetAccuracy = subsetAccuracy;
    }

    public double getExampleBasedPrecision() {
        return exampleBasedPrecision;
    }

    public void setExampleBasedPrecision(double exampleBasedPrecision) {
        this.exampleBasedPrecision = exampleBasedPrecision;
    }

    public double getExampleBasedRecall() {
        return exampleBasedRecall;
    }

    public void setExampleBasedRecall(double exampleBasedRecall) {
        this.exampleBasedRecall = exampleBasedRecall;
    }

    public double getExampleBasedFMeasure() {
        return exampleBasedFMeasure;
    }

    public void setExampleBasedFMeasure(double exampleBasedFMeasure) {
        this.exampleBasedFMeasure = exampleBasedFMeasure;
    }

    public double getExampleBasedAccuracy() {
        return exampleBasedAccuracy;
    }

    public void setExampleBasedAccuracy(double exampleBasedAccuracy) {
        this.exampleBasedAccuracy = exampleBasedAccuracy;
    }

    public double getExampleBasedSpecificity() {
        return exampleBasedSpecificity;
    }

    public void setExampleBasedSpecificity(double exampleBasedSpecificity) {
        this.exampleBasedSpecificity = exampleBasedSpecificity;
    }

    public double getMicroPrecision() {
        return microPrecision;
    }

    public void setMicroPrecision(double microPrecision) {
        this.microPrecision = microPrecision;
    }

    public double getMicroRecall() {
        return microRecall;
    }

    public void setMicroRecall(double microRecall) {
        this.microRecall = microRecall;
    }

    public double getMicroFMeasure() {
        return microFMeasure;
    }

    public void setMicroFMeasure(double microFMeasure) {
        this.microFMeasure = microFMeasure;
    }

    public double getMicroSpecificity() {
        return microSpecificity;
    }

    public void setMicroSpecificity(double microSpecificity) {
        this.microSpecificity = microSpecificity;
    }

    public double getMacroPrecision() {
        return macroPrecision;
    }

    public void setMacroPrecision(double macroPrecision) {
        this.macroPrecision = macroPrecision;
    }

    public double getMacroRecall() {
        return macroRecall;
    }

    public void setMacroRecall(double macroRecall) {
        this.macroRecall = macroRecall;
    }

    public double getMacroFMeasure() {
        return macroFMeasure;
    }

    public void setMacroFMeasure(double macroFMeasure) {
        this.macroFMeasure = macroFMeasure;
    }

    public double getMacroSpecificity() {
        return macroSpecificity;
    }

    public void setMacroSpecificity(double macroSpecificity) {
        this.macroSpecificity = macroSpecificity;
    }

    public double getAveragePrecision() {
        return averagePrecision;
    }

    public void setAveragePrecision(double averagePrecision) {
        this.averagePrecision = averagePrecision;
    }

    public double getCoverage() {
        return coverage;
    }

    public void setCoverage(double coverage) {
        this.coverage = coverage;
    }

    public double getOneError() {
        return oneError;
    }

    public void setOneError(double oneError) {
        this.oneError = oneError;
    }

    public double getIsError() {
        return isError;
    }

    public void setIsError(double isError) {
        this.isError = isError;
    }

    public double getErrorSetSize() {
        return errorSetSize;
    }

    public void setErrorSetSize(double errorSetSize) {
        this.errorSetSize = errorSetSize;
    }

    public double getRankingLoss() {
        return rankingLoss;
    }

    public void setRankingLoss(double rankingLoss) {
        this.rankingLoss = rankingLoss;
    }

    public double getMeanAveragePrecision() {
        return meanAveragePrecision;
    }

    public void setMeanAveragePrecision(double meanAveragePrecision) {
        this.meanAveragePrecision = meanAveragePrecision;
    }

    public double getGeometricMeanAveragePrecision() {
        return geometricMeanAveragePrecision;
    }

    public void setGeometricMeanAveragePrecision(double geometricMeanAveragePrecision) {
        this.geometricMeanAveragePrecision = geometricMeanAveragePrecision;
    }

    private double// add label-based measures
        microPrecision;
    private double microRecall;
    private double microFMeasure;
    private double microSpecificity;
    private double//microAccuracy,
        macroPrecision;
    private double macroRecall;
    private double macroFMeasure;
    private double macroSpecificity;
    //macroAccuracy,

    private double// add ranking based measures
        averagePrecision;
    private double coverage;
    private double oneError;
    private double isError;
    private double errorSetSize;
    private double rankingLoss;

    private double// add confidence measures if applicable
        meanAveragePrecision;
    private double geometricMeanAveragePrecision;
}
