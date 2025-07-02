package se.gu.ml.preprocessing;

public enum NormalisationStrategy {
    FTR,//Featracer normalization strategy that uses (x-min)/(max-min)
    WEKA//uses weka's inbuilt filters to perform normalization
}
