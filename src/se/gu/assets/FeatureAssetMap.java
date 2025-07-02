package se.gu.assets;

import se.gu.parser.fmparser.FeatureTreeNode;

import java.io.Serializable;

public class FeatureAssetMap implements Serializable {


    private static final long serialVersionUID = -517860562507314424L;

    private int tangled;

    public int getTangled() {
        return tangled;
    }

    public void setTangled(int tangled) {
        this.tangled = tangled;
    }

    public AnnotationType getAnnotationType() {
        return annotationType;
    }

    public void setAnnotationType(AnnotationType annotationType) {
        this.annotationType = annotationType;
    }

    private AnnotationType annotationType;

    public String getFeatureName() {
        return featureName;
    }

    public void setFeatureName(String featureName) {
        this.featureName = featureName;
    }

    private String featureName;

    public FeatureAssetMap(String mappedFeature, Asset mappedAsset, AnnotationType annotationType) {
        this.annotationType = annotationType;
        this.featureName = mappedFeature;
        this.mappedAsset = mappedAsset;
    }
    public FeatureAssetMap(String mappedFeature, Asset mappedAsset, AnnotationType annotationType,int tangled) {
        this.annotationType = annotationType;
        this.featureName = mappedFeature;
        this.mappedAsset = mappedAsset;
        this.tangled = tangled;
    }
    private FeatureTreeNode mappedFeature;

    public FeatureTreeNode getMappedFeature() {
        return mappedFeature;
    }

    public void setMappedFeature(FeatureTreeNode mappedFeature) {
        this.mappedFeature = mappedFeature;
    }

    private Asset mappedAsset;

    public Asset getMappedAsset() {
        return mappedAsset;
    }

    public void setMappedAsset(Asset mappedAsset) {
        this.mappedAsset = mappedAsset;
    }

    @Override
    public String toString() {
        return String.format("%s -> %s [%s]", mappedAsset.getFullyQualifiedName(), featureName, getAnnotationType());
    }

    @Override
    public boolean equals(Object obj) {
        if(obj==null && this!=null){
            return false;
        }else if(obj==null&&this==null){
            return  true;
        }else {
            FeatureAssetMap mp = (FeatureAssetMap)obj;
            return this.getMappedAsset().getFullyQualifiedName().equalsIgnoreCase(mp.getMappedAsset().getFullyQualifiedName())&&
                    this.getFeatureName().equalsIgnoreCase(mp.getFeatureName())&&
                    this.getAnnotationType()==mp.getAnnotationType();
        }

    }
}
