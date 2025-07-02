package se.gu.ml.experiment;

import java.io.Serializable;

public class PredictionMeasure  implements Serializable {
    private static final long serialVersionUID = -6617063941166521337L;

    public PredictionMeasure(double precision, double recall, double fmeasure) {
        this.precision = precision;
        this.recall = recall;
        this.fmeasure = fmeasure;
    }

    private double precision,recall,fmeasure;

    public double getPrecision() {
        return precision;
    }

    public void setPrecision(double precision) {
        this.precision = precision;
    }

    public double getRecall() {
        return recall;
    }

    public void setRecall(double recall) {
        this.recall = recall;
    }

    public double getFmeasure() {
        return fmeasure;
    }

    public void setFmeasure(double fmeasure) {
        this.fmeasure = fmeasure;
    }
}
