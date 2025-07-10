package se.gu.git;

import java.io.File;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Commit implements Serializable {
    private static final long serialVersionUID = -8871996632559277545L;
    private boolean isMergeCommit;
    private int commitIndex;

    public boolean isSkipCommit() {
        return skipCommit;
    }

    public void setSkipCommit(boolean skipCommit) {
        this.skipCommit = skipCommit;
    }

    private boolean skipCommit;

    public Commit(int commitId, String commitHash,int releaseId, String release, File projectRepository,boolean skip) {
        this.commitId = commitId;
        this.releaseId = releaseId;
        this.commitHash = commitHash;
        this.release = release;
        this.projectRepository = projectRepository;
        this.skipCommit = skip;
    }

    public int getCommitId() {
        return commitId;
    }

    public void setCommitId(int commitId) {
        this.commitId = commitId;
    }

    public int getReleaseId() {
        return releaseId;
    }

    public void setReleaseId(int releaseId) {
        this.releaseId = releaseId;
    }

    private int commitId,releaseId;

    public Commit(int commitIndex, String commitHash) {
        this.commitIndex = commitIndex;
        this.commitHash = commitHash;
    }

    public Commit() {
    }

    public Commit(String commitHash,String previousCommitHash, String release,File projectRepository) {
        this.commitHash = commitHash;
        this.previousCommitHash = previousCommitHash;
        this.release = release;
        this.projectRepository = projectRepository;
    }

    public boolean isMergeCommit() {
        return isMergeCommit;
    }

    public void setMergeCommit(boolean mergeCommit) {
        isMergeCommit = mergeCommit;
    }

    private String commitHash;

    public String getPreviousCommitHash() {
        return previousCommitHash;
    }

    public void setPreviousCommitHash(String previousCommitHash) {
        this.previousCommitHash = previousCommitHash;
    }

    private String previousCommitHash;

    public String getRelease() {
        return release;
    }

    public void setRelease(String release) {
        this.release = release;
    }

    private String release;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    private String message;
    private String author;

    public File getProject() {
        return projectRepository;
    }

    public void setProject(File projectRepository) {
        this.projectRepository = projectRepository;
    }

    private File projectRepository;
    private Date commitDate;

    public String getCommitHash() {
        return commitHash;
    }

    public void setCommitHash(String commitHash) {
        this.commitHash = commitHash;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Date getCommitDate() {
        return commitDate;
    }

    public void setCommitDate(Date commitDate) {
        this.commitDate = commitDate;
    }
    public void setCommitDate(String commitDate) throws ParseException {
        this.commitDate = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss").parse(commitDate);
    }
    public String getCommitDateAsString(){
        Calendar c = Calendar.getInstance();
        c.setTime(commitDate);
        return String.format("%d%d%d%d%d%d\n",c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH),c.get(Calendar.HOUR),c.get(Calendar.MINUTE),c.get(Calendar.SECOND));
    }

    public Long getCommitDateAsLong(){
        return commitDate.getTime();
    }

    @Override
    public String toString() {
        return String.format("%s | %s | %s | %s | %s",commitDate,commitHash,author,projectRepository,message);
    }

    public int getCommitIndex() {
        return commitIndex;
    }

    public void setCommitIndex(int commitIndex) {
        this.commitIndex = commitIndex;
    }
}
