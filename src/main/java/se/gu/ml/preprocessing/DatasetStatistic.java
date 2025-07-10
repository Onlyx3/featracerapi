package se.gu.ml.preprocessing;

import java.io.File;
import java.io.Serializable;

public class DatasetStatistic  implements Serializable {
    private static final long serialVersionUID = 1355693962810060672L;
    private File arrfFile,xmlFile;

    public File getArrfFile() {
        return arrfFile;
    }

    public void setArrfFile(File arrfFile) {
        this.arrfFile = arrfFile;
    }

    public File getXmlFile() {
        return xmlFile;
    }

    public void setXmlFile(File xmlFile) {
        this.xmlFile = xmlFile;
    }

    private String datasetName;
    private double numOfInstances;
    private double meanIR;
    private double CVIR;
    private double maxIRLb;
    private double minIRLb;
    private double meanImR;
    private double maxImR;

    public double getCVIR() {
        return CVIR;
    }

    public void setCVIR(double CVIR) {
        this.CVIR = CVIR;
    }

    public double getMaxIRLb() {
        return maxIRLb;
    }

    public void setMaxIRLb(double maxIRLb) {
        this.maxIRLb = maxIRLb;
    }

    public double getMinIRLb() {
        return minIRLb;
    }

    public void setMinIRLb(double minIRLb) {
        this.minIRLb = minIRLb;
    }

    public double getMeanImR() {
        return meanImR;
    }

    public void setMeanImR(double meanImR) {
        this.meanImR = meanImR;
    }

    public double getMaxImR() {
        return maxImR;
    }

    public void setMaxImR(double maxImR) {
        this.maxImR = maxImR;
    }

    public double getMinImR() {
        return minImR;
    }

    public void setMinImR(double minImR) {
        this.minImR = minImR;
    }

    private double minImR;
    private double scumble;
    private double cardinality;
    private double numOfLabels;

    public double getCardinality() {
        return cardinality;
    }

    public void setCardinality(double cardinality) {
        this.cardinality = cardinality;
    }

    public double getNumOfLabels() {
        return numOfLabels;
    }

    public void setNumOfLabels(double numOfLabels) {
        this.numOfLabels = numOfLabels;
    }

    public double getNumAttributes() {
        return numAttributes;
    }

    public void setNumAttributes(double numAttributes) {
        this.numAttributes = numAttributes;
    }

    private double numAttributes;

    public String getDatasetName() {
        return datasetName;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    public double getNumOfInstances() {
        return numOfInstances;
    }

    public void setNumOfInstances(double numOfInstances) {
        this.numOfInstances = numOfInstances;
    }

    public double getMeanIR() {
        return meanIR;
    }

    public void setMeanIR(double meanIR) {
        this.meanIR = meanIR;
    }

    public double getScumble() {
        return scumble;
    }

    public void setScumble(double scumble) {
        this.scumble = scumble;
    }

    public int getCommitIndex() {
        return commitIndex;
    }

    public void setCommitIndex(int commitIndex) {
        this.commitIndex = commitIndex;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public void setCommitHash(String commitHash) {
        this.commitHash = commitHash;
    }

    private int commitIndex;
    private String project;
    private String commitHash;

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    private String level;
}
