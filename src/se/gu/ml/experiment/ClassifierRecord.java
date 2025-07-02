package se.gu.ml.experiment;

import mulan.classifier.MultiLabelLearnerBase;
import mulan.data.MultiLabelInstances;
import mulan.evaluation.measure.Measure;

import java.io.Serializable;
import java.util.List;

public class ClassifierRecord  implements Serializable {
    private static final long serialVersionUID = -6709256729514681416L;
    private String classifierName;

    public String getClassifierName() {
        return classifierName;
    }

    public void setClassifierName(String classifierName) {
        this.classifierName = classifierName;
    }

    private List<MultiLabelLearnerBase> classifierList;
    private List<Measure> measures;
    private MultiLabelInstances trainingSet,testSet;
    private int nFolds;

    public ClassifierRecord() {
    }

    public MultiLabelInstances getTestSet() {
        return testSet;
    }

    public void setTestSet(MultiLabelInstances testSet) {
        this.testSet = testSet;
    }

    public int getnFolds() {
        return nFolds;
    }

    public void setnFolds(int nFolds) {
        this.nFolds = nFolds;
    }

    public ClassifierRecord(List<MultiLabelLearnerBase> classifierList, List<Measure> measures, MultiLabelInstances trainingSet) {
        this.classifierList = classifierList;
        this.measures = measures;
        this.trainingSet = trainingSet;
    }

    public List<MultiLabelLearnerBase> getClassifierList() {
        return classifierList;
    }

    public void setClassifierList(List<MultiLabelLearnerBase> classifierList) {
        this.classifierList = classifierList;
    }

    public List<Measure> getMeasures() {
        return measures;
    }

    public void setMeasures(List<Measure> measures) {
        this.measures = measures;
    }

    public MultiLabelInstances getTrainingSet() {
        return trainingSet;
    }

    public void setTrainingSet(MultiLabelInstances trainingSet) {
        this.trainingSet = trainingSet;
    }
}
