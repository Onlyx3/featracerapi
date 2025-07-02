package se.gu.assets;

public class DataSetRecord {
    private int commitIdex;
    private String
            commitHash,
            project,
            assetType,
            trainingFile,
            testFile;
    private boolean
            isMappedOnly;
    private String
            trainingXMLFile,
            testXMLFile,
            testCSVFile;
    private int
            testCommitIndex;
    private String
            testCommitHash;

    public DataSetRecord(int commitIdex, String commitHash, String project, String assetType, String trainingFile, String testFile, boolean isMappedOnly, String trainingXMLFile, String testXMLFile, String testCSVFile, int testCommitIndex, String testCommitHash) {
        this.commitIdex = commitIdex;
        this.commitHash = commitHash;
        this.project = project;
        this.assetType = assetType;
        this.trainingFile = trainingFile;
        this.testFile = testFile;
        this.isMappedOnly = isMappedOnly;
        this.trainingXMLFile = trainingXMLFile;
        this.testXMLFile = testXMLFile;
        this.testCSVFile = testCSVFile;
        this.testCommitIndex = testCommitIndex;
        this.testCommitHash = testCommitHash;
    }

    public int getCommitIdex() {
        return commitIdex;
    }

    public void setCommitIdex(int commitIdex) {
        this.commitIdex = commitIdex;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public void setCommitHash(String commitHash) {
        this.commitHash = commitHash;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public String getTrainingFile() {
        return trainingFile;
    }

    public void setTrainingFile(String trainingFile) {
        this.trainingFile = trainingFile;
    }

    public String getTestFile() {
        return testFile;
    }

    public void setTestFile(String testFile) {
        this.testFile = testFile;
    }

    public boolean isMappedOnly() {
        return isMappedOnly;
    }

    public void setMappedOnly(boolean mappedOnly) {
        isMappedOnly = mappedOnly;
    }

    public String getTrainingXMLFile() {
        return trainingXMLFile;
    }

    public void setTrainingXMLFile(String trainingXMLFile) {
        this.trainingXMLFile = trainingXMLFile;
    }

    public String getTestXMLFile() {
        return testXMLFile;
    }

    public void setTestXMLFile(String testXMLFile) {
        this.testXMLFile = testXMLFile;
    }

    public String getTestCSVFile() {
        return testCSVFile;
    }

    public void setTestCSVFile(String testCSVFile) {
        this.testCSVFile = testCSVFile;
    }

    public int getTestCommitIndex() {
        return testCommitIndex;
    }

    public void setTestCommitIndex(int testCommitIndex) {
        this.testCommitIndex = testCommitIndex;
    }

    public String getTestCommitHash() {
        return testCommitHash;
    }

    public void setTestCommitHash(String testCommitHash) {
        this.testCommitHash = testCommitHash;
    }
}
