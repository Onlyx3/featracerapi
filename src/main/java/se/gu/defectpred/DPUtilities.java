package se.gu.defectpred;

import weka.core.Instances;
import weka.core.converters.ConverterUtils;
import weka.filters.Filter;
import weka.filters.supervised.instance.SMOTE;
import weka.filters.unsupervised.attribute.Normalize;

public class DPUtilities {
    public static Instances getTrainingDataset(String trainingFile, boolean normalize, boolean balance) throws Exception {

        ConverterUtils.DataSource source = new ConverterUtils.DataSource(trainingFile);
        Instances trainingData = source.getDataSet();
        trainingData.setClassIndex(trainingData.numAttributes() - 1);
        //if normalization or balancing failes, it shouldn't affect the whole prediction.
        if (normalize) {
            try {
                Normalize normalizer = new Normalize();
                normalizer.setInputFormat(trainingData);
                trainingData = Filter.useFilter(trainingData, normalizer);
            } catch (Exception ex) {

            }
        }
        if (balance) {
            try {
                SMOTE smote = new SMOTE();
                smote.setInputFormat(trainingData);
                trainingData = Filter.useFilter(trainingData, smote);
            } catch (Exception ex) {

            }
        }
        return trainingData;

    }
}
