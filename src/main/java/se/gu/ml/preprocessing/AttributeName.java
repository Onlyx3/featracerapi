package se.gu.ml.preprocessing;

import java.io.Serializable;

public class AttributeName  implements Serializable {
    private static final long serialVersionUID = -3679621677362907466L;
    private String name;
    private boolean isMLFeature;

    public AttributeName(String name, boolean isMLFeature) {
        this.name = name;
        this.isMLFeature = isMLFeature;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isMLFeature() {
        return isMLFeature;
    }

    public void setMLFeature(boolean MLFeature) {
        isMLFeature = MLFeature;
    }
}
