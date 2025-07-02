package se.gu.ml.experiment;

import java.io.Serializable;

public class FeatureSelectionResult  implements Serializable {
    private static final long serialVersionUID = 2316448228299212594L;
    private String evalMethod;

    public String getEvalMethodParent() {
        return evalMethodParent;
    }

    public void setEvalMethodParent(String evalMethodParent) {
        this.evalMethodParent = evalMethodParent;
    }

    private String evalMethodParent;

    public FeatureSelectionResult(String evalMethodParent,String evalMethod, String attribute, double weight, int rank) {
        this.evalMethodParent = evalMethodParent;
        this.evalMethod = evalMethod;
        this.attribute = attribute;
        this.weight = weight;
        this.rank = rank;
    }

    private String attribute;
    private double weight;
    private int rank;

    public String getEvalMethod() {
        return evalMethod;
    }

    public void setEvalMethod(String evalMethod) {
        this.evalMethod = evalMethod;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }
}
