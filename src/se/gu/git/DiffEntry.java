package se.gu.git;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DiffEntry implements Serializable {
    private static final long serialVersionUID = 2374798090641697088L;

    public String getAddedFileName() {
        return addedFileName;
    }

    public void setAddedFileName(String addedFileName) {
        this.addedFileName = addedFileName;
    }

    public String getAddedFullyQualifiedName() {
        return addedFullyQualifiedName;
    }

    public void setAddedFullyQualifiedName(String addedFullyQualifiedName) {
        this.addedFullyQualifiedName = addedFullyQualifiedName;
    }



    private String addedFileName;
    private String addedFullyQualifiedName;

    public String getDeletedFileName() {
        return deletedFileName;
    }

    public void setDeletedFileName(String deletedFileName) {
        this.deletedFileName = deletedFileName;
    }

    public String getDeletedFullyQualifiedName() {
        return deletedFullyQualifiedName;
    }

    public void setDeletedFullyQualifiedName(String deletedFullyQualifiedName) {
        this.deletedFullyQualifiedName = deletedFullyQualifiedName;
    }

    private String deletedFileName;
    private String deletedFullyQualifiedName;

    public int getTotalLinesDeleted() {
        return codeChanges.stream().map(CodeChange::getDeletedLines).reduce((a,b)->a+b).get();
    }

    public void setTotalLinesDeleted(int totalLinesDeleted) {
        this.totalLinesDeleted = totalLinesDeleted;
    }

    public int getTotalLinesAdded() {
        return codeChanges.stream().map(CodeChange::getAddedLines).reduce((a,b)->a+b).get();
    }

    public void setTotalLinesAdded(int totalLinesAdded) {
        this.totalLinesAdded = totalLinesAdded;
    }

    private int totalLinesDeleted,totalLinesAdded;

    public List<CodeChange> getCodeChanges() {
        return codeChanges;
    }

    public void setCodeChanges(List<CodeChange> codeChanges) {
        this.codeChanges = codeChanges;
    }

    private List<CodeChange> codeChanges;

    public DiffEntry() {
        codeChanges = new ArrayList<>();
        deletedLines=new ArrayList<>();
        addedLines = new ArrayList<>();
    }

    private List<String> deletedLines,addedLines;

    public List<String> getDeletedLines() {
        return deletedLines;
    }

    public void setDeletedLines(List<String> deletedLines) {
        this.deletedLines = deletedLines;
    }

    public List<String> getAddedLines() {
        return addedLines;
    }

    public void setAddedLines(List<String> addedLines) {
        this.addedLines = addedLines;
    }
}

