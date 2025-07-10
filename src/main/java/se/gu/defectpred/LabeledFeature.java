package se.gu.defectpred;

public class LabeledFeature {
    public LabeledFeature(String project, String release_number, String feature, String label, String predicted) {
        this.project = project;
        this.release_number = release_number;
        this.feature = feature;
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

    public String getFeature() {
        return feature;
    }

    public void setFeature(String feature) {
        this.feature = feature;
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

    private String project,	release_number,	feature,label,	predicted;
    private double fcomm,fadev,fddev,fexp,foexp,	fmodd,	fnloc,	fcyco,	faddl,	freml,	scat,	tanga,	ndep,	lofc;

}
