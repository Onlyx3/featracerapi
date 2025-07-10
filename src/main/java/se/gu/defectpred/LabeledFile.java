package se.gu.defectpred;

public class LabeledFile {
    public LabeledFile(String project, String release_number, String file, String label, String predicted) {
        this.project = project;
        this.release_number = release_number;
        this.file = file;
        this.label = label;
        this.predicted = predicted;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getRelease_number() {
        return release_number;
    }

    public void setRelease_number(String release_number) {
        this.release_number = release_number;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getPredicted() {
        return predicted;
    }

    public void setPredicted(String predicted) {
        this.predicted = predicted;
    }

    private String project,	release_number, file,label,	predicted;
}
