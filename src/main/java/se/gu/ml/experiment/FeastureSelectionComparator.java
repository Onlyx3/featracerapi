package se.gu.ml.experiment;

import java.io.Serializable;
import java.util.Comparator;

public class FeastureSelectionComparator implements Comparator<FeatureSelectionResultSummary>, Serializable {
    private static final long serialVersionUID = -1999180457993880528L;
    private FeatureSelectionRank featureSelectionRank;
    private TopNFeaturesMethod topNFeaturesMethod;

    public FeastureSelectionComparator(FeatureSelectionRank featureSelectionRank,TopNFeaturesMethod topNFeaturesMethod) {
        this.featureSelectionRank = featureSelectionRank;
        this.topNFeaturesMethod = topNFeaturesMethod;
    }

    @Override
    public int compare(FeatureSelectionResultSummary o1, FeatureSelectionResultSummary o2) {
        boolean topNIsThreshold = topNFeaturesMethod == TopNFeaturesMethod.Threshold;

        if(featureSelectionRank == FeatureSelectionRank.IG_rank){
            return topNIsThreshold?Double.compare(o1.getIG_weight(),o2.getIG_weight()): Double.compare(o1.getIG_rank(),o2.getIG_rank());
        }else if(featureSelectionRank == FeatureSelectionRank.IG_BR_rank){
            return topNIsThreshold?Double.compare(o1.getIG_BR_weight(),o2.getIG_BR_weight()):Double.compare(o1.getIG_BR_rank(),o2.getIG_BR_rank());
        }
        else if(featureSelectionRank == FeatureSelectionRank.IG_LP_rank){
            return topNIsThreshold?Double.compare(o1.getIG_LP_weight(),o2.getIG_LP_weight()):Double.compare(o1.getIG_LP_rank(),o2.getIG_LP_rank());
        }
        else if(featureSelectionRank == FeatureSelectionRank.RF_rank){
            return topNIsThreshold?Double.compare(o1.getRF_weight(),o2.getRF_weight()):Double.compare(o1.getRF_rank(),o2.getRF_rank());
        }else if(featureSelectionRank == FeatureSelectionRank.RF_BR_rank){
            return topNIsThreshold?Double.compare(o1.getRF_BR_weight(),o2.getRF_BR_weight()):Double.compare(o1.getRF_BR_rank(),o2.getRF_BR_rank());
        }
        else if(featureSelectionRank == FeatureSelectionRank.RF_LP_rank){
            return topNIsThreshold?Double.compare(o1.getRF_LP_weight(),o2.getRF_LP_weight()):Double.compare(o1.getRF_LP_rank(),o2.getRF_LP_rank());
        }else {
            return topNIsThreshold?Double.compare(o1.getOverall_weight(),o2.getOverall_weight()):Double.compare(o1.getOverall_rank(),o2.getOverall_rank());
        }

    }
}
