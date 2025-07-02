package se.gu.ml.experiment;

public enum TopNFeaturesMethod {
    Percent,//select top p percent features
    KBest, //top K best features
    Threshold //top features >= to specified threshhold
}
