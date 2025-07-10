package se.gu.metrics.structure;

import se.gu.assets.Asset;
import se.gu.parser.fmparser.FeatureTreeNode;

import java.io.Serializable;

public class AssetPairwiseComparison implements Serializable {
    private static final long serialVersionUID = 124857000167850976L;
    private Asset sourceAsset,targetAsset;
   private FeatureTreeNode feature;

    public FeatureTreeNode getFeature() {
        return feature;
    }

    public void setFeature(FeatureTreeNode feature) {
        this.feature = feature;
    }

    private double ged,sld,csm;

    public AssetPairwiseComparison(Asset sourceAsset, Asset targetAsset, FeatureTreeNode feature) {
        this.sourceAsset = sourceAsset;
        this.targetAsset = targetAsset;
        this.feature = feature;
        ged = -10.0;
        csm=-10.0;
        sld = -10.0;
    }

    public Asset getSourceAsset() {
        return sourceAsset;
    }

    public void setSourceAsset(Asset sourceAsset) {
        this.sourceAsset = sourceAsset;
    }

    public Asset getTargetAsset() {
        return targetAsset;
    }

    public void setTargetAsset(Asset targetAsset) {
        this.targetAsset = targetAsset;
    }

    public double getGed() {
        return ged;
    }

    public void setGed(double ged) {
        this.ged = ged;
    }

    public double getSld() {
        return sld;
    }

    public void setSld(double sld) {
        this.sld = sld;
    }

    public double getCsm() {
        return csm;
    }

    public void setCsm(double csm) {
        this.csm = csm;
    }

    @Override
    public String toString() {
        return "\nAssetPairwiseComparison{" +
                "SA=" + sourceAsset +
                ", TA=" + targetAsset +
                ", feature=" + feature +
                ", ged=" + ged +
                ", sld=" + sld +
                ", csm=" + csm +
                '}';
    }
}
