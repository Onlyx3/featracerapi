package se.gu.ml.preprocessing;

import se.gu.assets.Asset;
import se.gu.metrics.MLAttribute;

import java.io.Serializable;

public class MetricValue  implements Serializable {
    private static final long serialVersionUID = 813652179671826846L;
    private MLAttribute mlAttribute;
    private double score;
    private Asset asset;

    public Asset getAsset() {
        return asset;
    }

    public void setAsset(Asset asset) {
        this.asset = asset;
    }

    public MetricValue(MLAttribute mlAttribute, double score) {
        this.mlAttribute = mlAttribute;
        this.score = score;
    }

    public MetricValue(MLAttribute mlAttribute, double score, Asset asset) {
        this.mlAttribute = mlAttribute;
        this.score = score;
        this.asset = asset;
    }

    public MLAttribute getMlAttribute() {
        return mlAttribute;
    }

    public void setMlAttribute(MLAttribute mlAttribute) {
        this.mlAttribute = mlAttribute;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}
