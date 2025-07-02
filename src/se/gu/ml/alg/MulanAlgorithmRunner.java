package se.gu.ml.alg;

import me.tongfei.progressbar.ProgressBar;
import mulan.classifier.MultiLabelLearnerBase;
import mulan.classifier.MultiLabelOutput;
import mulan.classifier.lazy.IBLR_ML;
import mulan.classifier.lazy.MLkNN;
import mulan.classifier.meta.RAkEL;
import mulan.classifier.meta.RAkELd;
import mulan.classifier.neural.BPMLL;
import mulan.classifier.transformation.*;
import mulan.data.MultiLabelInstances;
import mulan.evaluation.Evaluation;
import mulan.evaluation.Evaluator;
import mulan.evaluation.MultipleEvaluation;
import mulan.evaluation.measure.*;
import mulan.evaluation.measures.regression.example.ExampleBasedRMaxSE;
import mulan.evaluation.measures.regression.macro.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import se.gu.main.Configuration;
import se.gu.ml.experiment.*;
import se.gu.utils.Utilities;
import weka.classifiers.trees.J48;
import weka.core.Instance;
import weka.core.Instances;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class MulanAlgorithmRunner  implements Serializable {

    private static final long serialVersionUID = 787358126538341060L;
    private PrintWriter pw = null;
    private PrintWriter predictionsPrintWriter = null;
    private PrintWriter predictionResultsPrintWriter = null;
    private MultiLabelInstances trainingSet = null;
    public MultiLabelInstances testSet = null;
    public Evaluator eval = new Evaluator();
    public Evaluation results;
    public MultipleEvaluation mResults;
    public List<Measure> measures = new ArrayList<Measure>();
    public long time_in, time_fin, total_time;
    public int nFolds;
    public boolean printHeader = true;
    private ArrayList<String> algorithmsRequiringFlatXMl = new ArrayList<>(Arrays.asList(new String[]{"RAkEL", "RAkELd"}));
    private Configuration configuration;
    private Instances unlabledData;
    private String predictionsOutputFile;
    private File instanceNameFile;
    private File assetMappingsFile;
    private Map<String,List<ResultMeasure>> resultMeasureMap;
    private List<PredictionResultSummary> predictionResultSummaries;
    private  File trainingFile;
    public   boolean printPredictionSummaryHeader;

    public String getPredictionResultSummaryOutputFile() {
        return predictionResultSummaryOutputFile;
    }

    public void setPredictionResultSummaryOutputFile(String predictionResultSummaryOutputFile) {
        this.predictionResultSummaryOutputFile = predictionResultSummaryOutputFile;
    }

    private String predictionResultSummaryOutputFile;


    public MulanAlgorithmRunner(MultiLabelInstances trainingSet, String trainingDataSetFile, String testDataSetFile, String xmlLabelsFile, String resultsOutputFile, String predictionsOutputFile, File instanceNameFile, File assetMappingsFile, int kFolds, int chainedIterations, String xmlFlatFile, MultiLabelInstances unlabledData, Configuration configuration) throws IOException {
        this(trainingDataSetFile, testDataSetFile, xmlLabelsFile, resultsOutputFile, kFolds, chainedIterations);
        this.trainingSet = trainingSet;
        this.xmlFlatFile = xmlFlatFile;
        this.configuration = configuration;
        this.unlabledData = unlabledData==null?null: unlabledData.getDataSet();
        this.predictionsOutputFile = predictionsOutputFile;
        this.instanceNameFile = instanceNameFile;
        this.assetMappingsFile = assetMappingsFile;
        resultMeasureMap = new LinkedHashMap<>();
        predictionResultSummaries = new ArrayList<>();

        //predictionsPrintWriter = new PrintWriter(new FileWriter(predictionsOutputFolder,true));
    }

    private String xmlFlatFile;


    private Map<String, ClassifierRecord> classifiers;


    private String trainingDataSetFile, testDataSetFile, xmlLabelsFile, resultsOutputFile;
    private boolean includeMacroAverageLabelValues = false;
    private int kFolds = 10, chainedIterations = 10;

    public MulanAlgorithmRunner(String trainingDataSetFile, String testDataSetFile, String xmlLabelsFile, String resultsOutputFile, int kFolds, int chainedIterations) throws IOException {
        this();
        this.trainingDataSetFile = trainingDataSetFile;
        this.trainingFile = new File(trainingDataSetFile);
        this.testDataSetFile = testDataSetFile;
        this.xmlLabelsFile = xmlLabelsFile;
        this.resultsOutputFile = resultsOutputFile;
        this.kFolds = kFolds;
        this.chainedIterations = chainedIterations;
        pw = new PrintWriter(new FileWriter(resultsOutputFile, true));

    }

    public MulanAlgorithmRunner() throws IOException {


    }

    private void addClassifierToList(String classifierName, List<MultiLabelLearnerBase> leanersList) {
        ClassifierRecord classifierRecord = prepareExecution(classifierName);
        classifierRecord.setClassifierList(classifierName.equalsIgnoreCase("CC") ? getClassifierChains(classifierRecord) : leanersList);
        classifiers.put(classifierName, classifierRecord);
    }

    private boolean isModelIncluded(ModelGroup modelGroup, String modelName) {
        return (modelGroup == ModelGroup.EVALUATION && configuration.getEvaluationModels().contains(modelName)) ||
                (modelGroup == ModelGroup.PREDICTION && configuration.getPredictionModels().contains(modelName)) ||
                (modelGroup == ModelGroup.FINAL_PREDICTION && configuration.getFinalPredictionModels().contains(modelName));
    }

    private void createClassifiersList(ModelGroup modelGroup) {
        classifiers = new LinkedHashMap<>();


        if (isModelIncluded(modelGroup, "BR")) {
            //Binary relevance
            List<MultiLabelLearnerBase> binaryRelevanceList = new ArrayList<>();
            binaryRelevanceList.add(new BinaryRelevance(new J48()));
            addClassifierToList("BR", binaryRelevanceList);
        }

        if (isModelIncluded(modelGroup, "CC")) {
            //classificer chains
            addClassifierToList("CC", null);//for classifier chains we use a different method to get list of learners so we don;t need t precreate the list here
        }
        //AdaBoost.MH
        if (isModelIncluded(modelGroup, "AdaBoostMH")) {
            List<MultiLabelLearnerBase> adaBoostList = new ArrayList<>();
            adaBoostList.add(new AdaBoostMH());
            addClassifierToList("AdaBoostMH", adaBoostList);
        }
        //Calibrated Label Ranking
        if (isModelIncluded(modelGroup, "CLR")) {
            List<MultiLabelLearnerBase> clrList = new ArrayList<>();
            clrList.add(new CalibratedLabelRanking(new J48()));
            addClassifierToList("CLR", clrList);
        }
        //EBR--enseble of BR
        if (isModelIncluded(modelGroup, "EBR")) {
            addClassifierToList("EBR", getEnsembleChains(EnsembleChain.EBR));
        }
        //ECC--enseble of CC
        if (isModelIncluded(modelGroup, "ECC")) {
            addClassifierToList("ECC", getEnsembleChains(EnsembleChain.ECC));
        }
        //EPS--enseble of Pruned sets
        if (isModelIncluded(modelGroup, "EPS")) {
            addClassifierToList("EPS", getEnsembleChains(EnsembleChain.EPS));
        }
        //IBLR-Instanc based logistic regression
        if (isModelIncluded(modelGroup, "IBLR")) {
            List<MultiLabelLearnerBase> iblrList = new ArrayList<>();
            iblrList.add(new IBLR_ML());
            addClassifierToList("IBLR", iblrList);
        }
        //LP-Label Poweset
        if (isModelIncluded(modelGroup, "LP")) {
            List<MultiLabelLearnerBase> lpList = new ArrayList<>();
            lpList.add(new LabelPowerset(new J48()));
            addClassifierToList("LP", lpList);
        }
        //MLkNN-MultiLabel kNearest Neighbors
        if (isModelIncluded(modelGroup, "MLkNN")) {
            List<MultiLabelLearnerBase> mlkNNList = new ArrayList<>();
            mlkNNList.add(new MLkNN());
            addClassifierToList("MLkNN", mlkNNList);
        }
        //MLS-MultiLabel Stacking
        if (isModelIncluded(modelGroup, "MLS")) {
            List<MultiLabelLearnerBase> mlsList = new ArrayList<>();
            mlsList.add(new MultiLabelStacking());
            addClassifierToList("MLS", mlsList);
        }

        //PS-PrunedSets
        if (isModelIncluded(modelGroup, "PS")) {
            List<MultiLabelLearnerBase> psList = new ArrayList<>();
            psList.add(new PrunedSets());
            addClassifierToList("PS", psList);
        }

        //RAkEL--Random k-label sets
        if (isModelIncluded(modelGroup, "RAkEL")) {
            addClassifierToList("RAkEL", getEnsembleChains(EnsembleChain.RAkEL));
        }
        //RAkELd--Random k-label sets
        if (isModelIncluded(modelGroup, "RAkELd")) {
            addClassifierToList("RAkELd", getEnsembleChains(EnsembleChain.RAkELd));
        }

    }

    private List<MultiLabelLearnerBase> getClassifierChains(ClassifierRecord classifierRecord) {

        List<MultiLabelLearnerBase> classifierChainsList = new ArrayList<>();

        /* The seeds are 10, 20, 30, ... */
        for (int i = 1; i <= chainedIterations; i++) {
            time_in = System.currentTimeMillis();
            Random rand = new Random(i * 10);

            //Get random chain
            int[] chain = new int[classifierRecord.getTrainingSet().getNumLabels()];
            for (int c = 0; c < classifierRecord.getTrainingSet().getNumLabels(); c++) {
                chain[c] = c;
            }

            for (int c = 0; c < classifierRecord.getTrainingSet().getNumLabels(); c++) {
                int r = rand.nextInt(classifierRecord.getTrainingSet().getNumLabels());
                int swap = chain[c];
                chain[c] = chain[r];
                chain[r] = swap;
            }

            ClassifierChain learner = new ClassifierChain(new J48(), chain);
            classifierChainsList.add(learner);
        }
        return classifierChainsList;
    }

    private List<MultiLabelLearnerBase> getEnsembleChains(EnsembleChain ensembleChain) {

        List<MultiLabelLearnerBase> classifierChainsList = new ArrayList<>();
        /* The seeds are 10, 20, 30, ... */
        for (int i = 1; i <= chainedIterations; i++) {

            MultiLabelLearnerBase learner = null;
            if (ensembleChain == EnsembleChain.BPMLL) {
                learner = new BPMLL(i * 10);
            } else if (ensembleChain == EnsembleChain.EBR) {
                learner = new EBR(i * 10);
            } else if (ensembleChain == EnsembleChain.ECC) {
                learner = new ECC(i * 10);
            } else if (ensembleChain == EnsembleChain.EPS) {
                learner = new EPS(i * 10);
            } else if (ensembleChain == EnsembleChain.RAkEL) {
                RAkEL rAkEL = new RAkEL(new LabelPowerset(new J48()));
                rAkEL.setSeed(i * 10);
                learner = rAkEL;

            } else if (ensembleChain == EnsembleChain.RAkELd) {
                RAkELd rAkEL = new RAkELd(new LabelPowerset(new J48()));
                rAkEL.setSeed(i * 10);
                learner = rAkEL;

            }
            classifierChainsList.add(learner);
        }
        return classifierChainsList;
    }


    public ClassifierRecord prepareExecution(String classifierName) {
        ClassifierRecord classifierRecord = new ClassifierRecord();
        try {
            classifierRecord.setClassifierName(classifierName);

            String xmlFile = algorithmsRequiringFlatXMl.contains(classifierName) ? xmlFlatFile : xmlLabelsFile;
            classifierRecord.setTrainingSet(trainingSet);

            if (kFolds <= 0) {
                classifierRecord.setTestSet(new MultiLabelInstances(testDataSetFile, xmlFile));
            } else {
                classifierRecord.setTestSet(null);
            }
            classifierRecord.setnFolds(kFolds);

            classifierRecord.setMeasures(prepareMeasuresClassification(classifierRecord.getTrainingSet()));


        } catch (Exception e) {
            e.printStackTrace();
        }
        return classifierRecord;
    }

    public void printHeader(ClassifierRecord classifierRecord, boolean lvalue) throws Exception {
        //Print header
        pw.print("commit" + ";");//commit number
        pw.print("Classifier" + ";");
        pw.print("Dataset" + ";");
        for (Measure m : classifierRecord.getMeasures()) {
            pw.print(m.getName().replaceAll("\\s*|\\W","").replaceAll("-","") + ";");//remove spaces and non word characters from column names
            if ((lvalue) && (m.getClass().getName().contains("Macro"))) {
                for (int l = 0; l < classifierRecord.getTrainingSet().getNumLabels(); l++) {
                    pw.print(m.getName() + " - " + classifierRecord.getTrainingSet().getLabelNames()[l] + ";");
                }
            }
        }
        pw.print("ExecutionTimeInMS");
        pw.println();
    }


    public void printResults(String classifierName) throws Exception {
        List<ResultMeasure> resultMeasures = resultMeasureMap.get(classifierName);
        String[] p = trainingDataSetFile.split("\\/");
        String datasetName = p[p.length - 1].split("\\.")[0];
        //pw.print(algorithm + "_" + datasetName + ";");
        String dsName[] = datasetName.split("\\\\|/");
        pw.print( dsName[dsName.length-1].split("_")[0]+ ";");//commit number
        pw.print(classifierName + ";");
        pw.print(datasetName + ";");

        //print metrics
        pw.print(resultMeasures.stream().map(ResultMeasure::getHammingLoss).collect(Collectors.averagingDouble(Double::doubleValue)) + ";");
        pw.print(resultMeasures.stream().map(ResultMeasure::getSubsetAccuracy).collect(Collectors.averagingDouble(Double::doubleValue)) + ";");
        pw.print(resultMeasures.stream().map(ResultMeasure::getExampleBasedPrecision).collect(Collectors.averagingDouble(Double::doubleValue)) + ";");
        pw.print(resultMeasures.stream().map(ResultMeasure::getExampleBasedRecall).collect(Collectors.averagingDouble(Double::doubleValue)) + ";");
        pw.print(resultMeasures.stream().map(ResultMeasure::getExampleBasedFMeasure).collect(Collectors.averagingDouble(Double::doubleValue)) + ";");
        pw.print(resultMeasures.stream().map(ResultMeasure::getExampleBasedAccuracy).collect(Collectors.averagingDouble(Double::doubleValue)) + ";");
        pw.print(resultMeasures.stream().map(ResultMeasure::getExampleBasedSpecificity).collect(Collectors.averagingDouble(Double::doubleValue)) + ";");

        // add label-based measures
        pw.print(resultMeasures.stream().map(ResultMeasure::getMicroPrecision).collect(Collectors.averagingDouble(Double::doubleValue)) + ";");
        pw.print(resultMeasures.stream().map(ResultMeasure::getMicroRecall).collect(Collectors.averagingDouble(Double::doubleValue)) + ";");
        pw.print(resultMeasures.stream().map(ResultMeasure::getMicroFMeasure).collect(Collectors.averagingDouble(Double::doubleValue)) + ";");
        pw.print(resultMeasures.stream().map(ResultMeasure::getMicroSpecificity).collect(Collectors.averagingDouble(Double::doubleValue)) + ";");
        //measures.add(new MicroAccuracy(numOfLabels));
        pw.print(resultMeasures.stream().map(ResultMeasure::getMacroPrecision).collect(Collectors.averagingDouble(Double::doubleValue)) + ";");
        pw.print(resultMeasures.stream().map(ResultMeasure::getMacroRecall).collect(Collectors.averagingDouble(Double::doubleValue)) + ";");
        pw.print(resultMeasures.stream().map(ResultMeasure::getMacroFMeasure).collect(Collectors.averagingDouble(Double::doubleValue)) + ";");
        pw.print(resultMeasures.stream().map(ResultMeasure::getMacroSpecificity).collect(Collectors.averagingDouble(Double::doubleValue)) + ";");
        //measures.add(new MacroAccuracy(numOfLabels));

        // add ranking based measures
        pw.print(resultMeasures.stream().map(ResultMeasure::getAveragePrecision).collect(Collectors.averagingDouble(Double::doubleValue)) + ";");
        pw.print(resultMeasures.stream().map(ResultMeasure::getCoverage).collect(Collectors.averagingDouble(Double::doubleValue)) + ";");
        pw.print(resultMeasures.stream().map(ResultMeasure::getOneError).collect(Collectors.averagingDouble(Double::doubleValue)) + ";");
        pw.print(resultMeasures.stream().map(ResultMeasure::getIsError).collect(Collectors.averagingDouble(Double::doubleValue)) + ";");
        pw.print(resultMeasures.stream().map(ResultMeasure::getErrorSetSize).collect(Collectors.averagingDouble(Double::doubleValue)) + ";");
        pw.print(resultMeasures.stream().map(ResultMeasure::getRankingLoss).collect(Collectors.averagingDouble(Double::doubleValue)) + ";");

        // add confidence measures if applicable
        pw.print(resultMeasures.stream().map(ResultMeasure::getMeanAveragePrecision).collect(Collectors.averagingDouble(Double::doubleValue)) + ";");
        pw.print(resultMeasures.stream().map(ResultMeasure::getGeometricMeanAveragePrecision).collect(Collectors.averagingDouble(Double::doubleValue)) + ";");

//        for (Measure m : measures) {
//            pw.print(mResults.getMean(m.getName()) + ";");
//
//            if ((includeMacroAverageLabelValues) && (m.getClass().getName().contains("Macro"))) {
//                for (int l = 0; l < trainingSet.getNumLabels(); l++) {
//                    pw.print(mResults.getMean(m.getName(), l) + ";");
//                }
//            }
//        }

        pw.print(total_time + ";");
        pw.println();
    }


    public void execute() {
        createClassifiersList(ModelGroup.EVALUATION);
        for (String classifierName : classifiers.keySet()) {
            System.out.println(classifierName);

            ClassifierRecord classifierRecord = classifiers.get(classifierName);
            List<MultiLabelLearnerBase> leaners = classifierRecord.getClassifierList();
            if(resultMeasureMap.get(classifierName)==null){
                resultMeasureMap.put(classifierName,new ArrayList<>());
            }


            //start timer
            time_in = System.currentTimeMillis();
            for (MultiLabelLearnerBase learner : leaners) {
                try {

                    if (classifierRecord.getnFolds() > 0) {
                        mResults = eval.crossValidate(learner, classifierRecord.getTrainingSet(), classifierRecord.getMeasures(), classifierRecord.getnFolds());
                    } else {
                        learner.build(classifierRecord.getTrainingSet());
                        results = eval.evaluate(learner, classifierRecord.getTestSet(), classifierRecord.getMeasures());
                    }

                    //set measures
                    ResultMeasure resultMeasure = new ResultMeasure();
                    for (Measure measure : kFolds > 0 ? classifierRecord.getMeasures() : results.getMeasures()) {
                        setMeasureValue(measure, resultMeasure);
                    }
                    resultMeasureMap.get(classifierName).add(resultMeasure);

                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
            //ed timer
            time_fin = System.currentTimeMillis();
            total_time = time_fin - time_in;
            System.out.println("Execution time (ms): " + total_time);
            //now print results for all classifiers in the list
            try {
                if (printHeader) {

                    printHeader(classifierRecord, includeMacroAverageLabelValues);

                    printHeader = false;
                }
                printResults(classifierRecord.getClassifierName());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //cloe the printer

        if (pw != null) {
            pw.close();
        }

    }

    public void executePredictions() throws IOException {
        createClassifiersList(ModelGroup.PREDICTION);
        int numInstances = unlabledData.numInstances();
        if(!assetMappingsFile.exists()){
            return;
        }
        List<String> assetMappings = FileUtils.readLines(assetMappingsFile, configuration.getTextEncoding());
        List<String> assetMappingsWithLabels =  assetMappings.stream().filter(s->s.split("->").length>=2).collect(Collectors.toList());
        predictionResultsPrintWriter = new PrintWriter(new FileWriter(predictionResultSummaryOutputFile,true));
        try (ProgressBar pb = new ProgressBar("Running PREDICTIONS:", classifiers.size())) {
            try {
                if(configuration.isPrintDetailedPredictionResults()) {
                    predictionsPrintWriter = new PrintWriter(new FileWriter(predictionsOutputFile, true));
                }
                boolean isPrintHeader = true;
                List<String> instanceNames = FileUtils.readLines(instanceNameFile, configuration.getTextEncoding());

                for (String classifierName : classifiers.keySet()) {
                    pb.step();
                    pb.setExtraMessage(classifierName);
                    //System.out.println(classifierName);
                    List<ResultMeasure> resultMeasures = new ArrayList<>();
                    ClassifierRecord classifierRecord = classifiers.get(classifierName);
                    List<MultiLabelLearnerBase> leaners = classifierRecord.getClassifierList();
                    if(configuration.isPrintDetailedPredictionResults()) {
                        if (isPrintHeader) {
                            printMultiLabelOutputHeader(classifierRecord);
                            isPrintHeader = false;
                        }
                    }
                    int learnerIndex = 0;
                    int lastLearnerIndex = leaners.size() - 1;
                    for (MultiLabelLearnerBase learner : leaners) {
                        try {

                            learner.build(classifierRecord.getTrainingSet());
                            //now predict
                            for (int i = 0; i < numInstances; i++) {
                                Instance instance = unlabledData.instance(i);
                                MultiLabelOutput output = learner.makePrediction(instance);

                                if (learnerIndex == lastLearnerIndex) {//only print for the last learner

                                  if(assetMappingsWithLabels.size()>0) {
                                      printMultiLabelOutput(classifierRecord, output, instanceNames.get(i), assetMappingsWithLabels);
                                  }
                                }
                            }
                            learnerIndex++;
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }


                }
                //at the end of all classifiers, print results summary
                printPredictionResultSummary();


            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (predictionsPrintWriter != null) {
                    predictionsPrintWriter.close();
                }
                if (predictionResultsPrintWriter != null) {
                    predictionResultsPrintWriter.close();
                }
            }
        }
    }

    private void printMultiLabelOutputHeader(ClassifierRecord classifierRecord) {

        //Print header
        predictionsPrintWriter.print("InstanceName" + ";");
        predictionsPrintWriter.print("Classifier" + ";");
        predictionsPrintWriter.print("OutputName" + ";");
        //labels
        String[] labelNames = classifierRecord.getTrainingSet().getLabelNames();
        for (String labelName : labelNames) {
            predictionsPrintWriter.print(labelName + ";");

        }
        predictionsPrintWriter.print("Ranking" + ";");
        predictionsPrintWriter.print("ActualLabels" + ";");
        predictionsPrintWriter.print("Precision" + ";");
        predictionsPrintWriter.print("Recall" + ";");
        predictionsPrintWriter.print("F-Measure" + ";");
        predictionsPrintWriter.println();

    }

    private List<String> getLinesFromCluster(String instanceName) {
        //get lines of cluster
        List<String> lines = new ArrayList<>();
        String[] instanceArray = instanceName.split(String.format("%s", configuration.getFeatureQualifiedNameSeparator()));
        String linesRange = instanceArray[1];
        String fileName = instanceArray[0];
        String start = linesRange.split(Pattern.quote("-"))[0].trim();
        int startLine = Integer.parseInt(start);
        int endLine = Integer.parseInt(linesRange.split(Pattern.quote("-"))[1].trim());
        for (int i = startLine; i <= endLine; i++) {
            int lineNumber = i;
            lines.add(String.format("%s%s%d", fileName, configuration.getFeatureQualifiedNameSeparator(), lineNumber));
        }

        return lines;
    }

    private String getMappedLabels(String instanceName, List<String> assetMappings, boolean isFragment) {
        List<String> mappings = new ArrayList<>();
        List<String> commaMappings = new ArrayList<>();
        if (isFragment) {

            List<String> lines = getLinesFromCluster(instanceName);
            for (String line : lines) {
                mappings.addAll(assetMappings.stream().filter(a -> a.split("->")[0].equalsIgnoreCase(line)).collect(Collectors.toList()));
            }
            mappings.addAll(assetMappings.stream().filter(a -> a.split("->")[0].equalsIgnoreCase(instanceName)).collect(Collectors.toList()));

        } else {
            mappings.addAll(assetMappings.stream().filter(a -> a.split("->")[0].equalsIgnoreCase(instanceName)).collect(Collectors.toList()));
        }
        //now process the mappings
        mappings.stream().forEach(m -> {
            String[] labels = m.split("->");
            if (labels.length == 2) {
                commaMappings.add(labels[1]);
            }
        });
        //now join the strings
        String allLabelsText = commaMappings.stream().distinct().collect(Collectors.joining(","));
//        String finalLabel = null;
//        //now split them and a get a distinct list
//        List<String> allLabels = Arrays.asList(allLabelsText.split(","));
//        if (allLabelsText != null) {
//            finalLabel = allLabels.stream().distinct().collect(Collectors.joining(","));
//        }
        //get unique labels
        return allLabelsText;
    }

    private void printPredictionResultSummary(){
        //print header

            if (printPredictionSummaryHeader) {
                predictionResultsPrintWriter.print("commit" + ";");
                predictionResultsPrintWriter.print("DataSet" + ";");
                predictionResultsPrintWriter.print("Classifier" + ";");
                predictionResultsPrintWriter.print("TotalInstances" + ";");
                predictionResultsPrintWriter.print("InstancesWithRankedLabels" + ";");
                predictionResultsPrintWriter.print("PercentageOfInstancesWithRankedLabels" + ";");
                predictionResultsPrintWriter.print("InstancesWithPrecisionNull" + ";");
                predictionResultsPrintWriter.print("PercentageOfInstancesWithPrecisionNull" + ";");
                predictionResultsPrintWriter.print("InstancesWithPrecisionZero" + ";");
                predictionResultsPrintWriter.print("PercentageOfInstancesWithPrecisionZero" + ";");
                predictionResultsPrintWriter.print("InstancesWithPrecisionNonZero" + ";");
                predictionResultsPrintWriter.print("PercentageOfInstancesWithPrecisionNonZero" + ";");
                predictionResultsPrintWriter.print("InstancesWithPrecisionOne" + ";");
                predictionResultsPrintWriter.print("PercentageOfInstancesWithPrecisionOne" + ";");
                predictionResultsPrintWriter.print("InstancesWithRecallZero" + ";");
                predictionResultsPrintWriter.print("PercentageOfInstancesWithRecallZero" + ";");
                predictionResultsPrintWriter.print("InstancesWithRecallOne" + ";");
                predictionResultsPrintWriter.print("PercentageOfInstancesWithRecallOne" + ";");
                predictionResultsPrintWriter.print("AveragePrecisionForAllNonNullPrecision" + ";");
                predictionResultsPrintWriter.print("AveragePrecisionForNonZeroPrecision" + ";");
                predictionResultsPrintWriter.print("AverageRecallForAllNonNullPrecision" + ";");
                predictionResultsPrintWriter.print("AverageRecallForAllNonZeroPrecision" + ";");
                predictionResultsPrintWriter.print("AverageFScoreForAllNonNullPrecision" + ";");
                predictionResultsPrintWriter.print("AverageFScoreForAllNonZeroPrecision" + ";");
                predictionResultsPrintWriter.println();

                printPredictionSummaryHeader = false;

        }
        //print rows
        //List<String> datasetNames = predictionResultSummaries.parallelStream().map(PredictionResultSummary::getCommit).distinct().collect(Collectors.toList());
        String datasetName = trainingFile.getName();
        List<String> classifiers = predictionResultSummaries.parallelStream().map(PredictionResultSummary::getClassifier).distinct().collect(Collectors.toList());


            for(String classifier:classifiers){
                ResultSummary resultSummary = getResultSummary(datasetName,classifier);
                predictionResultsPrintWriter.print(datasetName.split("_")[0]+ ";");
                predictionResultsPrintWriter.print(datasetName+ ";");
                predictionResultsPrintWriter.print(classifier+ ";");
                predictionResultsPrintWriter.print(resultSummary.getTotalInstances() + ";");
                predictionResultsPrintWriter.print(resultSummary.getInstancesWithRankedLabels() + ";");
                predictionResultsPrintWriter.print(resultSummary.getPercentageOfInstancesWithRankedLabels() + ";");
                predictionResultsPrintWriter.print(resultSummary.getInstancesWithPrecisionNull() + ";");
                predictionResultsPrintWriter.print(resultSummary.getPercentageOfInstancesWithPrecisionNull() + ";");
                predictionResultsPrintWriter.print(resultSummary.getInstancesWithPrecisionZero() + ";");
                predictionResultsPrintWriter.print(resultSummary.getPercentageOfInstancesWithPrecisionZero() + ";");
                predictionResultsPrintWriter.print(resultSummary.getInstancesWithPrecisionNonZero() + ";");
                predictionResultsPrintWriter.print(resultSummary.getPercentageOfInstancesWithPrecisionNonZero() + ";");
                predictionResultsPrintWriter.print(resultSummary.getInstancesWithPrecisionOne() + ";");
                predictionResultsPrintWriter.print(resultSummary.getPercentageOfInstancesWithPrecisionOne() + ";");
                predictionResultsPrintWriter.print(resultSummary.getInstancesWithRecallZero() + ";");
                predictionResultsPrintWriter.print(resultSummary.getPercentageOfInstancesWithRecallZero() + ";");
                predictionResultsPrintWriter.print(resultSummary.getInstancesWithRecallOne() + ";");
                predictionResultsPrintWriter.print(resultSummary.getPercentageOfInstancesWithRecallOne() + ";");
                predictionResultsPrintWriter.print(resultSummary.getAveragePrecisionForAllNonNullPrecision() + ";");
                predictionResultsPrintWriter.print(resultSummary.getAveragePrecisionForNonZeroPrecision() + ";");
                predictionResultsPrintWriter.print(resultSummary.getAverageRecallForAllNonNullPrecision() + ";");
                predictionResultsPrintWriter.print(resultSummary.getAverageRecallForAllNonZeroPrecision() + ";");
                predictionResultsPrintWriter.print(resultSummary.getAverageFScoreForAllNonNullPrecision() + ";");
                predictionResultsPrintWriter.print(resultSummary.getAverageFScoreForAllNonZeroPrecision() + ";");
                predictionResultsPrintWriter.println();

            }





    }

    private ResultSummary getResultSummary(String dataSetName,String classifierName){
        ResultSummary resultSummary = new ResultSummary();
        OptionalDouble value;
        //TotalInstances
        resultSummary.setTotalInstances(
                ((Long)predictionResultSummaries.stream()
                        .filter(p->p.getCommit().equalsIgnoreCase(dataSetName)&&p.getClassifier().equalsIgnoreCase(classifierName))
                        .count()).doubleValue()
        );
        //InstancesWithRankedLabels
        resultSummary.setInstancesWithRankedLabels(
                predictionResultSummaries.stream().filter(p->p.getCommit().equalsIgnoreCase(dataSetName)&&p.getClassifier().equalsIgnoreCase(classifierName))
                .map(PredictionResultSummary::getRankedInstance)
                .mapToDouble(Double::doubleValue)
                .sum()
        );
        //PercentageOfInstancesWithRankedLabels
        resultSummary.setPercentageOfInstancesWithRankedLabels(resultSummary.getInstancesWithRankedLabels()/resultSummary.getTotalInstances());
        //InstancesWithPrecisionNull
        resultSummary.setInstancesWithPrecisionNull(
                ((Long)predictionResultSummaries.stream()
                        .filter(p->p.getCommit().equalsIgnoreCase(dataSetName)&&p.getClassifier().equalsIgnoreCase(classifierName)&& (p.getPrecision()==null || p.getPrecision().isNaN()))
                        .count()).doubleValue()
        );
        //PercentageOfInstancesWithPrecisionNull
        resultSummary.setPercentageOfInstancesWithPrecisionNull(resultSummary.getInstancesWithPrecisionNull()/resultSummary.getTotalInstances());
        //InstancesWithPrecisionZero
        resultSummary.setInstancesWithPrecisionZero(
                ((Long)predictionResultSummaries.stream()
                        .filter(p->p.getCommit().equalsIgnoreCase(dataSetName)&&p.getClassifier().equalsIgnoreCase(classifierName)&&p.getPrecision().doubleValue()==0.0)
                        .count()).doubleValue()
        );
        //PercentageOfInstancesWithPrecisionZero
        resultSummary.setPercentageOfInstancesWithPrecisionZero(resultSummary.getInstancesWithPrecisionZero()/resultSummary.getTotalInstances());
        //InstancesWithPrecisionNonZero
        resultSummary.setInstancesWithPrecisionNonZero(
                ((Long)predictionResultSummaries.stream()
                        .filter(p->p.getCommit().equalsIgnoreCase(dataSetName)&&p.getClassifier().equalsIgnoreCase(classifierName)&&p.getPrecision()!=null &&!p.getPrecision().isNaN() && p.getPrecision().doubleValue()>0.0)
                        .count()).doubleValue()
        );
        //PercentageOfInstancesWithPrecisionNonZero
        resultSummary.setPercentageOfInstancesWithPrecisionNonZero(resultSummary.getInstancesWithPrecisionNonZero()/resultSummary.getTotalInstances());
        //InstancesWithPrecisionOne
        resultSummary.setInstancesWithPrecisionOne(
                ((Long)predictionResultSummaries.stream()
                        .filter(p->p.getCommit().equalsIgnoreCase(dataSetName)&&p.getClassifier().equalsIgnoreCase(classifierName)&&p.getPrecision().doubleValue()==1.0)
                        .count()).doubleValue()
        );
        //PercentageOfInstancesWithPrecisionOne
        resultSummary.setPercentageOfInstancesWithPrecisionOne(resultSummary.getInstancesWithPrecisionOne()/resultSummary.getTotalInstances());
        //InstancesWithRecallZero
        resultSummary.setInstancesWithRecallZero(
                ((Long)predictionResultSummaries.stream()
                        .filter(p->p.getCommit().equalsIgnoreCase(dataSetName)&&p.getClassifier().equalsIgnoreCase(classifierName)&&p.getRecall().doubleValue()==0.0)
                        .count()).doubleValue()
        );
        //PercentageOfInstancesWithRecallZero
        resultSummary.setPercentageOfInstancesWithRecallZero(resultSummary.getInstancesWithRecallZero()/resultSummary.getTotalInstances());
        //InstancesWithRecallOne
        resultSummary.setInstancesWithRecallOne(
                ((Long)predictionResultSummaries.stream()
                        .filter(p->p.getCommit().equalsIgnoreCase(dataSetName)&&p.getClassifier().equalsIgnoreCase(classifierName)&&p.getRecall().doubleValue()==1.0)
                        .count()).doubleValue()
        );
        //PercentageOfInstancesWithRecallOne
        resultSummary.setPercentageOfInstancesWithRecallOne(resultSummary.getInstancesWithRecallOne()/resultSummary.getTotalInstances());
        //AveragePrecisionForAllNonNullPrecision
        value = predictionResultSummaries.stream()
                .filter(p->p.getCommit().equalsIgnoreCase(dataSetName)&&p.getClassifier().equalsIgnoreCase(classifierName)&&!p.getPrecision().isNaN())
                .map(PredictionResultSummary::getPrecision)
                .mapToDouble(Double::doubleValue).average();
                resultSummary.setAveragePrecisionForAllNonNullPrecision(
                        value.isPresent()?value.getAsDouble():0.0
        );
        //AveragePrecisionForNonZeroPrecision
         value = predictionResultSummaries.stream()
                .filter(p->p.getCommit().equalsIgnoreCase(dataSetName)&&p.getClassifier().equalsIgnoreCase(classifierName)&&!p.getPrecision().isNaN()&&p.getPrecision().doubleValue()>0.0)
                .map(PredictionResultSummary::getPrecision)
                .mapToDouble(Double::doubleValue).average();
        resultSummary.setAveragePrecisionForNonZeroPrecision(value.isPresent()?value.getAsDouble():0.0

        );
        //AverageRecallForAllNonNullPrecision
        value =predictionResultSummaries.stream()
                .filter(p->p.getCommit().equalsIgnoreCase(dataSetName)&&p.getClassifier().equalsIgnoreCase(classifierName)&&!p.getPrecision().isNaN())
                .map(PredictionResultSummary::getRecall)
                .mapToDouble(Double::doubleValue).average();
                resultSummary.setAverageRecallForAllNonNullPrecision(
                        value.isPresent()?value.getAsDouble():0.0
        );
        //AverageRecallForAllNonZeroPrecision
        value = predictionResultSummaries.stream()
                .filter(p->p.getCommit().equalsIgnoreCase(dataSetName)&&p.getClassifier().equalsIgnoreCase(classifierName)&&!p.getPrecision().isNaN()&&p.getPrecision().doubleValue()>0.0)
                .map(PredictionResultSummary::getRecall)
                .mapToDouble(Double::doubleValue).average();
        resultSummary.setAverageRecallForAllNonZeroPrecision(
                value.isPresent()?value.getAsDouble():0.0
        );
        //AverageFScoreForAllNonNullPrecision
        value =predictionResultSummaries.stream()
                .filter(p->p.getCommit().equalsIgnoreCase(dataSetName)&&p.getClassifier().equalsIgnoreCase(classifierName)&&!p.getPrecision().isNaN())
                .map(PredictionResultSummary::getFscore)
                .mapToDouble(d-> d.isNaN()?0.0:d.doubleValue()).average();
                resultSummary.setAverageFScoreForAllNonNullPrecision(
                        value.isPresent()?value.getAsDouble():0.0
        );
        //AverageFScoreForAllNonZeroPrecision
        value =  predictionResultSummaries.stream()
                .filter(p->p.getCommit().equalsIgnoreCase(dataSetName)&&p.getClassifier().equalsIgnoreCase(classifierName)&&!p.getPrecision().isNaN()&&p.getPrecision().doubleValue()!=0.0)
                .map(PredictionResultSummary::getFscore)
                .mapToDouble(d-> d.isNaN()?0.0:d.doubleValue()).average();
                resultSummary.setAverageFScoreForAllNonZeroPrecision(
                        value.isPresent()?value.getAsDouble():0.0
        );
        return resultSummary;

    }

    private void printMultiLabelOutput(ClassifierRecord classifierRecord, MultiLabelOutput multiLabelOutput, String instanceName, List<String> assetMappings) {


        List<String> mappedLines = assetMappings.stream().map(a->a.split(";")[0]).collect(Collectors.toList());
        boolean[] bipartitions = multiLabelOutput.hasBipartition() ? multiLabelOutput.getBipartition() : null;
        double[] confidences = multiLabelOutput.hasConfidences() ? multiLabelOutput.getConfidences() : null;
        double[] pValues = multiLabelOutput.hasPvalues() ? multiLabelOutput.getPvalues() : null;
        int[] ranking = multiLabelOutput.hasRanking() ? multiLabelOutput.getRanking() : null;

        String regex = "(.*)\\d+-\\d+(.*)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(instanceName);
        boolean isFragment = matcher.find();
        String actualLabels = getMappedLabels(instanceName, assetMappings,isFragment);
        boolean intesectionExists = false;
        if(isFragment){
            List<String> lines = getLinesFromCluster(instanceName);
            intesectionExists = mappedLines.contains(instanceName)|| Utilities.intersectionExists(lines,mappedLines);
        }
        if(StringUtils.isBlank(actualLabels)&&!intesectionExists){
            return;
        }
        PredictionResultSummary predictionResultSummary = new PredictionResultSummary();
        List<String> datasetLabels = Arrays.asList(classifierRecord.getTrainingSet().getLabelNames());
        List<String> retrievedLabels = getRetrievedLabels(datasetLabels, bipartitions);

        PredictionMeasure predictionMeasure = isFragment?
                getPredictionMeasureForCluster(retrievedLabels,instanceName,assetMappings,isFragment):
                getPredictionMeasure(retrievedLabels, getMappedLabels(actualLabels));
        //bipartition
        if(configuration.isPrintDetailedPredictionResults()){


        predictionsPrintWriter.print(instanceName + ";");
        predictionsPrintWriter.print(classifierRecord.getClassifierName() + ";");
        predictionsPrintWriter.print("BIPARTITION" + ";");
        if (bipartitions != null) {
            for (boolean b : bipartitions) {
                predictionsPrintWriter.print(b + ";");
            }
        }

        predictionsPrintWriter.print(getLabelNameRanking(datasetLabels, ranking, bipartitions) + ";");
        predictionsPrintWriter.print(actualLabels + ";");

        //precision
        predictionsPrintWriter.print(predictionMeasure == null ? "" : predictionMeasure.getPrecision() + ";");
        //recall
        predictionsPrintWriter.print(predictionMeasure == null ? "" : predictionMeasure.getRecall() + ";");
        //f-measure
        predictionsPrintWriter.print(predictionMeasure == null ? "" : predictionMeasure.getFmeasure() + ";");
        predictionsPrintWriter.println();
        //confidences
        printOptionalDoubleOutput(confidences, "CONFIDENCES");
        //pValues
        printOptionalDoubleOutput(pValues, "PVALUES");
        }
        //set prediction results

        if(!StringUtils.isBlank(actualLabels)) {//only add instances that have actual labels (features that we can use to measure precision and recall)

            predictionResultSummary.setClassifier(classifierRecord.getClassifierName());
            predictionResultSummary.setCommit(trainingFile.getName());
            predictionResultSummary.setLabeldInstance(!StringUtils.isBlank(actualLabels) ? 1.0 : 0.0);
            predictionResultSummary.setInstances(1.0);
            predictionResultSummary.setRankedInstance(ranking != null && ranking.length > 0.0 ? 1.0 : 0.0);
            predictionResultSummary.setPrecision(predictionMeasure == null ? Double.NaN : predictionMeasure.getPrecision());
            predictionResultSummary.setRecall(predictionMeasure == null ? Double.NaN : predictionMeasure.getRecall());
            predictionResultSummary.setFscore(predictionMeasure == null ? Double.NaN : predictionMeasure.getFmeasure());

            predictionResultSummaries.add(predictionResultSummary);
        }
    }

    private PredictionMeasure getPredictionMeasureForCluster(List<String> retrievedLabels, String instanceName, List<String> assetMappings,boolean isFragment) {
        List<PredictionMeasure> lineMeasures = new ArrayList<>();
        List<String> lines = getLinesFromCluster(instanceName);
        for (String line : lines) {
            PredictionMeasure measure = getPredictionMeasure(retrievedLabels, getMappedLabels(getMappedLabels(line, assetMappings,false) ));
            if (measure != null) {
                lineMeasures.add(measure);
            }
        }
        double precision = lineMeasures.stream().map(PredictionMeasure::getPrecision).mapToDouble(Double::doubleValue).average().getAsDouble();
        double recall = lineMeasures.stream().map(PredictionMeasure::getRecall).mapToDouble(Double::doubleValue).average().getAsDouble();
        double fmeasure = lineMeasures.stream().map(PredictionMeasure::getFmeasure).mapToDouble(Double::doubleValue).average().getAsDouble();
        PredictionMeasure aggregatedMeasure = new PredictionMeasure(precision, recall, fmeasure);
        return aggregatedMeasure;
    }

    private List<String> getMappedLabels(String mappingString) {
        String[] featureMapping = mappingString == null ? null : mappingString.split(",");
        if (featureMapping == null) {
            return null;
        }
        List<String> actualLabels = new ArrayList<>();
        for (String featureMap : featureMapping) {
            actualLabels.add(featureMap.split(Pattern.quote("["))[0].trim());
        }

        return actualLabels.stream().distinct().collect(Collectors.toList());
    }

    private PredictionMeasure getPredictionMeasure(List<String> retrievedLabels, List<String> mappedLabels) {
        if (mappedLabels == null || mappedLabels.size() <= 0) {
            return null;
        }
        //out of all the retrieved instances, how mnay are actaul labels mapped to this instance
        double present = retrievedLabels.stream().filter(mappedLabels::contains).count();
        double totalRetrieved = retrievedLabels.size();
        double totalRelevant = mappedLabels.size();
        double precision = present / totalRetrieved;
        double recall = present / totalRelevant;
        double fMeasure = 2 * (precision * recall) / (precision + recall);
        return new PredictionMeasure(precision, recall, Double.isNaN(fMeasure)?0.0: fMeasure);
    }

    private List<String> getRetrievedLabels(List<String> dataSetLabels, boolean[] bipartitions) {
        List<String> retrievedLabels = new ArrayList<>();
        for (int i = 0; i < bipartitions.length; i++) {
            if (bipartitions[i]) {
                retrievedLabels.add(dataSetLabels.get(i));
            }
        }

        return retrievedLabels;
    }

    /**
     * Given a list labels i.e. feature names e.g., server, client, bubbleGraph, etc, we want to return a list of ranked labels based on their numerical ranked list positions
     * Additionally, if the option RankRelevantLabelsOnly is set to true then we only want to return Labels marked as Relavnt by the bipartitioning. Relevant labels are marked TRUE
     * in the array of bi[artitions
     *
     * @param labels
     * @param ranking
     * @param bipartitions
     * @return
     */
    private List<String> getLabelNameRanking(List<String> labels, int[] ranking, boolean bipartitions[]) {
        List<String> rankedLabels = new ArrayList<>();
        Arrays.stream(ranking).forEach(r -> {
            if (configuration.isRankRelevantLabelsOnly()) {
                if (bipartitions[r - 1]) {//check that the bipartition value for this rankindex is true; ranks are counted from 1 hence the reason for the subtraction of 1
                    rankedLabels.add(labels.get(r - 1));
                }
            } else {
                rankedLabels.add(labels.get(r - 1));
            }
        });
        return rankedLabels;
    }

    private void printOptionalDoubleOutput(double[] confidences, String outputName) {
        if (confidences != null) {
            predictionsPrintWriter.print("" + ";");//instance name is already printed out
            predictionsPrintWriter.print("" + ";");//classifier name already printed out
            predictionsPrintWriter.print(outputName + ";");

            for (double b : confidences) {
                predictionsPrintWriter.print(b + ";");
            }

            predictionsPrintWriter.print(" " + ";");
            predictionsPrintWriter.print(" " + ";");
            predictionsPrintWriter.print(" " + ";");
            predictionsPrintWriter.print(" " + ";");
            predictionsPrintWriter.println();
        }
    }

    private static List<Measure> prepareMeasuresClassification(MultiLabelInstances mlTrainData) {
        List<Measure> measures = new ArrayList<Measure>();

        int numOfLabels = mlTrainData.getNumLabels();

        // add example-based measures
        measures.add(new HammingLoss());
        measures.add(new SubsetAccuracy());
        measures.add(new ExampleBasedPrecision());
        measures.add(new ExampleBasedRecall());
        measures.add(new ExampleBasedFMeasure());
        measures.add(new ExampleBasedAccuracy());
        measures.add(new ExampleBasedSpecificity());

        // add label-based measures
        measures.add(new MicroPrecision(numOfLabels));
        measures.add(new MicroRecall(numOfLabels));
        measures.add(new MicroFMeasure(numOfLabels));
        measures.add(new MicroSpecificity(numOfLabels));
        //measures.add(new MicroAccuracy(numOfLabels));
        measures.add(new MacroPrecision(numOfLabels));
        measures.add(new MacroRecall(numOfLabels));
        measures.add(new MacroFMeasure(numOfLabels));
        measures.add(new MacroSpecificity(numOfLabels));
        //measures.add(new MacroAccuracy(numOfLabels));

        // add ranking based measures
        measures.add(new AveragePrecision());
        measures.add(new Coverage());
        measures.add(new OneError());
        measures.add(new IsError());
        measures.add(new ErrorSetSize());
        measures.add(new RankingLoss());

        // add confidence measures if applicable
        measures.add(new MeanAveragePrecision(numOfLabels));
        measures.add(new GeometricMeanAveragePrecision(numOfLabels));
//        measures.add(new MicroAUC(numOfLabels));
//        measures.add(new MacroAUC(numOfLabels));
        //System.out.println(measures);
        return measures;
    }

    protected static List<Measure> prepareMeasuresRegression(MultiLabelInstances mlTrainData, MultiLabelInstances mlTestData) {
        List<Measure> measures = new ArrayList<Measure>();

        int numOfLabels = mlTrainData.getNumLabels();
        measures.add(new MacroMAE(numOfLabels));
        measures.add(new MacroRMSE(numOfLabels));
        measures.add(new MacroRelMAE(mlTrainData, mlTestData));
        measures.add(new MacroRelRMSE(mlTrainData, mlTestData));

        measures.add(new MacroMaxAE(numOfLabels));
        measures.add(new MacroRMaxSE(numOfLabels));

        measures.add(new ExampleBasedRMaxSE());

        return measures;
    }

    private double getValue(Measure measure) {
        return kFolds > 0 ? mResults.getMean(measure.getName()) : measure.getValue();
    }

    private void setMeasureValue(Measure measure, ResultMeasure resultMeasure) {

        if (measure.getName().equalsIgnoreCase("Hamming Loss")) {
            resultMeasure.setHammingLoss(getValue(measure));
        } else if (measure.getName().equalsIgnoreCase("Subset Accuracy")) {
            resultMeasure.setSubsetAccuracy(getValue(measure));
        } else if (measure.getName().equalsIgnoreCase("Example-Based Precision")) {
            resultMeasure.setExampleBasedPrecision(getValue(measure));
        } else if (measure.getName().equalsIgnoreCase("Example-Based Recall")) {
            resultMeasure.setExampleBasedRecall(getValue(measure));
        } else if (measure.getName().equalsIgnoreCase("Example-Based F Measure")) {
            resultMeasure.setExampleBasedFMeasure(getValue(measure));
        } else if (measure.getName().equalsIgnoreCase("Example-Based Accuracy")) {
            resultMeasure.setExampleBasedAccuracy(getValue(measure));
        } else if (measure.getName().equalsIgnoreCase("Example-Based Specificity")) {
            resultMeasure.setExampleBasedSpecificity(getValue(measure));
        } else if (measure.getName().equalsIgnoreCase("Micro-averaged Precision")) {
            resultMeasure.setMicroPrecision(getValue(measure));
        } else if (measure.getName().equalsIgnoreCase("Micro-averaged Recall")) {
            resultMeasure.setMicroRecall(getValue(measure));
        } else if (measure.getName().equalsIgnoreCase("Micro-averaged F-Measure")) {
            resultMeasure.setMicroFMeasure(getValue(measure));
        } else if (measure.getName().equalsIgnoreCase("Micro-averaged Specificity")) {
            resultMeasure.setMicroSpecificity(getValue(measure));
        } else if (measure.getName().equalsIgnoreCase("Macro-averaged Precision")) {
            resultMeasure.setMacroPrecision(getValue(measure));
        } else if (measure.getName().equalsIgnoreCase("Macro-averaged Recall")) {
            resultMeasure.setMacroRecall(getValue(measure));
        } else if (measure.getName().equalsIgnoreCase("Macro-averaged F-Measure")) {
            resultMeasure.setMacroFMeasure(getValue(measure));
        } else if (measure.getName().equalsIgnoreCase("Macro-averaged Specificity")) {
            resultMeasure.setMicroSpecificity(getValue(measure));
        } else if (measure.getName().equalsIgnoreCase("Average Precision")) {
            resultMeasure.setAveragePrecision(getValue(measure));
        } else if (measure.getName().equalsIgnoreCase("Coverage")) {
            resultMeasure.setCoverage(getValue(measure));
        } else if (measure.getName().equalsIgnoreCase("OneError")) {
            resultMeasure.setOneError(getValue(measure));
        } else if (measure.getName().equalsIgnoreCase("IsError")) {
            resultMeasure.setIsError(getValue(measure));
        } else if (measure.getName().equalsIgnoreCase("ErrorSetSize")) {
            resultMeasure.setErrorSetSize(getValue(measure));
        } else if (measure.getName().equalsIgnoreCase("Ranking Loss")) {
            resultMeasure.setRankingLoss(getValue(measure));
        } else if (measure.getName().equalsIgnoreCase("Mean Average Precision")) {
            resultMeasure.setMeanAveragePrecision(getValue(measure));
        } else if (measure.getName().equalsIgnoreCase("Geometric Mean Average Precision")) {
            resultMeasure.setGeometricMeanAveragePrecision(getValue(measure));
        }

    }


}
