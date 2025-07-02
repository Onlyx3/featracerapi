package se.gu.ml.preprocessing;

import java.io.Serializable;

public class NestingDepthPair implements Serializable {

    private static final long serialVersionUID = -2036643959587806846L;
    private String assetName,featureName;
    int nestingDepth;

    public NestingDepthPair(String assetName, String featureName, int nestingDepth) {
        this.assetName = assetName;
        this.featureName = featureName;
        this.nestingDepth = nestingDepth;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getFeatureName() {
        return featureName;
    }

    public void setFeatureName(String featureName) {
        this.featureName = featureName;
    }

    public int getNestingDepth() {
        return nestingDepth;
    }

    public void setNestingDepth(int nestingDepth) {
        this.nestingDepth = nestingDepth;
    }
}
