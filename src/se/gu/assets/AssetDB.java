package se.gu.assets;

public class AssetDB {
    public AssetDB() {
    }

    public AssetDB(String assetFullName, String developer, String commitHash, int commitIndex,String assetType) {
        this.assetFullName = assetFullName;
        this.commitHash = commitHash;
        this.developer = developer;
        this.commitIndex = commitIndex;
        this.assetType=assetType;
    }

    public AssetDB(String assetFullName, String assetName, String parent, String commitHash, String developer, String assetType, String project, String changeType, int startingLine, int endingLine, int lineNumber, int commitIndex, int nloc) {
        this.assetFullName = assetFullName;
        this.assetName = assetName;
        this.parent = parent;
        this.commitHash = commitHash;
        this.developer = developer;
        this.assetType = assetType;
        this.project = project;
        this.changeType = changeType;
        this.startingLine = startingLine;
        this.endingLine = endingLine;
        this.lineNumber = lineNumber;
        this.commitIndex = commitIndex;
        this.nloc=nloc;
    }

    public int getNloc() {
        return nloc;
    }

    public void setNloc(int nloc) {
        this.nloc = nloc;
    }

    private int nloc;
    public String getAssetFullName() {
        return assetFullName;
    }

    public void setAssetFullName(String assetFullName) {
        this.assetFullName = assetFullName;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public void setCommitHash(String commitHash) {
        this.commitHash = commitHash;
    }

    public String getDeveloper() {
        return developer;
    }

    public void setDeveloper(String developer) {
        this.developer = developer;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public int getStartingLine() {
        return startingLine;
    }

    public void setStartingLine(int startingLine) {
        this.startingLine = startingLine;
    }

    public int getEndingLine() {
        return endingLine;
    }

    public void setEndingLine(int endingLine) {
        this.endingLine = endingLine;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public int getCommitIndex() {
        return commitIndex;
    }

    public void setCommitIndex(int commitIndex) {
        this.commitIndex = commitIndex;
    }

    private String assetFullName,
            assetName,
            parent,
            commitHash,
            developer,
            assetType,
            project,
            changeType;
    private int startingLine,
            endingLine,
            lineNumber,
            commitIndex;
}
