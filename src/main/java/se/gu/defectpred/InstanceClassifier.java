package se.gu.defectpred;

import com.google.common.base.Stopwatch;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.io.FileUtils;
import se.gu.main.Configuration;
import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.GreedyStepwise;
import weka.attributeSelection.PrincipalComponents;
import weka.attributeSelection.Ranker;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.Logistic;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.functions.SMO;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.unsupervised.attribute.Remove;
import weka.gui.visualize.PlotData2D;
import weka.gui.visualize.ThresholdVisualizePanel;

import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class InstanceClassifier {
    //Relief based ranking of attributes for file-with feature metrics
    //"avgc","maxc","aage","wage","auth","mods","ddev","adem","bugf","nloc","cyco","refa","modd","comm","cchm","adda","addm","revi","rema","freml","ccha","faddl","remm","reml","exp","oexp","addl","cchl"
    private String[] fileAttributes = new String[]{"26", "25", "27", "28", "15", "7", "3", "2", "14", "8", "9", "13", "6", "1", "23", "18", "17", "12", "21", "11", "24", "10", "20", "19", "4", "5", "16", "22"};
    //file only metrics
    //"avgc","maxc","aage","wage","auth","bugf","cchm","refa","adda","addm","revi","rema","ccha","remm","reml","addl","cchl"
    private String[] fileOnlyAttributes = new String[]{"15","14","16","17","4","3","12","2","7","6","1","10","13","9","8","5","11"};

    //"faddl","fcyco","foexp","freml","fnloc","scat","fddev","fcomm","fadev","fmodd","lofc","ndep","tanga","fexp"
    private String[] featureAttributes = new String[]{"9", "8", "5", "10", "7", "11", "3", "1", "2", "6", "14", "13", "12", "4"};
    private Configuration configuration;
    private List<LabeledFeature> labeledFeatures;
    private List<LabeledFile> labeledFiles;

    public InstanceClassifier(Configuration configuration) {
        this.configuration = configuration;

    }

    private List<Classifier> classifiers = new ArrayList<>();

    private void createClassifiers() {
        //J48
        classifiers = null;
        classifiers = new ArrayList<>();
        RandomForest randomForest = new RandomForest();
        classifiers.add(randomForest);
    }

    public void loadLabeledFeatures() throws IOException {
        File f = new File(configuration.getLabeledFeaturesPath());
        List<String> fileLines = FileUtils.readLines(f, configuration.getTextEncoding());
        labeledFeatures = new ArrayList<>();
        //dataset;project;release_number;feature;fcomm;fadev;fddev;fexp;foexp;fmodd;fnloc;fcyco;faddl;freml;scat;tanga;ndep;lofc;label;Predicted
        for (int line = 1; line < fileLines.size(); line++) {
            String row[] = fileLines.get(line).split(";");
            labeledFeatures.add(new LabeledFeature(row[1], row[2], row[3], row[row.length - 2], row[row.length - 1]));//project;release_number;feature;label;Predicted
        }

    }

    public void loadLabeledFiles() throws IOException {
        File f = new File(configuration.getLabeledFilesPath());
        List<String> fileLines = FileUtils.readLines(f, configuration.getTextEncoding());
        labeledFiles = new ArrayList<>();
        //dataset;project;release_number;filename;comm;adev;ddev;exp;oexp;modd;mods;nloc;cyco;faddl;freml;revi;refa;bugf;auth;addl;addm;adda;reml;remm;rema;cchl;cchm;ccha;maxc;avgc;aage;wage;label;Predicted
        for (int line = 1; line < fileLines.size(); line++) {
            String row[] = fileLines.get(line).split(";");
            labeledFiles.add(new LabeledFile(row[1], row[2], row[3], row[row.length - 2], row[row.length - 1]));//project;release_number;feature;label;Predicted
        }

    }

    private int getN(String defectLabel) {
        if (defectLabel.equalsIgnoreCase("defective")) {
            return 1;
        } else {
            return 0;
        }
    }

    public void mapLabelsToFeaturesAndFiles() throws IOException {
        //mapping file columns
        //project;release_number;feature;featureLabel;featureLabelN;predictedFeatureLabel;predictedFeatureLabelN;filename;fileLabel;fileLabelN;predictedFileLabel;predictedFileLabelN
        loadLabeledFeatures();
        loadLabeledFiles();
        File mappingFile = new File(configuration.getFeatureFileMapping());
        List<String> fileLines = FileUtils.readLines(mappingFile, configuration.getTextEncoding());
        File newMappedFile = new File(String.format("%s_mapped.csv", mappingFile.getAbsolutePath().replace(".csv", "")));
        if (newMappedFile.exists()) {
            FileUtils.forceDelete(newMappedFile);
        }
        PrintWriter printWriter = new PrintWriter(new FileWriter(newMappedFile, true));
//header
        printWriter.println("project;release_number;feature;featureLabel;featureLabelN;predictedFeatureLabel;predictedFeatureLabelN;filename;fileLabel;fileLabelN;predictedFileLabel;predictedFileLabelN");
        int size = fileLines.size();
        String featureLabel, predictedFeatureLabel, fileLabel, predictedFileLabel;
        int featureLabelN, predictedFeatureLabelN, fileLabelN, predictedFileLabelN;
        for (int line = 1; line < size; line++) {
            String[] row = fileLines.get(line).split(";");
            String project = row[0], release = row[1], feature = row[2], fileName = row[7];
            featureLabel = row[3];
            featureLabelN = Integer.parseInt(row[4]);
            fileLabel = row[8];
            fileLabelN = Integer.parseInt(row[9]);
            //get Labeled Feature
            Optional<LabeledFeature> optionalLabeledFeature = labeledFeatures.parallelStream()
                    .filter(f -> f.getProject().equalsIgnoreCase(project) && f.getRelease_number().equalsIgnoreCase(release) && f.getFeature().equalsIgnoreCase(feature))
                    .findAny();
            if (optionalLabeledFeature.isPresent()) {

                predictedFeatureLabel = optionalLabeledFeature.get().getPredicted();
                predictedFeatureLabelN = getN(predictedFeatureLabel);

                //get mapped file
                Optional<LabeledFile> optionalLabeledFile = labeledFiles.parallelStream()
                        .filter(f -> f.getProject().equalsIgnoreCase(project) && f.getRelease_number().equalsIgnoreCase(release) && f.getFile().equalsIgnoreCase(fileName))
                        .findAny();
                if (optionalLabeledFile.isPresent()) {
                    predictedFileLabel = optionalLabeledFile.get().getPredicted();
                    predictedFileLabelN = getN(predictedFileLabel);

                    //only consider cases where both features and files exist
                    printWriter.printf("%s;%s;%s;%s;%d;%s;%d;%s;%s;%d;%s;%d\n",
                            project, release, feature, featureLabel, featureLabelN, predictedFeatureLabel, predictedFeatureLabelN,
                            fileName, fileLabel, fileLabelN, predictedFileLabel, predictedFileLabelN);//project;release_number;feature

                }

            }
        }
        printWriter.close();


    }

    public void classifyInstances() throws Exception {

        String[] trainingFiles = configuration.getTrainDataFile().split(",");
        String[] testFiles = configuration.getTestDataFile().split(",");
        for (int f = 0; f < trainingFiles.length; f++) {
            //createClassifiers();//create classfiers for each dataset to be trained
            RandomForest classifier = new RandomForest();
            String trainDataFile = trainingFiles[f];
            String testDataFile = testFiles[f];
            String groundTruthCSV = testDataFile.replace("arff", "csv");
            System.out.println("Working on " + trainDataFile);
            File tFile = new File(trainDataFile);
            if(!tFile.exists()){
                throw  new Exception("training file does not exist");
            }

            Instances data = DPUtilities.getTrainingDataset(trainDataFile,false,true);
            System.out.println("building " + classifier.toString() + "...");
            classifier.buildClassifier(data);

            // load unlabeled data
            Instances unlabeled = new Instances(new BufferedReader(new FileReader(testDataFile)));
            // set class attribute
            unlabeled.setClassIndex(unlabeled.numAttributes() - 1);
            // create copy
            Instances labeled = new Instances(unlabeled);
            List<String> fileLines = FileUtils.readLines(new File(groundTruthCSV), configuration.getTextEncoding());
            //now predict
            //for (Classifier classifier : classifiers) {
                File groundTruthLabeled = new File(groundTruthCSV.replace("unlabeled", "labeled"));
                if (groundTruthLabeled.exists()) {
                    FileUtils.forceDelete(groundTruthLabeled);
                }

                PrintWriter printWriter = new PrintWriter(new FileWriter(groundTruthLabeled, true));
                try {
                    //write header
                    printWriter.printf("%s;Predicted\n", fileLines.get(0));
                    int numInstances = unlabeled.numInstances();
                    // label instances
                    try (ProgressBar pb = new ProgressBar("classifying with RF", numInstances)) {
                        for (int i = 0; i < numInstances; i++) {
                            pb.step();
                            double clsLabel = classifier.classifyInstance(unlabeled.instance(i));
                            labeled.instance(i).setClassValue(clsLabel);
                            printWriter.printf("%s%s\n", fileLines.get(i + 1), unlabeled.classAttribute().value((int) clsLabel));
                        }
                    }
                    // save labeled data
                    String labledFile = testDataFile.replace("unlabeled", "labeled");
                    BufferedWriter writer = new BufferedWriter(new FileWriter(labledFile));
                    writer.write(labeled.toString());
                    writer.newLine();
                    writer.flush();
                    writer.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    printWriter.close();
                }

            //}


        }
    }

    public Map<String[], List<Classifier>> getAllClassifiers() {
        List<Classifier> classifierList = new ArrayList<>();
        classifierList.add(new J48());
        RandomForest randomForest = new RandomForest();
        //randomForest.setNumTrees(200);
        classifierList.add(randomForest);
        classifierList.add(new NaiveBayes());
        classifierList.add(new Logistic());
        classifierList.add(new IBk());
        classifierList.add(new SMO());
        MultilayerPerceptron multilayerPerceptron = new MultilayerPerceptron();
        //multilayerPerceptron.setOptions(Utils.splitOptions("-L 0.3 -M 0.2 -N 500 -V 0 -S 0 -E 20 -H \"a, 13, 13, 13\""));
        classifierList.add(multilayerPerceptron);
        String[] classifierNames = new String[]{"J48", "RF", "NB", "LR", "kNN", "SVM", "NN"};
        Map<String[], List<Classifier>> map = new HashMap<>();
        map.put(classifierNames, classifierList);
        return map;
    }

    public void evaluateClassifiers() throws Exception {
        //Create classifiers
        Map<String[], List<Classifier>> map = getAllClassifiers();
        Map.Entry<String[], List<Classifier>> entry = map.entrySet().iterator().next();
        String[] classifierNames = entry.getKey();
        List<Classifier> classifierList = entry.getValue();


        String trainSet = configuration.getTrainDataFile().split(",")[1];
        String testSet = configuration.getTestDataFile().split(",")[1];
        ConverterUtils.DataSource source = new ConverterUtils.DataSource(trainSet);
        Instances data = source.getDataSet();
        data.setClassIndex(data.numAttributes() - 1);

        Instances unlabeled = new Instances(
                new BufferedReader(
                        new FileReader(testSet)));
        unlabeled.setClassIndex(unlabeled.numAttributes() - 1);

        File evaluationResults = new File(String.format("%s/filesEvaluation.csv", new File(trainSet).getParent()));
        File evaluationResultsText = new File(String.format("%s/filesEvaluationDetailed.txt", new File(trainSet).getParent()));
        if (evaluationResults.exists()) {
            FileUtils.forceDelete(evaluationResults);
        }
        PrintWriter printWriter = new PrintWriter(new FileWriter(evaluationResults, true));
        PrintWriter printWriterText = new PrintWriter(new FileWriter(evaluationResultsText, true));
        printWriter.println("Classifier;ROC;TimeElapsed");

        List<Instances> resultList = new ArrayList<>();

        try (ProgressBar pb = new ProgressBar("evaluating ", classifierList.size())) {
            for (int c = 0; c < classifierList.size(); c++) {
                Stopwatch stopwatch = Stopwatch.createStarted();
                pb.step();
                pb.setExtraMessage(classifierNames[c]);
                classifierList.get(c).buildClassifier(data);
                // evaluate classifier and print some statistics
                Evaluation eval = new Evaluation(data);
                eval.evaluateModel(classifierList.get(c), unlabeled);

                // generate curve
//                ThresholdCurve tc = new ThresholdCurve();
//
//                int classIndex = 0;
//                Instances result = tc.getCurve(eval.predictions(), classIndex);
//
//                resultList.add(result);
                stopwatch.stop();
                printWriter.printf("%s;%.4f;%s\n", classifierNames[c], eval.areaUnderROC(0), stopwatch.elapsed(TimeUnit.SECONDS));
                printWriterText.println(eval.toSummaryString(String.format("\n%s Results\n======\n", classifierNames[c]), false));


            }
        }
        printWriter.close();
        printWriterText.close();
        System.out.println("Generating ROC curves");
        //createROCCurves(resultList);
    }

    public void createROCCurves(List<Instances> resultList) throws Exception {
        boolean first = true;
        ThresholdVisualizePanel vmc = new ThresholdVisualizePanel();
        for (Instances result : resultList) {

            result.setClassIndex(result.numAttributes() - 1);
            // method visualize
            PlotData2D tempd = new PlotData2D(result);
            tempd.setPlotName(result.relationName());
            tempd.addInstanceNumberAttribute();
            // specify which points are connected
            boolean[] cp = new boolean[result.numInstances()];
            for (int n = 1; n < cp.length; n++)
                cp[n] = true;
            tempd.setConnectPoints(cp);
            // add plot
            if (first)
                vmc.setMasterPlot(tempd);
            else
                vmc.addPlot(tempd);
            first = false;
        }
        // method visualizeClassifierErrors
        final javax.swing.JFrame jf =
                new javax.swing.JFrame("Weka Classifier ROC");
        jf.setSize(500, 400);
        jf.getContentPane().setLayout(new BorderLayout());

        jf.getContentPane().add(vmc, BorderLayout.CENTER);
        jf.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                jf.dispose();
            }
        });

        jf.setVisible(true);
    }


    public Instances getFilteredDataByPCA(Instances data) throws Exception {
        AttributeSelection filter = new AttributeSelection();
        PrincipalComponents eval = new PrincipalComponents();
        Ranker search = new Ranker();
        filter.setEvaluator(eval);
        filter.setSearch(search);
        filter.setInputFormat(data);
        Instances newData = Filter.useFilter(data, filter);
        return newData;

    }

    public Instances getFilteredDataByCFS(Instances data) throws Exception {
        AttributeSelection filter = new AttributeSelection();
        CfsSubsetEval eval = new CfsSubsetEval();
        GreedyStepwise search = new GreedyStepwise();
        search.setSearchBackwards(true);
        filter.setEvaluator(eval);
        filter.setSearch(search);
        filter.setInputFormat(data);
        Instances newData = Filter.useFilter(data, filter);
        return newData;

    }

    private String[] attributesToRemove;

    public String[] getAttributesToRemove(String[] attributes, double percentage) throws Exception {
        //this is how ReleifF ranked the attributes
        //get attributes to remove
        if(percentage>=1.0){
            return null;
        }
        int attributesToUse = (int) Math.round(attributes.length * percentage);
        int start = attributesToUse;
        int end = attributes.length - 1;
        String[] copy = Arrays.copyOfRange(attributes, start, end);
        String[] toRemove = new String[2];
        toRemove[0] = "-R";
        toRemove[1] = Arrays.asList(copy).stream().collect(Collectors.joining(","));
        return toRemove;

    }

    public String getAttributesToRemoveAsString(String[] attributes, double percentage) throws Exception {
        //this is how ReleifF ranked the attributes
        //get attributes to remove
        if(percentage>=1.0){
            return null;
        }
        int attributesToUse = (int) Math.round(attributes.length * percentage);
        int start = attributesToUse;
        int end = attributes.length - 1;
        String[] copy = Arrays.copyOfRange(attributes, start, end);

        return Arrays.asList(copy).stream().collect(Collectors.joining(","));


    }

    public Instances getFilteredDataByReliefF(Instances data, String[] attributesToRemove) throws Exception {
        Remove remove = new Remove();
        remove.setOptions(attributesToRemove);
        remove.setInputFormat(data);
        Instances newData = Filter.useFilter(data, remove);
        return newData;
    }

    public void evaluateAttributes() throws Exception {

        String[] trainingFiles = configuration.getTrainDataFile().split(",");
        String[] testFiles = configuration.getTestDataFile().split(",");
        File evaluationResults = new File(String.format("%s/AttributeEvaluation.csv", new File(trainingFiles[0]).getParent()));

        if (evaluationResults.exists()) {
            FileUtils.forceDelete(evaluationResults);
        }
        PrintWriter printWriter = new PrintWriter(new FileWriter(evaluationResults, true));
        printWriter.println("dataset;classifier;selection;ROC;TimeElapsed");
        for (int f = 0; f < trainingFiles.length; f++) {
            //intialize clasifiers
            Map<String[], List<Classifier>> map = getAllClassifiers();
            Map.Entry<String[], List<Classifier>> entry = map.entrySet().iterator().next();
            String[] classifierNames = entry.getKey();
            List<Classifier> classifierList = entry.getValue();//create classfiers for each dataset to be trained

            String trainDataFile = trainingFiles[f];
            String testDataFile = testFiles[f];
            String trainingFileNameOnly = new File(trainDataFile).getName();

            System.out.println("Working on " + trainDataFile);

            ConverterUtils.DataSource source = new ConverterUtils.DataSource(trainDataFile);
            Instances data = source.getDataSet();
            // setting class attribute if the data format does not provide this information
            // For example, the XRFF format saves the class attribute information as well
            if (data.classIndex() == -1) {
                data.setClassIndex(data.numAttributes() - 1);
            }
            // load unlabeled data
            Instances unlabeled = new Instances(
                    new BufferedReader(
                            new FileReader(testDataFile)));

            // set class attribute
            unlabeled.setClassIndex(unlabeled.numAttributes() - 1);

            //use Relief only for now
            String [] attributeSelection = new String[]{"cfs","pca","0.25", "0.50", "0.75", "1.0"};//double values stands for ReliefF. here we evaluate datasets based on 25%,50%,75% and 100% of attributes




            //build model
            Instances newTrainingData = null, newTestData = null;
            try (ProgressBar pb = new ProgressBar("eval.", classifierNames.length)) {
                for (int classifier = 0; classifier < classifierList.size(); classifier++) {
                    pb.step();

                    for (String selection : attributeSelection) {
                        try {
                            pb.setExtraMessage(String.format("%s, %s, %s", trainDataFile.toLowerCase().contains("feature") ? "feat." : trainDataFile.toLowerCase().contains("only")?"file_o": "file", classifierNames[classifier], selection));
                            if(selection.equalsIgnoreCase("cfs")){
                                newTrainingData = getFilteredDataByCFS(data);
                            }else if(selection.equalsIgnoreCase("pca")){
                                newTrainingData=getFilteredDataByPCA(data);
                            }else {
                                attributesToRemove = getAttributesToRemove(trainDataFile.toLowerCase().contains("feature") ? featureAttributes : trainDataFile.toLowerCase().contains("only")?fileOnlyAttributes: fileAttributes, Double.parseDouble(selection));
                                newTrainingData = attributesToRemove == null ? data : getFilteredDataByReliefF(data, attributesToRemove);
                            }
                            newTestData = unlabeled;// attributesToRemove == null ? unlabeled : getFilteredDataByReliefF(unlabeled, attributesToRemove);
                            Stopwatch stopwatch = Stopwatch.createStarted();
                            classifierList.get(classifier).buildClassifier(newTrainingData);
                            Evaluation eval = new Evaluation(data);
                            eval.evaluateModel(classifierList.get(classifier), newTestData);
                            stopwatch.stop();
                            printWriter.printf("%s;%s;%s;%.4f;%s\n", trainingFileNameOnly, classifierNames[classifier], selection, eval.areaUnderROC(0), stopwatch.elapsed(TimeUnit.SECONDS));
                        }catch (Exception ex){
                            ex.printStackTrace();
                        }
                    }


                }
            }



        }
        printWriter.close();
    }

}
