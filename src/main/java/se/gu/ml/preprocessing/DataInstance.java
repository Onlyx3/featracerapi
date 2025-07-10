package se.gu.ml.preprocessing;

import java.io.Serializable;
import java.util.Map;

public class DataInstance implements Serializable {
    private static final long serialVersionUID = -2581160379538195672L;
    private Map<String,Double> attributeValues;

    public DataInstance(Map<String, Double> attributeValues, String assetName) {
        this.attributeValues = attributeValues;
        this.assetName = assetName;
    }

    public Map<String, Double> getAttributeValues() {
        return attributeValues;
    }

    public void setAttributeValues(Map<String, Double> attributeValues) {
        this.attributeValues = attributeValues;
    }


    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    private String assetName;





    @Override
    public String toString() {
        return assetName;
    }


}
