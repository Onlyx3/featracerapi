package se.gu.ml.experiment;

import java.io.File;
import java.io.Serializable;

public class ExperiementFileRecord implements Serializable {
    private static final long serialVersionUID = 7198472489336298880L;
    private File arffFile;
    private File xmlFile;
    private File flatXMLFile;
    private File predictionFile;
    private File assetMappingsFile;

    public File getPredictionsXMLFile() {
        return predictionsXMLFile;
    }

    public void setPredictionsXMLFile(File predictionsXMLFile) {
        this.predictionsXMLFile = predictionsXMLFile;
    }

    private File predictionsXMLFile;

    public File getAssetMappingsFile() {
        return assetMappingsFile;
    }

    public void setAssetMappingsFile(File assetMappingsFile) {
        this.assetMappingsFile = assetMappingsFile;
    }

    public File getInstanceNameFile() {
        return instanceNameFile;
    }

    public void setInstanceNameFile(File instanceNameFile) {
        this.instanceNameFile = instanceNameFile;
    }

    private File instanceNameFile;

    public File getPredictionFile() {
        return predictionFile;
    }

    public void setPredictionFile(File predictionFile) {
        this.predictionFile = predictionFile;
    }

    public File getTestARFFFile() {
        return testARFFFile;
    }

    public void setTestARFFFile(File testARFFFile) {
        this.testARFFFile = testARFFFile;
    }

    private File testARFFFile;
    private String commit;
    private int commitNumber;
    static int globalCommitNumber = 1;

    public int getCommitNumber() {
        return commitNumber;
    }

    public void setCommitNumber(int commitNumber) {
        this.commitNumber = commitNumber;
    }
    public void setCommitNumber(String arffFileWithUnderScoredCommitNumber) {
        if(arffFileWithUnderScoredCommitNumber.contains("_")){
            this.commitNumber = Integer.parseInt(arffFileWithUnderScoredCommitNumber.split("_")[0]);
        }else{
            this.commitNumber = globalCommitNumber+1;
            globalCommitNumber+=1;
        }

    }

    public ExperiementFileRecord() {
    }

    public ExperiementFileRecord(File arffFile, File xmlFile, File flatXMLFile, String commit) {
        this(arffFile,xmlFile,flatXMLFile);
        setCommit(commit);
    }

    public String getCommit() {
        return commit;
    }

    public void setCommit(String commit) {
        this.commit = commit;
    }

    public ExperiementFileRecord(File arffFile, File xmlFile, File flatXMLFile) {
        this.arffFile = arffFile;
        this.xmlFile = xmlFile;
        this.flatXMLFile = flatXMLFile;
        setCommitNumber(arffFile.getName());
    }

    public File getArffFile() {
        return arffFile;
    }

    public void setArffFile(File arffFile) {
        this.arffFile = arffFile;
    }

    public File getXmlFile() {
        return xmlFile;
    }

    public void setXmlFile(File xmlFile) {
        this.xmlFile = xmlFile;
    }

    public File getFlatXMLFile() {
        return flatXMLFile;
    }

    public void setFlatXMLFile(File flatXMLFile) {
        this.flatXMLFile = flatXMLFile;
    }
}
