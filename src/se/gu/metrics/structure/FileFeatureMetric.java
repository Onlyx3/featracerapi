package se.gu.metrics.structure;

import java.io.Serializable;

public class FileFeatureMetric implements Serializable {
    private static final long serialVersionUID = -7481271857183524386L;

    public int getNumberOffeaturesInFile() {
        return numberOffeaturesInFile;
    }

    public void setNumberOffeaturesInFile(int numberOffeaturesInFile) {
        this.numberOffeaturesInFile = numberOffeaturesInFile;
    }

    private int numberOffeaturesInFile;
    String file, feature;

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getFeature() {
        return feature;
    }

    public void setFeature(String feature) {
        this.feature = feature;
    }

    public double getScatteringDegree() {
        return scatteringDegree;
    }

    public void setScatteringDegree(double scatteringDegree) {
        this.scatteringDegree = scatteringDegree;
    }

    public double getTanglingDegreeWithinFile() {
        return tanglingDegreeWithinFile;
    }

    public void setTanglingDegreeWithinFile(double tanglingDegreeWithinFile) {
        this.tanglingDegreeWithinFile = tanglingDegreeWithinFile;
    }

    public double getTanglingDegreeAcrossFiles() {
        return tanglingDegreeAcrossFiles;
    }

    public void setTanglingDegreeAcrossFiles(double tanglingDegreeAcrossFiles) {
        this.tanglingDegreeAcrossFiles = tanglingDegreeAcrossFiles;
    }

    public double getNestingDepth() {
        return nestingDepth;
    }

    public void setNestingDepth(double nestingDepth) {
        this.nestingDepth = nestingDepth;
    }

    public double getLinesOfFeatureCode() {
        return linesOfFeatureCode;
    }

    public void setLinesOfFeatureCode(double linesOfFeatureCode) {
        this.linesOfFeatureCode = linesOfFeatureCode;
    }

    double scatteringDegree, tanglingDegreeWithinFile, tanglingDegreeAcrossFiles, nestingDepth, linesOfFeatureCode;
}
