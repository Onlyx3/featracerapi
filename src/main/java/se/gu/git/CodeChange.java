package se.gu.git;

import java.io.Serializable;

public class CodeChange implements Serializable {
    private int deletedLinesStart,deletedLines,addedLinesStart,addedLines;
    public int getDeletedLinesStart() {
        return deletedLinesStart;
    }

    public void setDeletedLinesStart(int deletedLinesStart) {
        this.deletedLinesStart = deletedLinesStart;
    }

    public int getDeletedLines() {
        return deletedLines;
    }

    public void setDeletedLines(int deletedLines) {
        this.deletedLines = deletedLines;
    }

    public int getAddedLinesStart() {
        return addedLinesStart;
    }

    public void setAddedLinesStart(int addedLinesStart) {
        this.addedLinesStart = addedLinesStart;
    }

    public int getAddedLines() {
        return addedLines;
    }

    public void setAddedLines(int addedLines) {
        this.addedLines = addedLines;
    }
}
