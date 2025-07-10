package se.gu.ml.preprocessing;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import org.repodriller.scm.SCM;
import se.gu.assets.Asset;
import se.gu.assets.AssetType;
import se.gu.assets.FeatureAssetMap;
import se.gu.main.ProjectData;
import se.gu.metrics.structure.AssetMetrics;
import se.gu.ml.experiment.ExperiementFileRecord;
import se.gu.parser.fmparser.FeatureTreeNode;
import se.gu.utils.LocalExecutionRunner;
import se.gu.utils.Utilities;
import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;
import uk.ac.shef.wit.simmetrics.similaritymetrics.CosineSimilarity;
import weka.core.*;
import weka.core.converters.ConverterUtils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DataGenerator implements Serializable {

    private static final long serialVersionUID = -4089655976682627459L;
    private ProjectData projectData;
    private String currentCommit, previousCommit;
    private LocalExecutionRunner metricsExecutorService;
    private Map<String, Double> attributeValues;
    private int currentCommitIndex, previousCommitIndex;
    private SCM scm;

    private List<Asset> unlabledAssetTargets;
    private List<Asset> unlabledChangedAssets;

    public DataGenerator(int currentCommitIndex, int previousCommitIndex, String currentCommit, String previousCommit, ProjectData projectData, LocalExecutionRunner metricsExecutorService, SCM scm) {

        this.currentCommitIndex = currentCommitIndex;
        this.previousCommitIndex = previousCommitIndex;
        this.currentCommit = currentCommit;
        this.previousCommit = previousCommit;
        this.projectData = projectData;
        this.metricsExecutorService = metricsExecutorService;
        attributeValues = getAttributeValues();
        this.scm = scm;
    }

    private Instances initializeInstances() {
        ArrayList<Attribute> atts;
        //1. setup the attributes
        atts = new ArrayList<>();
        // - classes (features)
        List<AttributeName> attributeNames = projectData.getMLAttributes();
        long numbeOfFeatures = attributeNames.stream().filter(a -> !a.isMLFeature()).count();
        for (AttributeName attribute : attributeNames) {
            if (attribute.isMLFeature()) {
                atts.add(new Attribute(attribute.getName()));
            } else {
                atts.add(new Attribute(attribute.getName(), Arrays.asList(new String[]{"0", "1"})));
            }
        }
        // 2. create Instances object to hold the data
        //-C -d means that the labels are the last d attributes

        Instances data = new Instances(String.format("FeaTraceR-Commit_%d-%s-(%d features): -C -%d", currentCommitIndex + 1, currentCommit, numbeOfFeatures, numbeOfFeatures), atts, 0);
        return data;
    }

    public void createARFF() throws Exception {
        Instances data = initializeInstances();

        //3. create the data
        updateAttributesForExistingDataInstances();

        Map<String, Map<String, Double>> dataInstances = null;
        //& begin [ML:PreProcessing::Normalization]
        if (projectData.getConfiguration().isNormaliseData() && projectData.getConfiguration().getNormalisationStrategy() == NormalisationStrategy.FTR) {
            dataInstances = getNormalizedDataInstances(projectData.getMlDataSet());
        } else {
            dataInstances = projectData.getMlDataSet();
        }

        //& end [ML:PreProcessing::Normalization]

        for (String instance : dataInstances.keySet()) {
            //remove unused features
            //removeUnusedFeatures(instance);
            //double[] vals = instance.getAttributeValues().values().stream().mapToDouble(d -> d == -1.0 ? Utils.missingValue() : d).toArray();
            data.add(getWekaInstance(dataInstances.get(instance)));
        }

        //& begin [ML:PreProcessing::Normalization]
        if (projectData.getConfiguration().isNormaliseData() && projectData.getConfiguration().getNormalisationStrategy() == NormalisationStrategy.WEKA) {
            //Normalise the data using WEKA filters
            data = normalizeData(data);
        }
        //& end [ML:PreProcessing::Normalization]


        //4 write file
        String dataSubFolder = projectData.getConfiguration().getDataFilesImbalancedDirectory();

        File dataFilesDirectory = Utilities.createOutputDirectory(String.format("%s/%d_%s", dataSubFolder, currentCommitIndex + 1, currentCommit), true);
        String arffFileName = String.format("%s/%d_%s-%s.arff", dataFilesDirectory, currentCommitIndex + 1, currentCommit, projectData.getConfiguration().getCodeAbstractionLevel());
        ConverterUtils.DataSink.write(arffFileName, data);

        //write the feature model XML file also
        String hierachicalXMlFile = String.format("%s/%d_%s-%s.xml", dataFilesDirectory, currentCommitIndex + 1, currentCommit, projectData.getConfiguration().getCodeAbstractionLevel());
        String flatXMlFile = String.format("%s/%d_%s-%s-FLAT.xml", dataFilesDirectory, currentCommitIndex + 1, currentCommit, projectData.getConfiguration().getCodeAbstractionLevel());

        Utilities.writeStringFile(hierachicalXMlFile, getXMLModel(projectData.getRootFeature()));
        Utilities.writeStringFile(flatXMlFile, getFlatXMLModel());


        projectData.getExperiementFileRecords().add(new ExperiementFileRecord(new File(arffFileName), new File(hierachicalXMlFile), new File(flatXMlFile), currentCommit));

    }


    private void removeNonMappedAssets() {
        List<String> mappedAssetNames = projectData.getAssetFeatureMap().parallelStream().map(FeatureAssetMap::getMappedAsset).map(Asset::getFullyQualifiedName).distinct().collect(Collectors.toList());
        Set<String> keys = projectData.getMlDataSet().keySet();
        for (String key : keys) {
            if (!mappedAssetNames.contains(key)) {
                projectData.getMlDataSet().remove(key);
            }
        }
    }

    private Map<String, Map<String, Double>> getNormalizedDataInstances(Map<String, Map<String, Double>> mlData) {

        Map<String, Map<String, Double>> dataInstances = normalizeData(mlData);

        return dataInstances;
    }

    private void removeNaNs(List<DataInstance> instances) {
        List<String> attributeNames = projectData.getMLAttributes().stream().filter(a -> a.isMLFeature()).map(AttributeName::getName).collect(Collectors.toList());

        for (DataInstance dataInstance : instances) {
            for (String attribute : dataInstance.getAttributeValues().keySet()) {
                if (!attributeNames.contains(attribute)) {
                    continue;
                }
                Double value = dataInstance.getAttributeValues().get(attribute);
                if (value.isNaN() || value.isInfinite()) {
                    dataInstance.getAttributeValues().put(attribute, -1.0);
                }

            }
        }
    }

    //& begin [ML:PreProcessing::Normalization]
    public Instances normalizeData(Instances data) throws Exception {
        Normalize normalizer = new Normalize();
        normalizer.setInputFormat(data);
        data = Filter.useFilter(data, normalizer);
        return data;
    }


    public Map<String, Map<String, Double>> normalizeData(Map<String, Map<String, Double>> instances) {
//        get features of ML data
        Map<String, Map<String, Double>> normalizedInstances = new HashMap<>(instances);


        List<String> attributes = projectData.getConfiguration().getAttributesToNormalize();
        double max, min;
        List<Map<String, Double>> attributeValues = new ArrayList<>(instances.values());

        for (String attribute : attributes) {
            List<Double> values = new ArrayList<>();

            for (Map<String, Double> m : attributeValues) {
                values.add(m.get(attribute));
            }

            //min and max
            min = values.stream().mapToDouble(Double::doubleValue).filter(d -> d != -1.0).min().orElse(0.0);//exclude the -1s
            max = values.stream().mapToDouble(Double::doubleValue).filter(d -> d != -1.0).max().orElse(0.0);

            //normalize
            for (String asset : normalizedInstances.keySet()) {
                Double value = normalizedInstances.get(asset).get(attribute);
                Double normalised = value == -1.0 ? Double.NaN : Math.abs((value - min) / (max - min));
                normalised = (normalised.isNaN() || normalised.isInfinite()) && value != -1.0 && !value.isNaN() ? value : normalised;
                normalised = normalised.isNaN() ? -1 : normalised;//if after setting, normalised is still NaN, hen assign missing value
                normalizedInstances.get(asset).put(attribute, normalised);
            }


        }
        return normalizedInstances;
    }

    //& end [ML:PreProcessing::Normalization]
    public void createUnlabledARFF() throws Exception {
        //there must be two or more experiment file records existing in order to create an unlabled dataset file
        //this is because we use the previous commit to make predictions on the current changeset
        if (projectData.getExperiementFileRecords().size() < 2) {
            return;
        }
        updateAttributesForUnlabaledInstances();
        Instances unlabledData = initializeInstances();
        String dataSubFolder = projectData.getConfiguration().getDataFilesImbalancedDirectory();
        File dataFilesDirectory = Utilities.createOutputDirectory(String.format("%s/%d_%s", dataSubFolder, previousCommitIndex + 1, previousCommit), false);
        Map<String, Map<String, Double>> unlabledDataInstances = null;
        //&begin Normalization
        if (projectData.getConfiguration().isNormaliseData() && projectData.getConfiguration().getNormalisationStrategy() == NormalisationStrategy.FTR) {
            unlabledDataInstances = getNormalizedDataInstances(projectData.getUnlabledMLDataSet());
        } else {
            unlabledDataInstances = projectData.getUnlabledMLDataSet();
        }

        //&end Normalization
        for (String instance : unlabledDataInstances.keySet()) {
            unlabledData.add(getWekaInstance(unlabledDataInstances.get(instance)));
        }
        //&begin Normalization
        if (projectData.getConfiguration().isNormaliseData() && projectData.getConfiguration().getNormalisationStrategy() == NormalisationStrategy.WEKA) {
            unlabledData = normalizeData(unlabledData);
        }
        //&end Normalization
        String predictionARFFFileName = String.format("%s/%d_%s-%s-PREDICT.arff", dataFilesDirectory, previousCommitIndex + 1, previousCommit, projectData.getConfiguration().getCodeAbstractionLevel());
        ConverterUtils.DataSink.write(predictionARFFFileName, unlabledData);

        //create feature model used for predictions
        String predictionXMLFileName = String.format("%s/%d_%s-%s-P-XML.xml", dataFilesDirectory, previousCommitIndex + 1, previousCommit, projectData.getConfiguration().getCodeAbstractionLevel());

        Utilities.writeStringFile(predictionXMLFileName, getPredictionFlatXMLModel());

        //data file with intance names in the order they appear in the arff file
        String instanceNameFile = String.format("%s/%d_%s-%s-INSTD.data", dataFilesDirectory, previousCommitIndex + 1, previousCommit, projectData.getConfiguration().getCodeAbstractionLevel());
        List<String> instanceNames = new ArrayList<>(unlabledDataInstances.keySet());
        Utilities.writeListFile(instanceNameFile, instanceNames);

        String mappingsFile = createUnlabledAssetsMappingsFile(dataFilesDirectory);


        Optional<ExperiementFileRecord> experiementFileRecord = projectData.getExperiementFileRecords().stream().filter(f -> f.getCommit().equalsIgnoreCase(previousCommit)).findAny();
        if (experiementFileRecord.isPresent()) {
            experiementFileRecord.get().setPredictionFile(new File(predictionARFFFileName));
            experiementFileRecord.get().setPredictionsXMLFile(new File(predictionXMLFileName));
            experiementFileRecord.get().setInstanceNameFile(new File(instanceNameFile));
            experiementFileRecord.get().setAssetMappingsFile(new File(mappingsFile));
        }

    }

    /**
     * @param instance
     * @return
     * @attributeValues represnts list of all metrics and label features as of the curent currentCommit. The order of these attributes may be diferent from the order
     * presented in existing instances. E.g., the current list maybe: SLD, CSM,...server,backend,chocosingle
     * For an old instance that may not have had label server, the order maybe diffeent. Hence we harmonise the order in this method
     */
    private Instance getWekaInstance(Map<String, Double> instance) {
        double[] vals = new double[attributeValues.keySet().size()];
        int index = 0;
        for (String key : attributeValues.keySet()) {

            double value = instance.containsKey(key) ? instance.get(key) : -1.0;
            vals[index] = value == -1.0 ? Utils.missingValue() : value;
            index++;
        }
        return projectData.getConfiguration().isUseSparseInstance() ? new SparseInstance(1.0, vals) : new DenseInstance(1.0, vals);

    }

    private String createUnlabledAssetsMappingsFile(File dataFilesDirectory) throws IOException {
        String mappingsFile = String.format("%s/%d_%s-%s-ULDMAP.data", dataFilesDirectory, previousCommitIndex + 1, previousCommit, projectData.getConfiguration().getCodeAbstractionLevel());
        List<String> mappingStrings = new ArrayList<>();
        //AssetType assetType = Utilities.getAssetType(projectData.getConfiguration().getCodeAbstractionLevel());
        unlabledChangedAssets = projectData.getAssetsChangedInCommitAll(currentCommit); //projectData.getAssetsForPredictionComparison(assetType);
        for (Asset asset : unlabledChangedAssets) {
            List<FeatureAssetMap> mappings = projectData.getAncestralMappingForAsset(asset);
            if (mappings.size() <= 0) {
                continue;
            }
            String line = String.format("%s->%s", asset.getFullyQualifiedName(), getCommaSeparatedMappings(mappings));
            mappingStrings.add(line);
        }
        Utilities.writeListFile(mappingsFile, mappingStrings);
        return mappingsFile;
    }

    private String getCommaSeparatedMappings(List<FeatureAssetMap> mappings) {

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < mappings.size(); i++) {
            if (i == 0) {
                stringBuilder.append(String.format("%s[%s]", mappings.get(i).getFeatureName(), mappings.get(i).getAnnotationType()));
            } else {
                stringBuilder.append(String.format(",%s[%s]", mappings.get(i).getFeatureName(), mappings.get(i).getAnnotationType()));
            }
        }

        return stringBuilder.toString();
    }

    public void createUnLabeledData() throws IOException {
        if (projectData.getExperiementFileRecords().size() < 2) {//only create unlabled if there exists at least one previous commit.
            return;
        }
        projectData.setUnlabledMLDataSet(null);
        projectData.setUnlabledMLDataSet(new HashMap<>());

        AbstractStringMetric metric = new CosineSimilarity();
        List<String> mappedAssetNames = projectData.getAssetFeatureMap().stream().map(FeatureAssetMap::getMappedAsset).map(Asset::getFullyQualifiedName).distinct().collect(Collectors.toList());


        List<Asset> changedAssets = projectData.getUnlabeledAssetList().stream().filter(a -> a.getAssetType() == AssetType.FRAGMENT || mappedAssetNames.contains(a.getFullyQualifiedName())).collect(Collectors.toList());

        for (Asset asset : changedAssets) {
            List<Asset> assetsChangedInCurentCommit = changedAssets.stream().filter(a -> a.getAssetType() == asset.getAssetType()).collect(Collectors.toList());
            createAssetMetric(asset, metric, assetsChangedInCurentCommit, true, 0);
        }

        metricsExecutorService.waitForTaskToFinish();
        metricsExecutorService.shutdown();

    }


    public void createMLData() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        AbstractStringMetric metric = new CosineSimilarity();
        projectData.setUnlabledMLDataSet(null);
        projectData.setUnlabledMLDataSet(new ConcurrentHashMap<>());
//        projectData.setMlDataSet(null);
//        projectData.setMlDataSet(new HashMap<>());
        //List<Asset> changedAssets = projectData.getAssetFeatureMap().parallelStream().map(FeatureAssetMap::getMappedAsset).filter(a->a.getAssetType()!=AssetType.FOLDER).distinct().collect(Collectors.toList());
        AssetType assetType = Utilities.getAssetType(projectData.getConfiguration().getCodeAbstractionLevel());

        List<Asset> changedAssets = projectData.getAssetsChangedInCommit(currentCommit).parallelStream().filter(a -> a.getAssetType() == assetType).collect(Collectors.toList());
        long changedCount = projectData.getAssetChangedList().size();
        //List<Asset> changedFiles = changedAssets.stream().filter(a->a.getAssetType()==AssetType.FILE).collect(Collectors.toList());
        if (currentCommit.startsWith("094afe7c1")) {
            System.out.println("here");
        }

//        for(Asset fileAsset:changedFiles){
//            List<Asset> fileAssets = changedAssets.stream().filter(a->a.getFullyQualifiedName().contains(fileAsset.getFullyQualifiedName())).collect(Collectors.toList());
//            List<BlamedLine> blamedLines = scm.blame(fileAsset.getFileRelativePath(),currentCommit,false);
//            List<Integer> blamedLineNumbers = blamedLines.stream().map(BlamedLine::getLineNumber).collect(Collectors.toList());
        int maxFutures = projectData.getConfiguration().getNumberOfThreads();
        int futureCount = 0;
        int size = changedAssets.size();
        for (int i = 0; i < size; i++) {
//                Stopwatch stopwatch = Stopwatch.createStarted();
            Asset asset = changedAssets.get(i);

            List<Asset> assetsChangedInCurentCommit = changedAssets.stream().filter(a -> a.getAssetType() == asset.getAssetType()).collect(Collectors.toList());
//                AssetMetrics assetMetrics = new AssetMetrics(asset, metric, projectData,currentCommit,assetsChangedInCurentCommit,false,i);
//                assetMetrics.createMetricsForAsset();
//                stopwatch.stop(); // optional
//                System.out.printf("%d/%d: %s, Time elapsed: %s, ChangedAssetCount=%d\n",(i+1),size, asset.getFullyQualifiedName(), stopwatch.elapsed(TimeUnit.SECONDS),changedCount);

            if (futureCount < maxFutures) {
                createAssetMetric(asset, metric, assetsChangedInCurentCommit, false, i);
                futureCount++;
            }
            if (futureCount == maxFutures) {
                metricsExecutorService.waitForTaskToFinish();
                futureCount=0;
            }
        }
        // }
        metricsExecutorService.waitForTaskToFinish();
        metricsExecutorService.shutdown();
        stopwatch.stop(); // optionale
        System.out.printf("Time elapsed: %s, ChangedAssetCount=%d\n", stopwatch.elapsed(TimeUnit.SECONDS), changedCount);
        //System.out.printf("Mapped Assets: %d\n",changedAssets.size());
        //System.out.printf("ML Data Instances: %d\n",projectData.getMlDataSet()!=null?projectData.getMlDataSet().size():0);
        //System.out.printf("UNlabeled Instances: %d\n",projectData.getUnlabledMLDataSet()!=null?projectData.getUnlabledMLDataSet().size():0);

        //updateAttributesForExistingDataInstances();

    }


    private void createAssetMetric(Asset asset, AbstractStringMetric metric, List<Asset> assetsChangedInCommit, boolean isUnlabaled, int i) {
        AssetMetrics assetMetrics = new AssetMetrics(asset, metric, projectData, currentCommit, assetsChangedInCommit, isUnlabaled, i);
        createFuture(assetMetrics);
    }

    private void updateAttributesForExistingDataInstances() {
        Map<String,Map<String,Double>> temp = new ConcurrentHashMap<>(projectData.getMlDataSet());
        for (String asset : temp.keySet()) {

            //remove unused features
            removeUnusedFeatures(temp.get(asset));
        }
        for(String asset: temp.keySet()){
            //get the missing (new) features
            Set<String> newFeatures = Sets.difference(attributeValues.keySet(), temp.get(asset).keySet());
            //now add the new features to this instance
            for (String key : newFeatures) {
                projectData.getMlDataSet().get(asset).put(key, 0.0);
            }
        }
        projectData.setMlDataSet(temp);
    }

    private void updateAttributesForUnlabaledInstances() {
        for (String asset : projectData.getUnlabledMLDataSet().keySet()) {
            Set<String> newFeatures = Sets.difference(attributeValues.keySet(), projectData.getUnlabledMLDataSet().get(asset).keySet());
            //now add the new features to this instance
            for (String key : newFeatures) {
                projectData.getUnlabledMLDataSet().get(asset).put(key, 0.0);
            }


        }
    }

    private void removeUnusedFeatures(Map<String, Double> instance) {
        //first remove non existing features
        for (Iterator<Map.Entry<String, Double>> it = instance.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Double> e = it.next();
            if (!attributeValues.keySet().contains(e.getKey())) {
                it.remove();
            }
        }

    }


    private Map<String, Double> getAttributeValues() {


        Map<String, Double> attributeValues = new LinkedHashMap<>();
        attributeValues.put("CSDEV", -1.0);
        attributeValues.put("COMM", -1.0);
        attributeValues.put("DDEV", -1.0);
        attributeValues.put("DCONT", -1.0);
        attributeValues.put("HDCONT", -1.0);
        attributeValues.put("CCC", -1.0);
        attributeValues.put("ACCC", -1.0);
        attributeValues.put("NLOC", -1.0);
        attributeValues.put("DNFMA", -1.0);
        attributeValues.put("NFMA", -1.0);
        attributeValues.put("NFF", -1.0);
        //attributeValues.put("ANFF", -1.0);
        List<String> mappedFeatures = projectData.getAssetFeatureMap().parallelStream().map(FeatureAssetMap::getFeatureName).distinct().collect(Collectors.toList());
        for (String feature : mappedFeatures) {
            attributeValues.put(feature, 0.0);
        }

        return attributeValues;
    }


    private void createFuture(Runnable sldMetric) {
        metricsExecutorService.addFuture((Future<?>) metricsExecutorService.submit(sldMetric));
    }

    public String getPredictionFlatXMLModel() {
        List<String> metrics = new ArrayList<>(Arrays.asList(new String[]{"CSDEV", "COMM", "DDEV", "DCONT", "HDCONT", "CCC", "ACCC", "NLOC", "DNFMA", "NFMA", "NFF"}));
        List<String> mappedFeatureNames = new ArrayList<>(attributeValues.keySet().stream().filter(m -> !metrics.contains(m)).collect(Collectors.toList()));
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        stringBuilder.append(System.lineSeparator());
        stringBuilder.append("<labels xmlns=\"http://mulan.sourceforge.net/labels\">");
        stringBuilder.append(System.lineSeparator());

        for (String featureName : mappedFeatureNames) {
            if (featureName.equalsIgnoreCase(projectData.getRootFeature().getText())) {
                continue;
            }
            stringBuilder.append(String.format("<label name=\"%s\"> </label>", featureName));
            stringBuilder.append(System.lineSeparator());

        }
        stringBuilder.append("</labels>");

        return stringBuilder.toString();

    }

    public String getFlatXMLModel() {
        List<String> mappedFeatureNames = projectData.getAssetFeatureMap().parallelStream().map(FeatureAssetMap::getFeatureName).distinct().collect(Collectors.toList());
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        stringBuilder.append(System.lineSeparator());
        stringBuilder.append("<labels xmlns=\"http://mulan.sourceforge.net/labels\">");
        stringBuilder.append(System.lineSeparator());

        for (String featureName : mappedFeatureNames) {
            if (featureName.equalsIgnoreCase(projectData.getRootFeature().getText())) {
                continue;
            }
            stringBuilder.append(String.format("<label name=\"%s\"> </label>", featureName));
            stringBuilder.append(System.lineSeparator());

        }
        stringBuilder.append("</labels>");

        return stringBuilder.toString();

    }

    public String getXMLModel(FeatureTreeNode rootFeature) {
        List<String> mappedFeatureNames = projectData.getAssetFeatureMap().parallelStream().map(FeatureAssetMap::getFeatureName).distinct().collect(Collectors.toList());
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        stringBuilder.append(System.lineSeparator());
        stringBuilder.append("<labels xmlns=\"http://mulan.sourceforge.net/labels\">");
        stringBuilder.append(System.lineSeparator());
        List<FeatureTreeNode> rootFeatures = rootFeature.getChildren();//exlude the root feature e.g., Clafer Moo Visuaizer and only get its childrn
        for (FeatureTreeNode feature : rootFeatures) {
            if (mappedFeatureNames.contains(projectData.getConfiguration().isUseFullFeatureNamesInMLDataFile() ? feature.getFullyQualifiedName() : feature.getText())) {
                doXML(mappedFeatureNames, feature, stringBuilder, "");
            }

        }
        stringBuilder.append("</labels>");

        return stringBuilder.toString();

    }

    private void doXML(List<String> mappedFeatureNames, FeatureTreeNode treeNode, StringBuilder sb, String indent) {
        sb.append(String.format("%s <label name=\"%s\">", indent, treeNode.getText()));
        sb.append(System.lineSeparator());
        for (FeatureTreeNode child : treeNode.getChildren()) {
            if (mappedFeatureNames.contains(projectData.getConfiguration().isUseFullFeatureNamesInMLDataFile() ? child.getFullyQualifiedName() : child.getText())) {
                doXML(mappedFeatureNames, child, sb, indent + "    ");
            }
        }
        sb.append(String.format("%s </label>", indent));
        sb.append(System.lineSeparator());
    }


}
