package se.gu.assets;

public class AssetMappingDB {
    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    private  String assetType;

    public AssetMappingDB(String assetfullname, String assetType, String parent, String featurename, String project, String annotationType, String commitHash, String developer, int commitIndex) {
        this.assetfullname = assetfullname;
        this.assetType = assetType;
        this.parent = parent;
        this.featurename = featurename;
        this.project = project;
        this.annotationType = annotationType;
        this.commitHash = commitHash;
        this.developer = developer;
        this.commitIndex = commitIndex;
    }

    public AssetMappingDB(String assetfullname,String assetType, String featurename, String commitHash, int commitIndex) {
        this.assetfullname = assetfullname;
        this.assetType=assetType;
        this.featurename = featurename;
        this.commitHash = commitHash;
        this.commitIndex = commitIndex;
    }

    public String getAssetfullname() {
        return assetfullname;
    }

    public void setAssetfullname(String assetfullname) {
        this.assetfullname = assetfullname;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getFeaturename() {
        return featurename;
    }

    public void setFeaturename(String featurename) {
        this.featurename = featurename;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getAnnotationType() {
        return annotationType;
    }

    public void setAnnotationType(String annotationType) {
        this.annotationType = annotationType;
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

    public int getCommitIndex() {
        return commitIndex;
    }

    public void setCommitIndex(int commitIndex) {
        this.commitIndex = commitIndex;
    }

    private String assetfullname,
            parent,
            featurename,
            project,
            annotationType,
            commitHash,
            developer;
    private int commitIndex;
}
