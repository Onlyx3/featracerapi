package se.gu.metrics;

public class AssetMetricsDB {
    public AssetMetricsDB() {
    }

    public AssetMetricsDB(String asset,String assetType, String project, boolean isMapped, String commitHash, String parent, int commitIndex, double csvDev, double ddev, double comm, double dcont, double hdcont, double ccc, double accc, double nloc, double dnfma, double nfma, double nff) {
        this.asset = asset;
        this.assetType = assetType;
        this.project = project;
        this.isMapped = isMapped;
        this.commitHash = commitHash;
        this.parent = parent;
        this.commitIndex = commitIndex;
        this.csvDev = csvDev;
        this.ddev = ddev;
        this.comm = comm;
        this.dcont = dcont;
        this.hdcont = hdcont;
        this.ccc = ccc;
        this.accc = accc;
        this.nloc = nloc;
        this.dnfma = dnfma;
        this.nfma = nfma;
        this.nff = nff;
    }

    private String asset;

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public boolean isMapped() {
        return isMapped;
    }

    public void setMapped(boolean mapped) {
        isMapped = mapped;
    }

    private String project;
    private boolean isMapped;

    public String getCommitHash() {
        return commitHash;
    }

    public void setCommitHash(String commitHash) {
        this.commitHash = commitHash;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public int getCommitIndex() {
        return commitIndex;
    }

    public void setCommitIndex(int commitIndex) {
        this.commitIndex = commitIndex;
    }

    private String commitHash;
    private String parent;

    public String getDeveloper() {
        return developer;
    }

    public void setDeveloper(String developer) {
        this.developer = developer;
    }

    private String developer;

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    private String assetType;
    private int commitIndex;
    private double csvDev, ddev , comm , dcont , hdcont , ccc , accc , nloc ,dnfma , nfma , nff ;

    public String getAsset() {
        return asset;
    }

    public void setAsset(String asset) {
        this.asset = asset;
    }

    public double getCsvDev() {
        return csvDev;
    }

    public void setCsvDev(double csvDev) {
        this.csvDev = csvDev;
    }

    public double getDdev() {
        return ddev;
    }

    public void setDdev(double ddev) {
        this.ddev = ddev;
    }

    public double getComm() {
        return comm;
    }

    public void setComm(double comm) {
        this.comm = comm;
    }

    public double getDcont() {
        return dcont;
    }

    public void setDcont(double dcont) {
        this.dcont = dcont;
    }

    public double getHdcont() {
        return hdcont;
    }

    public void setHdcont(double hdcont) {
        this.hdcont = hdcont;
    }

    public double getCcc() {
        return ccc;
    }

    public void setCcc(double ccc) {
        this.ccc = ccc;
    }

    public double getAccc() {
        return accc;
    }

    public void setAccc(double accc) {
        this.accc = accc;
    }

    public double getNloc() {
        return nloc;
    }

    public void setNloc(double nloc) {
        this.nloc = nloc;
    }

    public double getDnfma() {
        return dnfma;
    }

    public void setDnfma(double dnfma) {
        this.dnfma = dnfma;
    }

    public double getNfma() {
        return nfma;
    }

    public void setNfma(double nfma) {
        this.nfma = nfma;
    }

    public double getNff() {
        return nff;
    }

    public void setNff(double nff) {
        this.nff = nff;
    }
}
