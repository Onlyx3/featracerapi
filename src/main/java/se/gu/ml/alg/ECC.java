package se.gu.ml.alg;

import java.util.Random;

import weka.classifiers.Classifier;
import weka.classifiers.trees.J48;
import mulan.classifier.transformation.EnsembleOfClassifierChains;


public class ECC extends EnsembleOfClassifierChains {

    /**
     *
     */
    private static final long serialVersionUID = -7367910050240734330L;
    public void setRand(Random r)
    {
        this.rand = r;
    }

    ECC()
    {
        super();
    }
    public ECC(int s)
    {
        super(new J48(), 10, false, true);
        Random r = new Random(s);
        setRand(r);
    }

    public  ECC(int s, boolean doUseSamplingWithReplacement)
    {
        super(new J48(), 10, false, doUseSamplingWithReplacement);
        Random r = new Random(s);
        setRand(r);
    }

    public ECC(Classifier classifier, int s)
    {
        super(classifier, 10, false, true);
        Random r = new Random(s);
        setRand(r);
    }

}
