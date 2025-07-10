package se.gu.ml.experiment;

public class PredictionResult {
    public int getrCommit() {
        return rCommit;
    }

    public void setrCommit(int rCommit) {
        this.rCommit = rCommit;
    }

    public int getCommitIndex() {
        return commitIndex;
    }

    public void setCommitIndex(int commitIndex) {
        this.commitIndex = commitIndex;
    }

    public String getCommit() {
        return commit;
    }

    public void setCommit(String commit) {
        this.commit = commit;
    }

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public String getMeasure() {
        return measure;
    }

    public void setMeasure(String measure) {
        this.measure = measure;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public double getMeasureValue() {
        return measureValue;
    }

    public void setMeasureValue(double measureValue) {
        this.measureValue = measureValue;
    }

    private int rCommit,commitIndex;
    private String commit,classifier,measure,project;
    private double measureValue;

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    private String level;

    public double getAvg() {
        return avg;
    }

    public void setAvg(double avg) {
        this.avg = avg;
    }

    public double getMed() {
        return med;
    }

    public void setMed(double med) {
        this.med = med;
    }

    private double avg,med;

    //instaniate a result from the line items: commitIndex;commit;classifier;measure;measureValue;project
    public PredictionResult(String resultLine) {
        String[]items = resultLine.split(";");
        commitIndex = Integer.parseInt(items[0].trim());
        commit=items[1].trim();
        classifier=items[2].trim();
        measure=items[3].trim();
        measureValue = Double.parseDouble(items[4].trim());
        project = items[7].trim();
    }
    public PredictionResult(String resultLine,String project,String level) {
        String[]items = resultLine.split(";");
        commitIndex = Integer.parseInt(items[0].trim());
        commit=items[1].trim();
        classifier=items[2].trim();
        measure=items[3].trim();
        measureValue = Double.parseDouble(items[4].trim());
        this.project = project;
        this.level=level;

    }

    public PredictionResult(String classifier, String measure, String project, String level, double avg, double med) {
        this.classifier = classifier;
        this.measure = measure;
        this.project = project;
        this.level = level;
        this.avg = avg;
        this.med = med;
    }
}
