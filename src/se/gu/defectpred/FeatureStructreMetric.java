package se.gu.defectpred;

public class FeatureStructreMetric {
    public FeatureStructreMetric(String textLine) {
        //releaseID;release;commitID;commitHash;feature;scatteringDegree;tanglingDegreeAcrossFiles;nestingDepth;linesOfFeatureCode;fAddedLines;fDeletedLines
        String[]items = textLine.split(";");
        setReleaseID(Integer.parseInt(items[0]));
        setRelease(items[1]);
        setCommitID(Integer.parseInt(items[2]));
        setCommitHash(items[3]);
        setFeature(items[4]);
        setScatteringDegree(Double.parseDouble(items[5].trim()));
        setTanglingDegreeAcrossFiles(Double.parseDouble(items[6].trim()));
        setNestingDepth(Double.parseDouble(items[7].trim()));
        setLinesOfFeatureCode(Double.parseDouble(items[8].trim()));
        setfAddedLines(Double.parseDouble(items[9].trim()));
        setfDeletedLines(Double.parseDouble(items[10].trim()));

    }
    public FeatureStructreMetric(){

    }

    public String getRelease() {
        return release;
    }

    public void setRelease(String release) {
        this.release = release;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public void setCommitHash(String commitHash) {
        this.commitHash = commitHash;
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

    public double getfAddedLines() {
        return fAddedLines;
    }

    public void setfAddedLines(double fAddedLines) {
        this.fAddedLines = fAddedLines;
    }

    public double getfDeletedLines() {
        return fDeletedLines;
    }

    public void setfDeletedLines(double fDeletedLines) {
        this.fDeletedLines = fDeletedLines;
    }

    private String release,commitHash,feature;

    public int getReleaseID() {
        return releaseID;
    }

    public void setReleaseID(int releaseID) {
        this.releaseID = releaseID;
    }

    public int getCommitID() {
        return commitID;
    }

    public void setCommitID(int commitID) {
        this.commitID = commitID;
    }

    private int releaseID,commitID;
    private double scatteringDegree,tanglingDegreeAcrossFiles,nestingDepth,linesOfFeatureCode,fAddedLines,fDeletedLines;
}
