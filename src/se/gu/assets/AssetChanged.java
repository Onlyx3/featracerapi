package se.gu.assets;

import java.io.Serializable;
import java.util.List;

public class AssetChanged implements Serializable {
    private static final long serialVersionUID = -2636602997680237881L;

    public AssetChanged(String fileName, String fileRelativePath, List<Integer> linesChanged, String commitHash, String author) {
        this.fileName = fileName;
        //this.lineNumber = lineNumber;
        this.commitHash = commitHash;
        this.fileRelativePath = fileRelativePath;
        this.author = author;
        this.linesChanged = linesChanged;
    }

    private List<Integer> linesChanged;

    public List<Integer> getLinesChanged() {
        return linesChanged;
    }

    public void setLinesChanged(List<Integer> linesChanged) {
        this.linesChanged = linesChanged;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public void setCommitHash(String commitHash) {
        this.commitHash = commitHash;
    }

    private String fileName;

    public String getFileRelativePath() {
        return fileRelativePath;
    }

    public void setFileRelativePath(String fileRelativePath) {
        this.fileRelativePath = fileRelativePath;
    }

    private String fileRelativePath;

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    private String author;
    private int lineNumber;
    private String commitHash;

    @Override
    public String toString() {
        return String.format("%s, %s, %s",fileRelativePath,commitHash,author);
    }
}
