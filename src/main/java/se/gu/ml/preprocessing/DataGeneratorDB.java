package se.gu.ml.preprocessing;

import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.io.FileUtils;
import se.gu.assets.AssetMappingDB;
import se.gu.data.DataController;
import se.gu.git.Commit;
import se.gu.main.ProjectData;
import se.gu.metrics.AssetMetricsDB;
import se.gu.utils.Utilities;
import weka.core.*;
import weka.core.converters.ConverterUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

public class DataGeneratorDB {
    private ProjectData projectData;

    public DataGeneratorDB(ProjectData projectData) {
        this.projectData = projectData;
    }

    public void createDataSets() throws Exception {
        File analysisDirectory = projectData.getConfiguration().getAnalysisDirectory();
        String projectName = projectData.getConfiguration().getProjectRepository().getName();
        boolean isMappedOnly = !projectData.getConfiguration().isTrainingDataIncludesUnMappedAssets();
        String[] assetTypes = projectData.getConfiguration().getAssetTypes();
        //==========create output folders==========
        String projectFolder = String.format("%s/featracer/mappedOnlyAssets/%s", analysisDirectory, projectName);
        if(!isMappedOnly){
            projectFolder = String.format("%s/featracer/allAssets/%s", analysisDirectory, projectName);
        }

        Utilities.createOutputDirectory(String.format("%s/featracer", analysisDirectory), false);
        Utilities.createOutputDirectory(projectFolder, true);
        for (String assetType : assetTypes) {
            Utilities.createOutputDirectory(String.format("%s/%s", projectFolder, assetType), true);
        }
        //=====get needed data===========
        DataController dataController = new DataController(projectData.getConfiguration());
        //get all commits with metrics
        List<Commit> commits = dataController.getAllCommitsWithMetrics(projectName);
        //list of distinct mappings
        List<AssetMappingDB> mappings = dataController.getAllAssetMappingsForProject(projectName);
        //get all project metrics
        List<AssetMetricsDB> metrics = dataController.getAllAssetMetricsForProject(projectName);
        //unique metrics
        Map<String, AssetMetricsDB> metricsDBMap = new HashMap<>();
        //=======create datasets=============
        int commitsSize = commits.size();
        int startingIndex = projectData.getConfiguration().getStartingCommitIndex();
        try (ProgressBar pb = new ProgressBar("Commits:", commitsSize)) {
            for (int i = startingIndex; i < commitsSize; i++) {
                Commit trainingCommit = commits.get(i);
                Commit testCommit = null;
                if (i < commitsSize - 1) {
                    testCommit = commits.get(i + 1);
                }
                pb.step();
                pb.setExtraMessage(trainingCommit.getCommitHash());
//delete existing record for this commit
                dataController.deleteDatasetsForCommit(trainingCommit.getCommitHash(), projectName);
                //used for the training dataset
                //List<AssetMetricsDB> commitMetrics = metrics.parallelStream().filter(m -> m.getCommitIndex() <= trainingCommit.getCommitIndex()).collect(Collectors.toList());

                List<AssetMappingDB> commitMappings = mappings.parallelStream().filter(m -> m.getCommitIndex() <= trainingCommit.getCommitIndex()).collect(Collectors.toList());
                //List<AssetMetricsDB> uniqueCommitMetrics = getUniqueList(commitMetrics);
                //create unique metrics
                List<AssetMetricsDB> currentCommitMetrics = metrics.parallelStream().filter(m -> m.getCommitIndex() == trainingCommit.getCommitIndex()).collect(Collectors.toList());
                for(AssetMetricsDB metric:currentCommitMetrics){
                    metricsDBMap.put(metric.getAsset(),metric);
                }
                List<AssetMetricsDB> uniqueCommitMetrics = metricsDBMap.values().parallelStream().collect(Collectors.toList());
                //used for the test dataset
                List<AssetMetricsDB> testCommitMetrics = null;
                List<AssetMappingDB> testCommitMappings = null;
                if (testCommit != null) {
                    int testCommitIndex = testCommit.getCommitIndex();
                    testCommitMetrics = metrics.parallelStream().filter(m -> m.getCommitIndex() <= testCommitIndex).collect(Collectors.toList());
                    testCommitMappings = mappings.parallelStream().filter(m -> m.getCommitIndex() <= testCommitIndex).collect(Collectors.toList());
                }

                for (String assetType : assetTypes) {
                    //geenerate datasets per asset type
                    String trainingFileName = String.format("%s/%s/train_%d_%s.arff", projectFolder, assetType, trainingCommit.getCommitIndex(), trainingCommit.getCommitHash());
                    String trainingXMLFileName = String.format("%s/%s/train_%d_%s.xml", projectFolder, assetType, trainingCommit.getCommitIndex(), trainingCommit.getCommitHash());
                    List<AssetMetricsDB> typeCommitMetrics = uniqueCommitMetrics.parallelStream().filter(m -> m.getAssetType().equalsIgnoreCase(assetType)).collect(Collectors.toList());
                    if (typeCommitMetrics.size() <= 0) {
                        continue;
                    }
                    if (isMappedOnly) {//if we only want assets with features in the training set
                        typeCommitMetrics = typeCommitMetrics.parallelStream().filter(m -> m.isMapped()).collect(Collectors.toList());
                    }
                    if (typeCommitMetrics.size() <= 0) {
                        continue;
                    }
                    List<AssetMappingDB> typeCommitMappings = commitMappings.parallelStream().filter(m -> m.getAssetType().equalsIgnoreCase(assetType)).collect(Collectors.toList());
                    List<String> typeFeatures = typeCommitMappings.parallelStream().map(AssetMappingDB::getFeaturename).distinct().collect(Collectors.toList());
                    if(typeFeatures.size()<=0){
                        continue;
                    }
                    createARFF(typeFeatures, trainingCommit.getCommitIndex(), trainingCommit.getCommitHash(), typeCommitMetrics, typeCommitMappings, trainingFileName, trainingXMLFileName, false, null);
                    //do prediction dataset
                    String testFileName = null;
                    String testXMLFileName = null;
                    String testCSVFileName = null;
                    if (testCommit != null) {

                        //get metrics for assets that changed in test commit only
                        String testCommitHash = testCommit.getCommitHash();
                        List<AssetMetricsDB> testTypeCommitMetrics = testCommitMetrics.parallelStream()
                                .filter(m -> m.getAssetType().equalsIgnoreCase(assetType) && m.getCommitHash().equalsIgnoreCase(testCommitHash)).collect(Collectors.toList());
                        if (testTypeCommitMetrics.size() > 0) {//write files only there are assets of this type in the testcommit
                            testFileName = String.format("%s/%s/test_%d_%s.arff", projectFolder, assetType, testCommit.getCommitIndex(), testCommit.getCommitHash());
                            testXMLFileName = String.format("%s/%s/test_%d_%s.xml", projectFolder, assetType, testCommit.getCommitIndex(), testCommit.getCommitHash());
                            //csv file used to store mappings of each asset in test dataset
                            testCSVFileName = String.format("%s/%s/test_%d_%s.csv", projectFolder, assetType, testCommit.getCommitIndex(), testCommit.getCommitHash());
                            //currenty this gets all features mapped to assets changed upto the test commit. The best would be to get features mapped only to the assets in the test commit
                            List<AssetMappingDB> testTypeCommitMappings = testCommitMappings.parallelStream().filter(m -> m.getAssetType().equalsIgnoreCase(assetType)).collect(Collectors.toList());
                            List<String> testTypeFeatures = testTypeCommitMappings.parallelStream().map(AssetMappingDB::getFeaturename).distinct().collect(Collectors.toList());
                            if(testTypeFeatures.size()<=0){
                                testTypeFeatures=typeFeatures;
                            }
                            createARFF(testTypeFeatures, testCommit.getCommitIndex(), testCommit.getCommitHash(), testTypeCommitMetrics, testTypeCommitMappings, testFileName, testXMLFileName, true, testCSVFileName);

                        }
                    }
                    dataController.datasetInsert(trainingCommit.getCommitIndex(), trainingCommit.getCommitHash(), projectName, assetType, trainingFileName, testFileName, isMappedOnly, trainingXMLFileName, testXMLFileName, testCSVFileName, testCommit==null?0: testCommit.getCommitIndex(), testCommit==null?null:testCommit.getCommitHash());

                }


            }
        }

    }

    private List<AssetMetricsDB> getUniqueList(List<AssetMetricsDB> commitMetrics) {
        Map<String, AssetMetricsDB> metricsDBMap = new HashMap<>();
        for (AssetMetricsDB metric : commitMetrics) {
            if (metricsDBMap.containsKey(metric.getAsset())) {
                //if asset already added in unique list, then update its value
                AssetMetricsDB existingMetric = metricsDBMap.get(metric.getAsset());
                if (metric.getCommitIndex() > existingMetric.getCommitIndex()) {
                    metricsDBMap.put(metric.getAsset(), metric);
                }
            } else {
                metricsDBMap.put(metric.getAsset(), metric);
            }
        }
        return metricsDBMap.values().parallelStream().collect(Collectors.toList());
    }

    private void createARFF(List<String> typeCommitFeatures, int commitIndex, String commitHash, List<AssetMetricsDB> typeCommitMetrics, List<AssetMappingDB> mappings, String arrfFileName, String xmlFile, boolean isTestARFF, String testCSVFile) throws Exception {
        Instances data = initializeInstances(typeCommitFeatures, commitIndex, commitHash);
        //only for test dataset
        File csvFile = null;
        if (isTestARFF) {
            csvFile = new File(testCSVFile);
        }
        if (isTestARFF && csvFile.exists()) {

            FileUtils.forceDelete(csvFile);
        }
        PrintWriter writer = null;
        if (isTestARFF) {
            writer = new PrintWriter(new FileWriter(testCSVFile, true));
        }
        //3. create the data
        for (AssetMetricsDB metric : typeCommitMetrics) {
            List<String> assetFeatures = mappings.parallelStream().filter(m -> m.getAssetfullname().equalsIgnoreCase(metric.getAsset()) && m.getCommitIndex() <= metric.getCommitIndex()).map(AssetMappingDB::getFeaturename).collect(Collectors.toList());
            data.add(getWekaInstance(metric, typeCommitFeatures, assetFeatures));
            if (isTestARFF) {
                try {

                    writer.printf("%s;%s\n", metric.getAsset(), assetFeatures.parallelStream().collect(Collectors.joining(",")));
                } catch (Exception ex) {

                }
            }
        }
        if (isTestARFF && writer != null) {
            writer.close();
        }
        //4 write file
        ConverterUtils.DataSink.write(arrfFileName, data);
        //5 write XML feature file
        Utilities.writeStringFile(xmlFile, getFlatXMLModel(typeCommitFeatures));
    }


    private Instance getWekaInstance(AssetMetricsDB metric, List<String> commitFeatures, List<String> assetFeatures) {
        ArrayList<Double> vals = new ArrayList<>();
        //order is return  {"CSDEV","COMM","DDEV","DCONT","HDCONT","CCC","ACCC","NLOC","DNFMA","NFMA","NFF"});
        vals.add(metric.getCsvDev());
        vals.add(metric.getComm());
        vals.add(metric.getDdev());
        vals.add(metric.getDcont());
        vals.add(metric.getHdcont());
        vals.add(metric.getCcc());
        vals.add(metric.getAccc());
        vals.add(metric.getNloc());
        vals.add(metric.getDnfma());
        vals.add(metric.getNfma());
        vals.add(metric.getNff());
        //now add classes
        for (String commitFeature : commitFeatures) {
            vals.add(assetFeatures.contains(commitFeature) ? 1.0 : 0.0);
        }
        double[] myvals = vals.stream().mapToDouble(Double::doubleValue).toArray();
        return projectData.getConfiguration().isUseSparseInstance() ? new SparseInstance(1.0, myvals) : new DenseInstance(1.0, myvals);


    }

    private Instances initializeInstances(List<String> commitFeatures, int commitIndex, String commitHash) {
        ArrayList<Attribute> atts;
        //1. setup the attributes
        atts = new ArrayList<>();
        //ml attributes (features)
        //atts.addAll(getAttributes().parallelStream().map(a->new Attribute(a)).collect(Collectors.toList()));
        for (String attribute : getAttributes()) {
            atts.add(new Attribute(attribute));//ensure order is maintaied.
        }
        // - classes (software features)
        for (String commitFeature : commitFeatures) {
            atts.add(new Attribute(commitFeature, Arrays.asList(new String[]{"0", "1"})));
        }

        long numbeOfFeatures = commitFeatures.size();

        // 2. create Instances object to hold the data
        //-C -d means that the labels are the last d attributes

        Instances data = new Instances(String.format("FeaTraceR-Commit_%d-%s-(%d features): -C -%d", commitIndex, commitHash, numbeOfFeatures, numbeOfFeatures), atts, 0);
        return data;
    }

    public List<String> getAttributes() {
        return Arrays.asList(new String[]{"CSDEV", "COMM", "DDEV", "DCONT", "HDCONT", "CCC", "ACCC", "NLOC", "DNFMA", "NFMA", "NFF"});
    }

    public String getFlatXMLModel(List<String> mappedFeatureNames) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        stringBuilder.append(System.lineSeparator());
        stringBuilder.append("<labels xmlns=\"http://mulan.sourceforge.net/labels\">");
        stringBuilder.append(System.lineSeparator());

        for (String featureName : mappedFeatureNames) {
            stringBuilder.append(String.format("<label name=\"%s\"> </label>", featureName));
            stringBuilder.append(System.lineSeparator());

        }
        stringBuilder.append("</labels>");

        return stringBuilder.toString();

    }
}
