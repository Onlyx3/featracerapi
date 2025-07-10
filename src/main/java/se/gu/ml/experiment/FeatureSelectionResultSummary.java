package se.gu.ml.experiment;

import java.io.Serializable;

public class FeatureSelectionResultSummary  implements Serializable {
    private static final long serialVersionUID = 298172711110389481L;
    private int index;
    private String attribute,project,is_balanced,is_normalized;
    private double IG_BR_rank,	IG_BR_weight,	IG_LP_rank,	IG_LP_weight,	IG_rank,	IG_weight,	RF_BR_rank,	RF_BR_weight,	RF_LP_rank,	RF_LP_weight,	RF_rank,	RF_weight,	overall_rank,	overall_weight;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getIs_balanced() {
        return is_balanced;
    }

    public void setIs_balanced(String is_balanced) {
        this.is_balanced = is_balanced;
    }

    public String getIs_normalized() {
        return is_normalized;
    }

    public void setIs_normalized(String is_normalized) {
        this.is_normalized = is_normalized;
    }

    public double getIG_BR_rank() {
        return IG_BR_rank;
    }

    public void setIG_BR_rank(double IG_BR_rank) {
        this.IG_BR_rank = IG_BR_rank;
    }

    public double getIG_BR_weight() {
        return IG_BR_weight;
    }

    public void setIG_BR_weight(double IG_BR_weight) {
        this.IG_BR_weight = IG_BR_weight;
    }

    public double getIG_LP_rank() {
        return IG_LP_rank;
    }

    public void setIG_LP_rank(double IG_LP_rank) {
        this.IG_LP_rank = IG_LP_rank;
    }

    public double getIG_LP_weight() {
        return IG_LP_weight;
    }

    public void setIG_LP_weight(double IG_LP_weight) {
        this.IG_LP_weight = IG_LP_weight;
    }

    public double getIG_rank() {
        return IG_rank;
    }

    public void setIG_rank(double IG_rank) {
        this.IG_rank = IG_rank;
    }

    public double getIG_weight() {
        return IG_weight;
    }

    public void setIG_weight(double IG_weight) {
        this.IG_weight = IG_weight;
    }

    public double getRF_BR_rank() {
        return RF_BR_rank;
    }

    public void setRF_BR_rank(double RF_BR_rank) {
        this.RF_BR_rank = RF_BR_rank;
    }

    public double getRF_BR_weight() {
        return RF_BR_weight;
    }

    public void setRF_BR_weight(double RF_BR_weight) {
        this.RF_BR_weight = RF_BR_weight;
    }

    public double getRF_LP_rank() {
        return RF_LP_rank;
    }

    public void setRF_LP_rank(double RF_LP_rank) {
        this.RF_LP_rank = RF_LP_rank;
    }

    public double getRF_LP_weight() {
        return RF_LP_weight;
    }

    public void setRF_LP_weight(double RF_LP_weight) {
        this.RF_LP_weight = RF_LP_weight;
    }

    public double getRF_rank() {
        return RF_rank;
    }

    public void setRF_rank(double RF_rank) {
        this.RF_rank = RF_rank;
    }

    public double getRF_weight() {
        return RF_weight;
    }

    public void setRF_weight(double RF_weight) {
        this.RF_weight = RF_weight;
    }

    public double getOverall_rank() {
        return overall_rank;
    }

    public void setOverall_rank(double overall_rank) {
        this.overall_rank = overall_rank;
    }

    public double getOverall_weight() {
        return overall_weight;
    }

    public void setOverall_weight(double overall_weight) {
        this.overall_weight = overall_weight;
    }

    public FeatureSelectionResultSummary() {
    }

    public FeatureSelectionResultSummary(int index, String attribute, String project, String is_balanced, String is_normalized, double IG_BR_rank, double IG_BR_weight, double IG_LP_rank, double IG_LP_weight, double IG_rank, double IG_weight, double RF_BR_rank, double RF_BR_weight, double RF_LP_rank, double RF_LP_weight, double RF_rank, double RF_weight, double overall_rank, double overall_weight) {
        this.index = index;
        this.attribute = attribute;
        this.project = project;
        this.is_balanced = is_balanced;
        this.is_normalized = is_normalized;
        this.IG_BR_rank = IG_BR_rank;
        this.IG_BR_weight = IG_BR_weight;
        this.IG_LP_rank = IG_LP_rank;
        this.IG_LP_weight = IG_LP_weight;
        this.IG_rank = IG_rank;
        this.IG_weight = IG_weight;
        this.RF_BR_rank = RF_BR_rank;
        this.RF_BR_weight = RF_BR_weight;
        this.RF_LP_rank = RF_LP_rank;
        this.RF_LP_weight = RF_LP_weight;
        this.RF_rank = RF_rank;
        this.RF_weight = RF_weight;
        this.overall_rank = overall_rank;
        this.overall_weight = overall_weight;
    }
}
