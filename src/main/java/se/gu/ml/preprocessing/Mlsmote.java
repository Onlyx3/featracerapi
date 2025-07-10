package se.gu.ml.preprocessing;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import mulan.data.InvalidDataFormatException;
import mulan.data.LabelSet;
import mulan.data.MultiLabelInstances;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

public class Mlsmote  implements Serializable {
    private static final long serialVersionUID = -154577585666328866L;
    private Map<String, Object> parameter;
    private Mlsmote.MLSBag myBag;

    private  String inpath,outpath,fileext,xml;
    private int labelCombination;
    public Mlsmote() {
    }

    public void execute(String folderWithARRF,String outputFolder,String extensions,String xmlLabelsFile,int labelSets ) {
        inpath = folderWithARRF;
        outpath = outputFolder;
        fileext = extensions;
        xml = xmlLabelsFile;
        labelCombination = labelSets;
        run();
    }

    private void run() {
        this.parameter = Mlsmote.MLSUtils.readParameters(inpath,outpath,fileext,xml,labelCombination);

        try {
            File[] files = Mlsmote.MLSUtils.getFilenames(inpath, fileext);
            String xmlFile = xml;
            File[] arr$ = files;
            int len$ = files.length;

            for(int i$ = 0; i$ < len$; ++i$) {
                File dataset = arr$[i$];
                Mlsmote.MLSUtils.DEBUG("Processing " + dataset.getAbsolutePath() + " -> " + xmlFile);
                this.myBag = new Mlsmote.MLSBag(new MultiLabelInstances(dataset.getAbsolutePath(), xmlFile), this.parameter);
                (new Mlsmote.MlSmote(this.myBag)).applyMethod();
                Mlsmote.MLSUtils.saveResult(dataset, this.myBag.getMlDS());
            }
        } catch (IOException var8) {
            var8.printStackTrace();
        } catch (InvalidDataFormatException var9) {
            var9.printStackTrace();
        } catch (Exception var10) {
            var10.printStackTrace();
        }

    }

    private class MlSmote {
        public static final int LC_INTERSECTION = 1;
        public static final int LC_UNION = 2;
        public static final int LC_RANKING = 3;
        private Mlsmote.MLSBag myBag;
        private Map vdmMap;
        protected int nearestNeighbors = 5;
        protected int m_RandomSeed = 1;
        protected double m_Percentage = 100.0D;

        public MlSmote(Mlsmote.MLSBag bag) {
            this.myBag = bag;
        }

        public MlSmote(Mlsmote.MLSBag bag, int nearestNeighbors) {
            this.myBag = bag;
            this.nearestNeighbors = nearestNeighbors;
        }

        public int getNearestNeighbors() {
            return this.nearestNeighbors;
        }

        public void setNearestNeighbors(int nearestNeighbors) {
            this.nearestNeighbors = nearestNeighbors;
        }

        public int getRandomSeed() {
            return this.m_RandomSeed;
        }

        public void setRandomSeed(int m_RandomSeed) {
            this.m_RandomSeed = m_RandomSeed;
        }

        public double getPercentage() {
            return this.m_Percentage;
        }

        public void setPercentage(double m_Percentage) {
            this.m_Percentage = m_Percentage;
        }

        public void applyMethod() {
            List<Integer> minIndexes = this.myBag.getMinorClasses();
            MultiLabelInstances tmpDS = this.myBag.getMlDS().clone();
            Instances outDS = tmpDS.getDataSet();
            Instances mliDS = this.myBag.getMlDS().getDataSet();
            int[] counters = this.myBag.getCounters(mliDS);
            Iterator i$ = minIndexes.iterator();

            while(true) {
                while(i$.hasNext()) {
                    int index = (Integer)i$.next();
                    int min = counters[index] - 1;
                    int nearestNeighbors = min < this.getNearestNeighbors() ? min : this.getNearestNeighbors();
                    if (nearestNeighbors < 1) {
                        Mlsmote.MLSUtils.DEBUG("Cannot use 0 neighbors of label " + index);
                    } else {
                        Instances sample = this.myBag.getBagOfLabel(index);
                        mliDS.addAll(sample);
                        this.vdmMap = this.getVdmMap(index);
                        Random rand = new Random((long)this.getRandomSeed());
                        Set extraIndexSet = this.getExtraIndexSet(sample, rand);
                        Instance[] nnArray = new Instance[nearestNeighbors];

                        for(int i = 0; i < sample.numInstances(); ++i) {
                            if (Mlsmote.MLSUtils.uniformIRSet()) {
                                double labelIR = (double)this.myBag.getMaxCounter() / (double)counters[index];
                                if (labelIR <= this.myBag.getMeanIR()) {
                                    i = sample.numInstances() + 1;
                                    break;
                                }
                            }

                            Instance instanceI = sample.instance(i);
                            List distanceToInstance = this.getDistanceToInstance(instanceI, sample, i);
                            Iterator entryIterator = distanceToInstance.iterator();

                            for(int j = 0; entryIterator.hasNext() && j < nearestNeighbors; ++j) {
                                nnArray[j] = (Instance)((Object[])((Object[])entryIterator.next()))[1];
                            }

                            int var10002;
                            for(int n = (int)Math.floor(this.getPercentage() / 100.0D); n > 0 || extraIndexSet.remove(i); var10002 = counters[index]++) {
                                double[] values = new double[sample.numAttributes()];
                                int nn = rand.nextInt(nearestNeighbors);
                                Enumeration attrEnum = mliDS.enumerateAttributes();

                                while(attrEnum.hasMoreElements()) {
                                    Attribute attr = (Attribute)attrEnum.nextElement();
                                    if (attr.index() < this.myBag.getMlDS().getLabelIndices()[0]) {
                                        values[attr.index()] = this.getSyntheticValue(attr, instanceI, nnArray, rand, nn, nearestNeighbors);
                                    }
                                }

                                this.setSyntheticLabels(instanceI, nnArray, values);
                                Instance synthetic = new DenseInstance(1.0D, values);
                                outDS.add(synthetic);
                                --n;
                            }
                        }
                    }
                }

                this.myBag.setMlDS(tmpDS);
                return;
            }
        }

        private void setSyntheticLabels(Instance instanceI, Instance[] nnArray, double[] values) {
            int[] labelIndices = this.myBag.getMlDS().getLabelIndices();
            Instance[] sample = new Instance[nnArray.length + 1];

            int idxLabel;
            for(idxLabel = 0; idxLabel < nnArray.length; ++idxLabel) {
                sample[idxLabel] = nnArray[idxLabel];
            }

            sample[nnArray.length] = instanceI;
            int[] labels;
            switch((Integer)this.myBag.getParameter().get("labelCombination")) {
                case 1:
                default:
                    labels = this.getLabelIntersection(sample);
                    break;
                case 2:
                    labels = this.getLabelUnion(sample);
                    break;
                case 3:
                    labels = this.getLabelRanking(sample);
            }

            idxLabel = 0;
            int[] arr$ = labelIndices;
            int len$ = labelIndices.length;

            for(int i$ = 0; i$ < len$; ++i$) {
                int index = arr$[i$];
                values[index] = (double)labels[idxLabel++];
            }

        }

        private double getSyntheticValue(Attribute attr, Instance instanceI, Instance[] nnArray, Random rand, int nn, int nearestNeighbors) {
            double dif;
            double gap;
            if (attr.isNumeric()) {
                dif = nnArray[nn].value(attr) - instanceI.value(attr);
                gap = rand.nextDouble();
                return instanceI.value(attr) + gap * dif;
            } else if (attr.isDate()) {
                dif = nnArray[nn].value(attr) - instanceI.value(attr);
                gap = rand.nextDouble();
                return (double)((long)(instanceI.value(attr) + gap * dif));
            } else {
                int[] valueCounts = new int[attr.numValues()];
                int iVal = (int)instanceI.value(attr);
                int var10002 = valueCounts[iVal]++;

                int maxIndex;
                int max;
                for(maxIndex = 0; maxIndex < nearestNeighbors; ++maxIndex) {
                    max = (int)nnArray[maxIndex].value(attr);
                    var10002 = valueCounts[max]++;
                }

                maxIndex = 0;
                max = -2147483648;

                for(int idx = 0; idx < attr.numValues(); ++idx) {
                    if (valueCounts[idx] > max) {
                        max = valueCounts[idx];
                        maxIndex = idx;
                    }
                }

                return (double)maxIndex;
            }
        }

        private List getDistanceToInstance(Instance instanceI, Instances sample, int i) {
            List distanceToInstance = new LinkedList();
            Instances mliDS = this.myBag.getMlDS().getDataSet();

            for(int j = 0; j < sample.numInstances(); ++j) {
                Instance instanceJ = sample.instance(j);
                if (i != j) {
                    double distance = 0.0D;
                    Enumeration attrEnum = mliDS.enumerateAttributes();

                    while(attrEnum.hasMoreElements()) {
                        Attribute attr = (Attribute)attrEnum.nextElement();
                        if (attr.index() < this.myBag.getMlDS().getLabelIndices()[0]) {
                            double iVal = instanceI.value(attr);
                            double jVal = instanceJ.value(attr);
                            if (attr.isNumeric()) {
                                distance += Math.pow(iVal - jVal, 2.0D);
                            } else {
                                distance += ((double[][])((double[][])this.vdmMap.get(attr)))[(int)iVal][(int)jVal];
                            }
                        }
                    }

                    distance = Math.pow(distance, 0.5D);
                    distanceToInstance.add(new Object[]{distance, instanceJ});
                }
            }

            Collections.sort(distanceToInstance, new Comparator() {
                public int compare(Object o1, Object o2) {
                    double distance1 = (Double)((Object[])((Object[])o1))[0];
                    double distance2 = (Double)((Object[])((Object[])o2))[0];
                    return Double.compare(distance1, distance2);
                }
            });
            return distanceToInstance;
        }

        private Set getExtraIndexSet(Instances sample, Random rand) {
            List extraIndicesx = new LinkedList();
            double percentageRemainder = this.getPercentage() / 100.0D - Math.floor(this.getPercentage() / 100.0D);
            int extraIndicesCount = (int)(percentageRemainder * (double)sample.numInstances());
            if (extraIndicesCount >= 1) {
                for(int i = 0; i < sample.numInstances(); ++i) {
                    extraIndicesx.add(i);
                }
            }

            Collections.shuffle(extraIndicesx, rand);
            List extraIndices = extraIndicesx.subList(0, extraIndicesCount);
            return new HashSet(extraIndices);
        }

        private Map getVdmMap(int index) {
            Map vdmMap = new HashMap();
            Instances mliDS = this.myBag.getMlDS().getDataSet();
            Enumeration attrEnum = mliDS.enumerateAttributes();
            Attribute labelAttr = mliDS.attribute(this.myBag.getMlDS().getLabelIndices()[index]);

            while(true) {
                Attribute attr;
                do {
                    do {
                        if (!attrEnum.hasMoreElements()) {
                            return vdmMap;
                        }

                        attr = (Attribute)attrEnum.nextElement();
                    } while(attr.index() >= this.myBag.getMlDS().getLabelIndices()[0]);
                } while(!attr.isNominal() && !attr.isString());

                double[][] vdm = new double[attr.numValues()][attr.numValues()];
                vdmMap.put(attr, vdm);
                int[] featureValueCounts = new int[attr.numValues()];
                int[][] featureValueCountsByClass = new int[2][attr.numValues()];

                int valueIndex2;
                int classValue;
                int var10002;
                for(Enumeration instanceEnum = mliDS.enumerateInstances(); instanceEnum.hasMoreElements(); var10002 = featureValueCountsByClass[classValue][valueIndex2]++) {
                    Instance instance = (Instance)instanceEnum.nextElement();
                    valueIndex2 = (int)instance.value(attr);
                    classValue = (int)instance.value(labelAttr);
                    var10002 = featureValueCounts[valueIndex2]++;
                }

                for(int valueIndex1 = 0; valueIndex1 < attr.numValues(); ++valueIndex1) {
                    for(valueIndex2 = 0; valueIndex2 < attr.numValues(); ++valueIndex2) {
                        double sum = 0.0D;

                        for(int classValueIndex = 0; classValueIndex < 2; ++classValueIndex) {
                            double c1i = (double)featureValueCountsByClass[classValueIndex][valueIndex1];
                            double c2i = (double)featureValueCountsByClass[classValueIndex][valueIndex2];
                            double c1 = (double)featureValueCounts[valueIndex1];
                            double c2 = (double)featureValueCounts[valueIndex2];
                            double term1 = c1i / c1;
                            double term2 = c2i / c2;
                            sum += Math.abs(term1 - term2);
                        }

                        vdm[valueIndex1][valueIndex2] = sum;
                    }
                }
            }
        }

        private int[] getLabelRanking(Instance[] bag) {
            int numInstances = bag.length;
            int numLabels = this.myBag.getMlDS().getNumLabels();
            int[] labelIndices = this.myBag.getMlDS().getLabelIndices();
            int[] labelCount = new int[numLabels];

            int threshold;
            int j;
            for(threshold = 0; threshold < numInstances; ++threshold) {
                for(j = 0; j < numLabels; ++j) {
                    if (bag[threshold].stringValue(labelIndices[j]).equals("1")) {
                        int var10002 = labelCount[j]++;
                    }
                }
            }

            threshold = (int)((double)bag.length * 0.5D);

            for(j = 0; j < numLabels; ++j) {
                labelCount[j] = labelCount[j] >= threshold ? 1 : 0;
            }

            return labelCount;
        }

        private int[] getLabelUnion(Instance[] bag) {
            int numInstances = bag.length;
            int numLabels = this.myBag.getMlDS().getNumLabels();
            int[] labelIndices = this.myBag.getMlDS().getLabelIndices();
            int[] union = new int[numLabels];
            int[] dblLabels = new int[numLabels];

            for(int i = 0; i < numInstances; ++i) {
                for(int j = 0; j < numLabels; ++j) {
                    if (bag[i].stringValue(labelIndices[j]).equals("1")) {
                        dblLabels[j] = 1;
                    } else {
                        dblLabels[j] = 0;
                    }
                }

                this.unionLabelSet(union, dblLabels);
            }

            return union;
        }

        private void unionLabelSet(int[] ls1, int[] ls2) {
            for(int index = 0; index < ls1.length; ++index) {
                ls1[index] = ls1[index] != 1 && ls2[index] != 1 ? 0 : 1;
            }

        }

        private int[] getLabelIntersection(Instance[] bag) {
            int numInstances = bag.length;
            int numLabels = this.myBag.getMlDS().getNumLabels();
            int[] labelIndices = this.myBag.getMlDS().getLabelIndices();
            LabelSet intersection = null;

            int j;
            for(int i = 0; i < numInstances; ++i) {
                double[] dblLabelsx = new double[numLabels];

                for(j = 0; j < numLabels; ++j) {
                    try {
                        if (bag[i].stringValue(labelIndices[j]).equals("1")) {
                            dblLabelsx[j] = 1.0D;
                        } else {
                            dblLabelsx[j] = 0.0D;
                        }
                    } catch (NullPointerException var10) {
                        dblLabelsx[j] = 0.0D;
                    }
                }

                LabelSet labelSet = new LabelSet(dblLabelsx);
                intersection = intersection == null ? labelSet : LabelSet.intersection(intersection, labelSet);
            }

            double[] dblLabels = intersection.toDoubleArray();
            int[] labels = new int[numLabels];

            for(j = 0; j < dblLabels.length; ++j) {
                labels[j] = (int)dblLabels[j];
            }

            return labels;
        }
    }

    private class MLSBag {
        private MultiLabelInstances mlDS;
        private Map<String, Object> parameter;
        private List<Integer> majorClasses;
        private List<Integer> minorClasses;
        private double[] labelFrequencies;
        private HashMap<Double, Integer> freqIndex;
        private double mean;
        private double std;
        private double meanIR;
        private double[] labelIR;
        private int maxCounter;
        private int minCounter;

        public MultiLabelInstances getMlDS() {
            return this.mlDS;
        }

        public void setMlDS(MultiLabelInstances mlDS) {
            this.mlDS = mlDS;
        }

        public int getMaxCounter() {
            return this.maxCounter;
        }

        public double getMeanIR() {
            return this.meanIR;
        }

        public Map<String, Object> getParameter() {
            return this.parameter;
        }

        public double[] getLabelFreq() {
            return this.labelFrequencies;
        }

        public HashMap<Double, Integer> getFreqIndex() {
            return this.freqIndex;
        }

        public double getMean() {
            return this.mean;
        }

        public double getStd() {
            return this.std;
        }

        public List<Integer> getMajorClasses() {
            return this.majorClasses;
        }

        public List<Integer> getMinorClasses() {
            return this.minorClasses;
        }

        public MLSBag(MultiLabelInstances mlDS, Map<String, Object> parameter) {
            this.mlDS = mlDS;
            this.parameter = parameter;
            this.getLabelFrequencies();
        }

        public final void getLabelFrequencies() {
            int nLabels = this.mlDS.getNumLabels();
            int[] counters = new int[nLabels];
            int numInstances = this.mlDS.getNumInstances();
            this.labelFrequencies = new double[nLabels];
            counters = this.getCounters(this.mlDS.getDataSet());
            double sum = 0.0D;
            double squaredSum = 0.0D;
            this.freqIndex = new HashMap();

            for(int index = 0; index < nLabels; ++index) {
                this.labelFrequencies[index] = (double)((float)counters[index] * 100.0F / (float)numInstances);
                this.freqIndex.put(this.labelFrequencies[index], index);
                sum += this.labelFrequencies[index];
                squaredSum += this.labelFrequencies[index] * this.labelFrequencies[index];
            }

            this.mean = sum / (double)nLabels;
            this.std = Math.sqrt(squaredSum / (double)nLabels - this.mean * this.mean);
            Mlsmote.MLSUtils.DEBUG("mean=" + this.mean + ", std=" + this.std);
            this.getMayLabels(Mlsmote.MLSUtils.uniformIRSet());
            this.getMinLabels(Mlsmote.MLSUtils.uniformIRSet());
        }

        public List<Integer> getMayLabels() {
            List<Integer> indexes = new ArrayList();
            if ((Double)this.parameter.get("delta") < 0.0D) {
                this.parameter.put("delta", 0.5D);
            }

            Mlsmote.MLSUtils.DEBUG("delta=" + (Double)this.parameter.get("delta"));
            double limit = this.mean + (Double)this.parameter.get("delta") * this.std;

            for(int index = 0; index < this.labelFrequencies.length; ++index) {
                if (this.labelFrequencies[index] >= limit) {
                    indexes.add(index);
                    Mlsmote.MLSUtils.DEBUG("May " + indexes.size() + " -> " + index);
                }
            }

            this.majorClasses = indexes;
            return indexes;
        }

        public List<Integer> getMayLabels(boolean IR) {
            if (!IR) {
                return this.getMayLabels();
            } else {
                List<Integer> indexes = new ArrayList();

                for(int index = 0; index < this.labelFrequencies.length; ++index) {
                    if (this.labelIR[index] < this.meanIR) {
                        indexes.add(index);
                        Mlsmote.MLSUtils.DEBUG("Max " + indexes.size() + " -> " + index + " (IR: " + this.labelIR[index] + ")");
                    }
                }

                this.majorClasses = indexes;
                return indexes;
            }
        }

        public List<Integer> getMinLabels() {
            List<Integer> indexes = new ArrayList();
            if ((Double)this.parameter.get("delta") < 0.0D) {
                this.parameter.put("delta", 0.5D);
            }

            double limit = this.mean - (Double)this.parameter.get("delta") * this.std;

            for(int index = 0; index < this.labelFrequencies.length; ++index) {
                if (this.labelFrequencies[index] <= limit) {
                    indexes.add(index);
                    Mlsmote.MLSUtils.DEBUG("Min " + indexes.size() + " -> " + index);
                }
            }

            this.minorClasses = indexes;
            return indexes;
        }

        public List<Integer> getMinLabels(boolean IR) {
            if (!IR) {
                return this.getMinLabels();
            } else {
                List<Integer> indexes = new ArrayList();

                for(int index = 0; index < this.labelFrequencies.length; ++index) {
                    if (this.labelIR[index] > this.meanIR) {
                        indexes.add(index);
                        Mlsmote.MLSUtils.DEBUG("Min " + indexes.size() + " -> " + index + " (IR: " + this.labelIR[index] + ")");
                    }
                }

                this.minorClasses = indexes;
                return indexes;
            }
        }

        public boolean isMinority(Instance aInstance) {
            return this.isInGroup(aInstance, this.minorClasses);
        }

        public boolean isMajority(Instance aInstance) {
            return this.isInGroup(aInstance, this.majorClasses);
        }

        public boolean isInGroup(Instance aInstance, List<Integer> aGroup) {
            Iterator i$ = aGroup.iterator();

            Integer index;
            do {
                if (!i$.hasNext()) {
                    return false;
                }

                index = (Integer)i$.next();
            } while(aInstance.value(this.mlDS.getLabelIndices()[index]) != 1.0D);

            return true;
        }

        public Integer[] getLabelsUnderMean(double[] frequencies) {
            double sum = 0.0D;
            List<Integer> listIndexes = new ArrayList();
            double[] arr$ = frequencies;
            int len$ = frequencies.length;

            for(int i$ = 0; i$ < len$; ++i$) {
                double f = arr$[i$];
                sum += f;
            }

            long mean = Math.round(sum / (double)frequencies.length);

            for(int index = 0; index < frequencies.length; ++index) {
                if (Math.round(frequencies[index]) < mean) {
                    listIndexes.add(index);
                }
            }

            return (Integer[])listIndexes.toArray(new Integer[listIndexes.size()]);
        }

        public int[] getCounters(Instances ds) {
            int[] counters = new int[this.mlDS.getNumLabels()];
            Iterator i$ = ds.iterator();

            while(i$.hasNext()) {
                Instance aInstance = (Instance)i$.next();
                this.addCounters(aInstance, counters);
            }

            this.maxCounter = this.minCounter = counters[0];
            int[] arr$ = counters;
            int len$ = counters.length;

            for(int i$x = 0; i$x < len$; ++i$x) {
                int c = arr$[i$x];
                this.maxCounter = c > this.maxCounter ? c : this.maxCounter;
                this.minCounter = c < this.minCounter ? c : this.minCounter;
            }

            double sumIRs = 0.0D;
            this.labelIR = new double[this.mlDS.getNumLabels()];
            double minIR;
            double maxIR = minIR = counters[0] > 0 ? (double)(this.maxCounter / counters[0]) : 0.0D;

            int ix;
            for(ix = 0; ix < this.mlDS.getNumLabels(); ++ix) {
                if (counters[ix] > 0) {
                    this.labelIR[ix] = (double)this.maxCounter / (double)counters[ix];
                    sumIRs += this.labelIR[ix];
                    maxIR = this.labelIR[ix] > maxIR ? this.labelIR[ix] : maxIR;
                    minIR = this.labelIR[ix] < minIR ? this.labelIR[ix] : minIR;
                } else {
                    this.labelIR[ix] = 1.0D;
                }
            }

            this.meanIR = sumIRs / (double)this.mlDS.getNumLabels();
            Mlsmote.MLSUtils.DEBUG("meanIR=" + this.meanIR + ", maxIR=" + maxIR + ", minIR=" + minIR);
            sumIRs = 0.0D;

            for(ix = 0; ix < this.mlDS.getNumLabels(); ++ix) {
                sumIRs += (this.labelIR[ix] - this.meanIR) * (this.labelIR[ix] - this.meanIR);
            }

            sumIRs /= (double)(this.mlDS.getNumLabels() - 1);
            Mlsmote.MLSUtils.DEBUG("CVIR=" + sumIRs / this.meanIR);
            return counters;
        }

        public void addCounters(Instance aInstance, int[] counters) {
            int[] labelIndices = this.mlDS.getLabelIndices();

            for(int index = 0; index < labelIndices.length; ++index) {
                counters[index] = (int)((double)counters[index] + aInstance.value(labelIndices[index]));
            }

        }

        public double[] getLabelFrequencies(int[] counters, int numInstances) {
            int nLabels = this.mlDS.getNumLabels();
            double[] frequencies = new double[nLabels];

            for(int index = 0; index < nLabels; ++index) {
                frequencies[index] = (double)((float)counters[index] * 100.0F / (float)numInstances);
            }

            return frequencies;
        }

        public Instances getMinBag() {
            Instances imlDS = this.mlDS.getDataSet();
            Instances bag = new Instances(this.mlDS.clone().getDataSet());
            bag.delete();

            for(int index = 0; index < imlDS.numInstances(); ++index) {
                Instance aInstance = imlDS.get(index);
                if (this.isMinority(aInstance)) {
                    bag.add(aInstance);
                    imlDS.delete(index);
                    --index;
                }
            }

            return bag;
        }

        public Instances getBagOfLabel(int labelIndex) {
            Instances imlDS = this.mlDS.getDataSet();
            Instances bag = new Instances(this.mlDS.clone().getDataSet());
            bag.delete();

            for(int index = 0; index < imlDS.numInstances(); ++index) {
                Instance aInstance = imlDS.get(index);
                if (aInstance.value(this.mlDS.getLabelIndices()[labelIndex]) == 1.0D) {
                    bag.add(aInstance);
                    imlDS.delete(index);
                    --index;
                }
            }

            return bag;
        }

        public int getNumInstancesOfLabel(int labelIndex) {
            int[] counters = this.getCounters(this.mlDS.getDataSet());
            return counters[labelIndex];
        }

        public int getMeanInstancesPerLabel() {
            int[] counters = this.getCounters(this.mlDS.getDataSet());
            double sum = 0.0D;
            int[] arr$ = counters;
            int len$ = counters.length;

            for(int i$ = 0; i$ < len$; ++i$) {
                int count = arr$[i$];
                sum += (double)count;
            }

            return (int)(sum / (double)(counters.length - this.majorClasses.size()));
        }

        public int[] getMayLabels(double[] labelFrequencies) {
            return this.getLabels(labelFrequencies, true);
        }

        public int[] getMinLabels(double[] labelFrequencies) {
            return this.getLabels(labelFrequencies, false);
        }

        private int[] getLabels(double[] labelFrequencies, boolean reverse) {
            int numLabels = (double)labelFrequencies.length * 5.0D / 100.0D < 1.0D ? 1 : (int)((double)labelFrequencies.length * 5.0D / 100.0D);
            int[] labelIndices = new int[numLabels];
            ArrayList<Double> freqClone = new ArrayList();

            for(int index = 0; index < labelFrequencies.length; ++index) {
                freqClone.add(labelFrequencies[index]);
            }

            Collections.sort(freqClone, Collections.reverseOrder());
            if (!reverse) {
                Collections.reverse(freqClone);
            }

            Double[] temp = (Double[])freqClone.toArray(new Double[freqClone.size()]);

            for(int indexx = 0; indexx < numLabels; ++indexx) {
                double element = temp[indexx];

                for(int index2 = 0; index2 < freqClone.size(); ++index2) {
                    if (labelFrequencies[index2] == element) {
                        labelIndices[indexx] = index2;
                    }
                }

                Mlsmote.MLSUtils.DEBUG((reverse ? "May " : "Min ") + indexx + " -> " + labelIndices[indexx]);
            }

            return labelIndices;
        }

        public int[] getOrderedMajorityIndexes() {
            Map<Double, Integer> freqIndex = this.getFreqIndex();
            TreeSet<Double> keys = new TreeSet(freqIndex.keySet());
            int[] indexes = new int[this.getMajorClasses().size()];
            int index = 0;
            Iterator i$ = keys.iterator();

            while(i$.hasNext()) {
                Double key = (Double)i$.next();
                if (this.getMajorClasses().contains(freqIndex.get(key))) {
                    indexes[index++] = (Integer)freqIndex.get(key);
                }
            }

            return indexes;
        }
    }

    static class MLSUtils {
        public static final String FILEEXT = "fileext";
        public static final String INPATH = "inpath";
        public static final String OUTPATH = "outpath";
        public static final String LABEL_COMBINATION = "labelCombination";
        public static final String XML = "xml";
        private static boolean m_debug;
        private static boolean m_uniformIR;
        private static Map<String, Object> parameter = new HashMap();

        MLSUtils() {
        }

        public static Map<String, Object> readParameters(String folderWithARRF,String outputFolder,String extensions,String xmlLabelsFile,int labelSets) {
            try {
                parameter.put("inpath", folderWithARRF);
                parameter.put("outpath", outputFolder);
                if (!(new File((String)parameter.get("inpath"))).isDirectory() || !(new File((String)parameter.get("outpath"))).isDirectory()) {
                    throw new IOException();
                }

                parameter.put("fileext", extensions);

                parameter.put("labelCombination", 1);
                if (labelSets>0) {
                    parameter.put("labelCombination", labelSets);
                }

                parameter.put("xml", xmlLabelsFile);
                if (!(new File((String)parameter.get("xml"))).canRead()) {
                    throw new IOException();
                }

                setDebug(false);
                m_uniformIR = true;
            } catch (Exception var2) {
                System.err.println(var2.getMessage());
                System.err.println(var2.getStackTrace());

            }

            return parameter;
        }

        public static File[] getFilenames(String inpath, final String fileext) throws IOException {
            File sourceDirectory = new File(inpath);
            return sourceDirectory.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(fileext);
                }
            });
        }

        public static String xmlOf(File dataset) {
            String filename = dataset.getAbsolutePath();
            return filename.substring(0, filename.indexOf("-")) + ".xml";
        }

        public static void saveResult(File dataset, MultiLabelInstances mlDS) throws Exception {
            String filename = dataset.getName();
            ArffSaver saver = new ArffSaver();
            saver.setInstances(mlDS.getDataSet());
            saver.setFile(new File(parameter.get("outpath") + File.separator + filename));
            saver.writeBatch();
            saver = null;
        }

        public static boolean uniformIRSet() {
            return m_uniformIR;
        }

        public static void DEBUG(String message) {
            if (m_debug) {
                System.out.println(message);
            }

        }

        public static void setDebug(boolean debug) {
            m_debug = debug;
        }
    }
}

