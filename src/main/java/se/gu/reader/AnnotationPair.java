package se.gu.reader;

import se.gu.assets.AnnotationType;

import java.io.Serializable;

public class AnnotationPair  implements Serializable {
    private static final long serialVersionUID = 3279059965419264397L;
    private int startLine, endLine;
    private String featureName;
    private AnnotationType annotationType;

    public AnnotationPair(int startLine, int endLine, String featureName, AnnotationType annotationType) {
        this.startLine = startLine;
        this.endLine = endLine;
        this.featureName = featureName;
        this.annotationType = annotationType;
    }

    public AnnotationPair(int startLine,  String featureName, AnnotationType annotationType) {
        this.startLine = startLine;
        this.featureName = featureName;
        this.annotationType = annotationType;
    }
    public int getStartLine() {
        return startLine;
    }

    public void setStartLine(int startLine) {
        this.startLine = startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public void setEndLine(int endLine) {
        this.endLine = endLine;
    }

    public String getFeatureName() {
        return featureName;
    }

    public void setFeatureName(String featureName) {
        this.featureName = featureName;
    }

    public AnnotationType getAnnotationType() {
        return annotationType;
    }

    public void setAnnotationType(AnnotationType annotationType) {
        this.annotationType = annotationType;
    }

    @Override
    public String toString() {
        return String.format("%d-%d: %s", startLine, endLine, featureName);
    }
}
