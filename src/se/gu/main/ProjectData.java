package se.gu.main;

import com.google.common.collect.ConcurrentHashMultiset;
import org.apache.commons.lang3.StringUtils;
import se.gu.assets.*;
import se.gu.data.DataController;
import se.gu.git.CodeChange;
import se.gu.git.scenarios.*;
import se.gu.metrics.ged.CallNode;
import se.gu.metrics.ged.FunctionCall;
import se.gu.metrics.structure.AssetPairwiseComparison;
import se.gu.ml.experiment.ExperiementFileRecord;
import se.gu.ml.preprocessing.*;
import se.gu.parser.fmparser.FeatureTreeNode;
import se.gu.utils.Utilities;

import java.io.File;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProjectData implements Serializable {


    private static final long serialVersionUID = -1748870581385554685L;
    private List<ExperiementFileRecord> experiementFileRecords;

    public List<ExperiementFileRecord> getExperiementFileRecords() {
        return experiementFileRecords;
    }

    public void setExperiementFileRecords(List<ExperiementFileRecord> experiementFileRecords) {
        this.experiementFileRecords = experiementFileRecords;
    }

    private List<FeatureTreeNode> featureTree;
    private List<FeatureTreeNode> featureList;

    public void setFeatureNames(List<String> featureNames) {
        this.featureNames = featureNames;
    }

    private List<String> featureNames;
    private List<Asset> assetList;

    public List<Asset> getUnlabeledAssetList() {
        return unlabeledAssetList;
    }

    public void setUnlabeledAssetList(List<Asset> unlabeledAssetList) {
        this.unlabeledAssetList = unlabeledAssetList;
    }

    private List<Asset> unlabeledAssetList;
    private Asset projectRoot;
    private List<FeatureAssetMap> assetFeatureMap;
    private List<Asset> changedAssetsInCurrentCommit;
    private Map<String, Integer> assetChangeCount;
    private List<DataInstance> unlabledData;
    private List<AssetPairwiseComparison> unlabledAssetPairwiseComparisons;

    public Map<String, Map<String, Double>> getMlDataSet() {
        return mlDataSet;
    }

    public void setMlDataSet(Map<String, Map<String, Double>> mlDataSet) {
        this.mlDataSet = mlDataSet;
    }

    private Map<String,Map<String,Double>> mlDataSet;

    public Map<String, Map<String, Double>> getUnlabledMLDataSet() {
        return unlabledMLDataSet;
    }

    public void setUnlabledMLDataSet(Map<String, Map<String, Double>> unlabledMLDataSet) {
        this.unlabledMLDataSet = unlabledMLDataSet;
    }

    private Map<String,Map<String,Double>> unlabledMLDataSet;

    public List<AssetChanged> getAssetChangedList() {
        return assetChangedList;
    }

    public void setAssetChangedList(List<AssetChanged> assetChangedList) {
        this.assetChangedList = assetChangedList;
    }

    private List<AssetChanged> assetChangedList;



    public void setUnlabledAssetPairwiseComparisons(List<AssetPairwiseComparison> unlabledAssetPairwiseComparisons) {
        this.unlabledAssetPairwiseComparisons = unlabledAssetPairwiseComparisons;
    }

    public List<DataInstance> getUnlabledData() {
        return unlabledData;
    }

    public void setUnlabledData(List<DataInstance> unlabledData) {
        this.unlabledData = unlabledData;
    }

    public List<CallNode> getCallNodes() {
        return callNodes;
    }

    public void setCallNodes(List<CallNode> callNodes) {
        this.callNodes = callNodes;
    }

    private List<CallNode> callNodes;

    public List<FunctionCall> getCurrentFunctionCalls() {
        return currentFunctionCalls;
    }

    public void setCurrentFunctionCalls(List<FunctionCall> currentFunctionCalls) {
        this.currentFunctionCalls = currentFunctionCalls;
    }

    private List<FunctionCall> currentFunctionCalls;

    public Map<String, Integer> getAssetChangeCount() {
        return assetChangeCount;
    }

    public void setAssetChangeCount(Map<String, Integer> assetChangeCount) {
        this.assetChangeCount = assetChangeCount;
    }

    public List<Asset> getChangedAssetsInCurrentCommit() {
        return changedAssetsInCurrentCommit;
    }

    public void setChangedAssetsInCurrentCommit(List<Asset> changedAssetsInCurrentCommit) {
        this.changedAssetsInCurrentCommit = changedAssetsInCurrentCommit;
    }

    private List<DataInstance> mlData;

    public List<DataInstance> getMlData() {
        return mlData;
    }

    public void setMlData(List<DataInstance> mlData) {
        this.mlData = mlData;
    }

    static long globalAssetId;

    public FeatureTreeNode getRootFeature() {
        return featureList.stream().filter(f -> f.getParent() == null).findFirst().get();
    }

    public void setRootFeature(FeatureTreeNode rootFeature) {
        this.rootFeature = rootFeature;
    }

    private FeatureTreeNode rootFeature;

    public Configuration getConfiguration() {
        return configuration;
    }

    public List<AssetChanged> getAssetModificationList(Asset asset){
        if (asset.getAssetType() == AssetType.FRAGMENT) {
            List<Integer> fragmentLines = asset.getFragmentLines();
            return assetChangedList.stream().filter(a -> a.getFileName().equalsIgnoreCase(asset.getParent().getFullyQualifiedName())&& Utilities.intersectionExists(fragmentLines,a.getLinesChanged())).collect(Collectors.toList());

        } else if (asset.getAssetType() == AssetType.LOC) {
            return assetChangedList.stream().filter(a -> a.getFileName().equalsIgnoreCase(asset.getParent().getParent().getFullyQualifiedName()) && a.getLinesChanged().contains(asset.getLineNumber())).collect(Collectors.toList());
        } else {
            return assetChangedList.stream().filter(f->f.getFileName().equalsIgnoreCase(asset.getFullyQualifiedName())).collect(Collectors.toList());
        }
    }
    public List<Asset> getAssetsChangedInCommit(String commitHash){
        AssetType assetType = Utilities.getAssetType(configuration.getCodeAbstractionLevel());

        List<AssetChanged> fileLines = assetChangedList.parallelStream().filter(a->a.getCommitHash().equalsIgnoreCase(commitHash)).collect(Collectors.toList());
        List<Asset> changedAssets = new ArrayList<>();

        //files
        for(AssetChanged record:fileLines){
            Optional<Asset> fileAsset = assetList.parallelStream().filter(f->f.getAssetType()== AssetType.FILE && f.getFullyQualifiedName().equalsIgnoreCase(record.getFileName())).findAny();
            if(fileAsset.isPresent()){
                fileAsset.get().setFileRelativePath(record.getFileRelativePath());
                changedAssets.add(fileAsset.get());
                //now add child assets
                List<Integer> fileLinesChanged = record.getLinesChanged();
                List<Asset> childAssets = fileAsset.get().flattened().filter(a->!a.getFullyQualifiedName().equalsIgnoreCase(fileAsset.get().getFullyQualifiedName())).collect(Collectors.toList());
                childAssets.stream().forEach(c->c.setFileRelativePath(fileAsset.get().getFileRelativePath()));
                List<Asset> changedFragments = childAssets.stream().filter(f->f.getAssetType()== AssetType.FRAGMENT&& Utilities.intersectionExists(fileLinesChanged,f.getFragmentLines())).collect(Collectors.toList());
                changedAssets.addAll(changedFragments);
                for(Asset fragment:changedFragments){//for each affected fragment, add all it's lines and relearn them
                    if(assetType==AssetType.LOC) {
                        changedAssets.addAll(fragment.getChildren());
                    }
                }
                if(assetType==AssetType.LOC) {
                    List<Asset> changedLines = childAssets.stream().filter(f -> f.getAssetType() == AssetType.LOC && fileLinesChanged.contains(f.getLineNumber())).collect(Collectors.toList());
                    changedAssets.addAll(changedLines);
                }

            }
            //check in unlabed assets also

        }

        return changedAssets.parallelStream().distinct().collect(Collectors.toList());

    }
    public List<Asset> getAssetsChangedInCommitAll(String commitHash){

        List<AssetChanged> fileLines = assetChangedList.parallelStream().filter(a->a.getCommitHash().equalsIgnoreCase(commitHash)).collect(Collectors.toList());
        List<Asset> changedAssets = new ArrayList<>();

        //files
        for(AssetChanged record:fileLines){
            Optional<Asset> fileAsset = assetList.parallelStream().filter(f->f.getAssetType()== AssetType.FILE && f.getFullyQualifiedName().equalsIgnoreCase(record.getFileName())).findAny();
            if(fileAsset.isPresent()){
                fileAsset.get().setFileRelativePath(record.getFileRelativePath());
                changedAssets.add(fileAsset.get());
                //now add child assets
                List<Integer> fileLinesChanged = record.getLinesChanged();
                List<Asset> childAssets = fileAsset.get().flattened().filter(a->!a.getFullyQualifiedName().equalsIgnoreCase(fileAsset.get().getFullyQualifiedName())).collect(Collectors.toList());
                childAssets.stream().forEach(c->c.setFileRelativePath(fileAsset.get().getFileRelativePath()));
                List<Asset> changedFragments = childAssets.stream().filter(f->f.getAssetType()== AssetType.FRAGMENT&& Utilities.intersectionExists(fileLinesChanged,f.getFragmentLines())).collect(Collectors.toList());
                changedAssets.addAll(changedFragments);
                for(Asset fragment:changedFragments){//for each affected fragment, add all it's lines and relearn them

                        changedAssets.addAll(fragment.getChildren());

                }

                    List<Asset> changedLines = childAssets.stream().filter(f -> f.getAssetType() == AssetType.LOC && fileLinesChanged.contains(f.getLineNumber())).collect(Collectors.toList());
                    changedAssets.addAll(changedLines);


            }
            //check in unlabed assets also

        }

        return changedAssets.parallelStream().distinct().collect(Collectors.toList());

    }
private DataController dataController;
    public void setConfiguration(Configuration configuration) throws SQLException, ClassNotFoundException {
        this.configuration = configuration;
        //assets
        assetList = new CopyOnWriteArrayList<>();
        //root repository should be parent of the clones e.g., if all clafer clones are in a folder called Clafer, then make this as root
        File cloneParentFolder = configuration.getCopiedGitRepositories().get(0).getParentFile();
        Asset rootAsset = new Asset(cloneParentFolder.getName(), cloneParentFolder.getAbsolutePath(), AssetType.REPOSITORY, null);
        setProjectRoot(rootAsset);
        assetList.add(rootAsset);
        //features
        featureList = new CopyOnWriteArrayList<>();
        featureNames = new CopyOnWriteArrayList<>();
        rootFeature = new FeatureTreeNode(rootAsset.getAssetName(), 0);
        featureList.add(rootFeature);
        //mappings
        assetFeatureMap = new CopyOnWriteArrayList<>();
        mlData = new CopyOnWriteArrayList<>();
        mlDataSet = new ConcurrentHashMap<>();

        assetChangeCount = new ConcurrentHashMap<>();
        currentFunctionCalls = new CopyOnWriteArrayList<>();
        callNodes = new ArrayList<>();
        experiementFileRecords = new ArrayList<>();


        developerScenarios = new ArrayList<>();//&line [DeveloperScenarios]
        assetChangedList = new CopyOnWriteArrayList<>();//&line [Metrics]
        if(configuration.isSaveDataInDataBase()) {
            dataController = new DataController(configuration);
        }
    }

    private Configuration configuration;

    public List<FeatureTreeNode> getFeatureTree() {
        return featureTree;
    }

    public void setFeatureTree(List<FeatureTreeNode> featureTree) {
        this.featureTree = featureTree;
    }

    public List<FeatureTreeNode> getFeatureList() {
        return featureList;
    }

    public void setFeatureList(List<FeatureTreeNode> featureList) {
        this.featureList = featureList;
    }

    public List<Asset> getAssetList() {
        return assetList;
    }

    public void setAssetList(List<Asset> assetList) {
        this.assetList = assetList;
    }

    public Asset getProjectRoot() {
        return assetList.stream().filter(a -> a.getParent() == null).findFirst().get();
    }

    public void setProjectRoot(Asset projectRoot) {
        this.projectRoot = projectRoot;
    }

    public List<FeatureAssetMap> getAssetFeatureMap() {
        return assetFeatureMap;
    }

    public void setAssetFeatureMap(List<FeatureAssetMap> assetFeatureMap) {
        this.assetFeatureMap = assetFeatureMap;
    }


    public ProjectData(Configuration configuration) throws SQLException, ClassNotFoundException {
        setConfiguration(configuration);

    }

    public DataController getDataController() {
        return dataController;
    }

    public void setDataController(DataController dataController) {
        this.dataController = dataController;
    }

    public Asset addFileAssetToDB(File file) throws SQLException {
        File rootFolder = new File(projectRoot.getFullyQualifiedName());
        File originalFile = file;
        List<File> fileAncestry = new ArrayList<>();
        while (file != null && !file.getAbsolutePath().equalsIgnoreCase(rootFolder.getAbsolutePath())) {
            fileAncestry.add(file);
            file = file.getParentFile();
        }
        //reverse this
        Collections.reverse(fileAncestry);
        Asset asset = null;
        for(File target:fileAncestry){
            Asset ta = new Asset(target.getName(),target.getAbsolutePath(), (target.isDirectory() ? AssetType.FOLDER : AssetType.FILE));
            ta = dataController.addAsset(ta,target.getParent(),configuration.getProjectRepository().getName());
            if(target.getAbsolutePath().equalsIgnoreCase(originalFile.getAbsolutePath())){
                asset=ta;
            }
        }
        return asset;

    }

    public Asset addFileAsset(File file) {
        //fanas/4/ClaferMooVisualizer/Server/ClaferMoo/input.js
        File originalFile = file;
        File rootFolder = new File(projectRoot.getFullyQualifiedName()); //configuration.getProjectRepository();
        List<File> fileAncestry = new ArrayList<>();

        while (file != null && !file.getAbsolutePath().equalsIgnoreCase(rootFolder.getAbsolutePath())) {
            fileAncestry.add(file);
            file = file.getParentFile();
        }
        fileAncestry.add(rootFolder);
        //reverse this
        Collections.reverse(fileAncestry);
        Asset previousParent = getFileAssetFromAssetList(rootFolder);
        for (File targetFile : fileAncestry) {
            if (targetFile.getAbsolutePath().equalsIgnoreCase(rootFolder.getAbsolutePath())) {
                continue;
            }
            Asset asset = getFileAssetFromAssetList(targetFile);
            if (asset == null) {
                asset = new Asset(targetFile.getName(),
                        targetFile.getAbsolutePath(),
                        (targetFile.isDirectory() ? AssetType.FOLDER : AssetType.FILE));
                asset.setAssetId(++globalAssetId);
                asset.setParent(previousParent);
                if(previousParent!=null){previousParent.getChildren().add(asset);}
                assetList.add(asset);
            }
            previousParent = asset;

        }

        return getFileAssetFromAssetList(originalFile);
    }


    public Asset getFileAssetFromAssetList(File fileName) {
        Optional<Asset> asset = assetList.stream().filter(a -> a.getFullyQualifiedName().equalsIgnoreCase(fileName.getAbsolutePath())).findFirst();
        return asset.isPresent() ? asset.get() : null;
    }

    /**
     * @param featureName partially qualified name of feature: if feature names are unique, then name is enough, otherwise a partially qualified name is supplied
     *                    e.g., processManagement::polling
     * @return
     * @example Given an existing feature in the feature model as ClaferMooVisualizer::processManagement::polling, and given parameter feature name as processManagement::polling
     * The search would evaluate to true since the former retains all the elements of the latter, hence the method would return the former as the matching feature.
     */
    public FeatureTreeNode getFeatureFromFeatureList(String featureName) {
        String seperator = configuration.getFeatureQualifiedNameSeparator();

        Optional<FeatureTreeNode> featureTreeNode = featureList.stream().filter(f -> Utilities.featureNamesMatch(f.getFullyQualifiedName(), featureName, seperator)).findFirst();
        return featureTreeNode.isPresent() ? featureTreeNode.get() : null;
    }

    public FeatureTreeNode addFeatureToList(String featureName) {
        featureName = featureName.trim();
        if (StringUtils.isBlank(featureName)) {
            return null;
        }
        FeatureTreeNode feature = getFeatureFromFeatureList(featureName);
        if (feature != null) {
            return feature;
        } else {
            String[] featureNames = featureName.split(configuration.getFeatureQualifiedNameSeparator());
            //if there's only one feature then add this feature to root feature since we cannot determine the parent
            if (featureNames.length == 1) {
                feature = new FeatureTreeNode(featureName, 4, rootFeature, FeatureType.SINGLE);
                rootFeature.getChildren().add(feature);
                featureList.add(feature);
            } else {
                FeatureTreeNode parentFeature = getFeatureFromFeatureList(featureNames[0]);
                //FeatureTreeNode prev= null;
                if (parentFeature == null) {
                    parentFeature = new FeatureTreeNode(featureNames[0], 4, rootFeature, FeatureType.SINGLE);
                    featureList.add(parentFeature);
                }
                for (int i = 1; i < featureNames.length; i++) {
                    feature = getFeatureFromFeatureList(featureNames[i]);
                    if (feature == null) {
                        feature = new FeatureTreeNode(featureNames[i], 4 * i, parentFeature, FeatureType.SINGLE);
                        featureList.add(feature);
                        parentFeature.getChildren().add(feature);
                    }

                    parentFeature = feature;


                }
            }

        }
        return feature;
    }


    public String getAssetFeatureMappingsAsStringOutput() {
        StringBuilder stringBuilder = new StringBuilder();
        for (FeatureAssetMap featureAssetMap : assetFeatureMap) {
            stringBuilder.append(featureAssetMap.toString());
            stringBuilder.append(System.lineSeparator());
        }
        return stringBuilder.toString();
    }

    public List<String> getFeatureNames() {
        return featureNames;
//        //only get features mapped to assets
//        List<String> featureNames = new ArrayList<>();
//        assetFeatureMap.stream()
//                .map(FeatureAssetMap::getMappedFeature).forEach(f -> {
//            featureNames.addAll(f.getAncestry().stream()
//                    .map(configuration.isUseFullFeatureNamesInMLDataFile() ? FeatureTreeNode::getFullyQualifiedName : FeatureTreeNode::getText).distinct().collect(Collectors.toList()));
//        });
//        return featureNames.stream().distinct().collect(Collectors.toList());

        // return featureList.stream().map(configuration.isUseFullFeatureNamesInMLDataFile() ? FeatureTreeNode::getFullyQualifiedName : FeatureTreeNode::getText).collect(Collectors.toList());
    }

    public List<String> getPreviousFeatureNames() {
        return previousFeatureNames;
    }

    public void setPreviousFeatureNames(List<String> previousFeatureNames) {
        this.previousFeatureNames = previousFeatureNames;
    }

    //previous featureNames
    private List<String> previousFeatureNames;

    public List<Asset> getAssetsMappedToFeature(String featureFullName) {
        AbstractionLevel abstractionLevel = configuration.getCodeAbstractionLevel();
        AssetType assetType = Utilities.getAssetType(abstractionLevel);
        List<Asset> featureAssets = new ArrayList<>();
        List<FeatureAssetMap> assetMap = assetFeatureMap.stream().filter(p -> p.getMappedFeature().getFullyQualifiedName().equalsIgnoreCase(featureFullName)).collect(Collectors.toList());
        List<Asset> mappedAssets = assetMap.stream().map(FeatureAssetMap::getMappedAsset).collect(Collectors.toList());
        for (Asset asset : mappedAssets) {
            Stream<Asset> assetStream = asset.flattened();
            featureAssets.addAll(assetStream.collect(Collectors.toList()));
        }

        return featureAssets;

    }


    public List<Asset> getUnlabledAssetsForKnownLabels() {
        List<Asset> targetAnalysisAssets = getTargetAssetsForAnalysis(true);
        List<Asset> mappedAssets = assetFeatureMap.stream().map(FeatureAssetMap::getMappedAsset).distinct().collect(Collectors.toList());
        List<Asset> allMappedAssets = new ArrayList<>();
        for (Asset asset : mappedAssets) {
            Stream<Asset> assetStream = asset.flattened();
            allMappedAssets.addAll(assetStream.collect(Collectors.toList()));
        }
        List<Asset> targets = targetAnalysisAssets.stream().distinct().filter(allMappedAssets::contains).collect(Collectors.toList());//get only mappd assets that have changed
        return targets;

    }

    /**
     * Gets assets that are mapped to features in the training set. We do this so that we only make predictions for features we know and not newly introduced on
     * ones on which our classifiers have no training
     * @return
     */
    public List<Asset> getAssetMappedToKnownFeatures(){
        List<String> previousFeatures = getPreviousFeatureNames();
        return assetFeatureMap.stream()
                .filter(am->previousFeatures.contains(configuration.isUseFullFeatureNamesInMLDataFile()?am.getMappedFeature().getFullyQualifiedName():am.getMappedFeature().getText()))
                .map(FeatureAssetMap::getMappedAsset).distinct().collect(Collectors.toList());
    }

    public List<Asset> getUnlabeledAssets() {
        List<Asset> targetAssets = new ArrayList<>();
        List<Asset> assetsWithKnownLabels = getAssetMappedToKnownFeatures();
        //&begin [DatasetGenerator::FragmentClusteringMethod]
        if (configuration.getCodeAbstractionLevel() == AbstractionLevel.FRAGMENT) {
            //get all files that changed
            List<Asset> allChangedFiles = getChangedAssetsByType(AssetType.FILE);
            //&begin [DatasetGenerator::FragmentClusteringMethod::Diff]
            if(configuration.getFragmentClusteringMethod() == FragmentClusteringMethod.DIFF){
                for (Asset changedFile : allChangedFiles) {
                    targetAssets.addAll(getClustersFromDiffCodeChanges(changedFile,assetsWithKnownLabels));
                }
            }
            //&end [DatasetGenerator::FragmentClusteringMethod::Diff]
            //&begin [DatasetGenerator::FragmentClusteringMethod::Threshold]
            else {
                //create fragments from all changed lines
                List<Asset> allChangedLines = getChangedAssetsByType(AssetType.LOC);

                List<Asset> allClusters = new ArrayList<>();
                for (Asset changedFile : allChangedFiles) {
                    List<Asset> clusterLines = changedFile.getLinesOfCode().stream().filter(allChangedLines::contains).collect(Collectors.toList());
                    targetAssets.addAll(getClustersFromLines(clusterLines, changedFile,assetsWithKnownLabels));
                }
            }
            //&end [DatasetGenerator::FragmentClusteringMethod::Threshold]

        }
        //&end [DatasetGenerator::FragmentClusteringMethod]
        else {
            targetAssets = getTargetAssetsForAnalysis(true);
        }
        return targetAssets;
    }


    //&begin [DatasetGenerator::FragmentClusteringMethod::Diff]
    /**
     * Gets clusters based on consecutive code changes in a diff
     * @param changedFile
     * @return
     */
    private List<Asset> getClustersFromDiffCodeChanges(Asset changedFile,List<Asset> assetsWithKnownLabels){
        int startingLine, endingLine;
        if(changedFile.getCodeChanges()!=null) {
            for (CodeChange codeChange : changedFile.getCodeChanges()) {
                startingLine = codeChange.getAddedLinesStart();
                endingLine = startingLine + codeChange.getAddedLines();
                addClusterAsset(changedFile, startingLine, endingLine,assetsWithKnownLabels);
            }
        }
        return changedFile.getChildren().stream().filter(a -> a.getAssetType() == AssetType.CLUSTER).distinct().collect(Collectors.toList());
    }
    //&end [DatasetGenerator::FragmentClusteringMethod::Diff]
    //&begin [DatasetGenerator::FragmentClusteringMethod::Threshold]
    private List<Asset> getClustersFromLines(List<Asset> changedLinesInFile, Asset changedFile,List<Asset> assetsWithKnownLabels) {
        int clusterThreshold = configuration.getFragmentClusterThreshold();
        int listSize = changedLinesInFile.size();
        int startingLine, endingLine;
        if (listSize / clusterThreshold < 2) {//lines don;t make up two clusters
            startingLine = changedLinesInFile.get(0).getLineNumber();
            endingLine = changedLinesInFile.get(listSize - 1).getLineNumber();

            addClusterAsset(changedFile, startingLine, endingLine,assetsWithKnownLabels);
        } else {
            int clusters = listSize / clusterThreshold;
            for (int i = 1; i <= clusters; i++) {
                int startingIndex = (i - 1) * clusterThreshold;
                if (i == clusters) {
                    startingLine = changedLinesInFile.get(startingIndex).getLineNumber();
                    endingLine = changedLinesInFile.get(listSize - 1).getLineNumber();
                } else {

                    startingLine = changedLinesInFile.get(startingIndex).getLineNumber();
                    endingLine = changedLinesInFile.get(startingIndex + (clusterThreshold - 1)).getLineNumber();
                }
                addClusterAsset(changedFile, startingLine, endingLine,assetsWithKnownLabels);
            }


        }

        return changedFile.getChildren().stream().filter(a -> a.getAssetType() == AssetType.CLUSTER).distinct().collect(Collectors.toList());
    }
    //&end [DatasetGenerator::FragmentClusteringMethod::Threshold]

    //&begin [DatasetGenerator::FragmentClusteringMethod]
    private void addClusterAsset(Asset changedFile, int startingLine, int endingLine,List<Asset> assetsWithKnownLabels) {
        Asset clusterAsset = new Asset(startingLine, endingLine, AnnotationType.NONE, String.format("CLUSTER %d-%d", startingLine, endingLine), String.format("%s%sCLUSTER %d-%d", changedFile.getFullyQualifiedName(), configuration.getFeatureQualifiedNameSeparator(), startingLine, endingLine), AssetType.CLUSTER, changedFile);
        clusterAsset.setChildren(null);
        clusterAsset.setChildren(new ArrayList<>());
        clusterAsset.getChildren().addAll(changedFile.getLinesOfCode().stream()
                .filter(l -> (configuration.isUseOnlyAssetsWithKnownLabelsForExperiment()?assetsWithKnownLabels.contains(l):true) && l.getLineNumber() >= startingLine && l.getLineNumber() <= endingLine).collect(Collectors.toList()));
        if(clusterAsset.getChildren().size()>0) {//only add clusters that have children
            changedFile.getChildren().add(clusterAsset);
            assetList.add(clusterAsset);
        }
    }
    //&end [DatasetGenerator::FragmentClusteringMethod]
    public List<Asset> getChangedAssetsByType(AssetType loc) {
        return changedAssetsInCurrentCommit.stream().filter(l -> l.getAssetType() == loc).collect(Collectors.toList());
    }

    public List<Asset> getAssetSiblings(Asset asset) {
        List<Asset> assetSiblings = new ArrayList<>();


        return assetSiblings;
    }

    public List<Asset> getAllNeighbours(Asset asset) {
        return asset.getParent().getChildren();
    }

    public void addAssetChangeCount(Asset asset) {

        if (assetChangeCount.containsKey(asset.getFullyQualifiedName())) {
            assetChangeCount.put(asset.getFullyQualifiedName(), assetChangeCount.get(asset.getFullyQualifiedName()) + 1);
        } else {
            assetChangeCount.put(asset.getFullyQualifiedName(), 1);
        }
    }

    public void addAssetChangeCount(List<Asset> assets) {
        for (Asset asset : assets) {
            addAssetChangeCount(asset);
        }

    }

    //&begin [GED]
    public List<FunctionCall> getFunctionCallsForAsset(Asset asset) {
        List<FunctionCall> assetFunctionCalls = new ArrayList<>();
        if (asset.getAssetType() == AssetType.FOLDER) {
            List<String> folderFileNames = asset.getChildren().stream().map(Asset::getFullyQualifiedName).collect(Collectors.toList());
            assetFunctionCalls = currentFunctionCalls.parallelStream().filter(fc -> (folderFileNames.contains(fc.getSourceNode().getFileName()) || folderFileNames.contains(fc.getTargetNode().getFileName())))
                    .collect(Collectors.toList());
        } else if (asset.getAssetType() == AssetType.FILE) {
            assetFunctionCalls = currentFunctionCalls.parallelStream().filter(fc -> (asset.getFullyQualifiedName().equalsIgnoreCase(fc.getSourceNode().getFileName()) || asset.getFullyQualifiedName().equalsIgnoreCase(fc.getTargetNode().getFileName())))
                    .collect(Collectors.toList());
        } else if (asset.getAssetType() == AssetType.FRAGMENT) {
            List<Integer> lineNumbers = asset.getParent().getLinesOfCode().parallelStream().filter(l -> l.getLineNumber() >= asset.getStartLine() && l.getLineNumber() <= asset.getEndLine())
                    .map(Asset::getLineNumber).collect(Collectors.toList());
            assetFunctionCalls = currentFunctionCalls.parallelStream()
                    .filter(fc -> (asset.getParent().getFullyQualifiedName().equalsIgnoreCase(fc.getSourceNode().getFileName()) || asset.getParent().getFullyQualifiedName().equalsIgnoreCase(fc.getTargetNode().getFileName())))
                    .filter(fc -> Utilities.intersectionExists(fc.getSourceNode().getLinesInRange(), lineNumbers) || Utilities.intersectionExists(fc.getTargetNode().getLinesInRange(), lineNumbers))
                    .collect(Collectors.toList());
        } else if (asset.getAssetType() == AssetType.LOC) {
            assetFunctionCalls = currentFunctionCalls.parallelStream()
                    .filter(fc -> (asset.getParent().getFullyQualifiedName().equalsIgnoreCase(fc.getSourceNode().getFileName()) || asset.getParent().getFullyQualifiedName().equalsIgnoreCase(fc.getTargetNode().getFileName())))
                    .filter(fc -> fc.getSourceNode().getLinesInRange().contains(asset.getLineNumber()) || fc.getTargetNode().getLinesInRange().contains(asset.getLineNumber()))
                    .collect(Collectors.toList());
        }

        return assetFunctionCalls;
    }
    //&end [GED]

    //&begin [ML::Attributes]
    public Map<String, Double> getAttributeValues() {
        Map<String, Double> attributeValues = new LinkedHashMap<>();
        getMLAttributes().parallelStream().forEach(a -> {
            attributeValues.put(a.getName(), a.isMLFeature() ? -1.0 : 0);
        });//add all existing attributes. set -1 fpor all feature attributes signifiying missing value and for label attrbutes, set to 0 to show non-labeled
        return attributeValues;
    }
    public List<AttributeName> getMLAttributes() {
        List<AttributeName> attributes = new ArrayList<>();

        attributes.addAll(Arrays.asList(
                new AttributeName[]{
                        new AttributeName("CSDEV", true),
                        new AttributeName("COMM", true),
                        new AttributeName("DDEV", true),
                        new AttributeName("DCONT", true),
                        new AttributeName("HDCONT", true),
                        new AttributeName("CCC", true),
                        new AttributeName("ACCC", true),
                        new AttributeName("NLOC", true),
                        new AttributeName("DNFMA", true),
                        new AttributeName("NFMA", true),
                        new AttributeName("NFF", true)
                        //new AttributeName("ACSFMA", true)
                }));
        FeatureTreeNode rootFeature = getRootFeature();
        String rootFeatureName = configuration.isUseFullFeatureNamesInMLDataFile() ? rootFeature.getFullyQualifiedName() : rootFeature.getText();
        getFeatureNames().stream().filter(f -> !f.equalsIgnoreCase(rootFeatureName)).distinct().forEach(f -> attributes.add(new AttributeName(f, false)));

        return attributes;
    }

    public List<String> getMLFeatures(){
        return getMLAttributes().parallelStream().filter(a->a.isMLFeature()).map(AttributeName::getName).collect(Collectors.toList());
    }
    public List<String> getMLFeaturesNames(){
        String[] featureNames= new String[]{"CSDEV","COMM","DDEV","DCONT","HDCONT","CCC","ACCC","NLOC","DNFMA","NFMA","NFF"};
        return Arrays.asList(featureNames);
    }
    //&end [ML::Attributes]
    public List<Asset> getTargetAssetsForAnalysis(boolean isForTestDataset) {
        AbstractionLevel abstractionLevel = configuration.getCodeAbstractionLevel();
        AssetType assetType = Utilities.getAssetType(abstractionLevel);
        List<Asset> assetsWithKNownLabels = getAssetMappedToKnownFeatures();
        //get changed assets
        //List<Asset> changedAssets = getChangedAssetsInCurrentCommit();
        List<Asset> targetAssets = getChangedAssetsByType(assetType);
        if (abstractionLevel == AbstractionLevel.LOC) {
            targetAssets = targetAssets.stream().filter(a -> a.getAssetType() == assetType)
                    .filter(l -> !StringUtils.isBlank(CodeCleaner.getCleanedContent(l.getAssetContent()))).collect(Collectors.toList());
        }
        return isForTestDataset? targetAssets.stream()
                .filter(a->configuration.isUseOnlyAssetsWithKnownLabelsForExperiment()?assetsWithKNownLabels.contains(a):true)
                .distinct().collect(Collectors.toList())
                :
                targetAssets.stream()
                        .distinct().collect(Collectors.toList());
    }

    //&begin [Statistics]
    public long getAssetCount(AssetType assetType) {
        List<Asset> assets = assetList.stream().filter(a -> a.getAssetType() == assetType).collect(Collectors.toList());
        if (assetType == AssetType.LOC) {
            assets = assets.stream().filter(l -> !StringUtils.isBlank(CodeCleaner.getCleanedContent(l.getAssetContent()))).collect(Collectors.toList());
        }
        return assets.stream().distinct().count();
    }

    public long getAnnotationCount(AssetType assetType, AnnotationType annotationType) {
        return assetFeatureMap.stream().filter(m -> assetType == AssetType.LOC ? (m.getAnnotationType() == annotationType || m.getAnnotationType() == AnnotationType.FRAGMENT) : m.getAnnotationType() == annotationType && m.getMappedAsset().getAssetType() == assetType).distinct().count();
    }

    //&end [Statistics]

    /**
     * To be orecise, only get features mapped to the nearest anestor. E.g., if asset is mapped then return it's specific features, else go to nearest ancestor and get his featureses Retrieves all features mapped to an asset
     * Changed to only get features directly mapped to each asset
     *
     * @param asset
     * @return
     */
    public List<FeatureTreeNode> getAssetFeatures(Asset asset) {
        List<FeatureTreeNode> mappedFeatures = null;
        if (configuration.isUseHierachicalLabeling()) {
            for (Asset ancestor : asset.getAncestry()) {
                mappedFeatures = getAssetMappedFeatures(ancestor);
                if (mappedFeatures != null && mappedFeatures.size() > 0) {
                    break;
                }
            }
        } else {
            mappedFeatures = assetFeatureMap.parallelStream().filter(m -> m.getMappedAsset().getFullyQualifiedName().equalsIgnoreCase(asset.getFullyQualifiedName()))
                    .map(FeatureAssetMap::getMappedFeature)
                    .distinct()
                    .collect(Collectors.toList());
        }
        return mappedFeatures;

    }

    private List<FeatureTreeNode> getMappedFeatures(Asset asset) {
        List<FeatureTreeNode> mappedFeatures = new ArrayList<>();
        asset.getAncestry().stream().forEach(a -> {
            mappedFeatures.addAll(
                    assetFeatureMap.stream()
                            .filter(mp -> mp.getMappedAsset().getFullyQualifiedName().equalsIgnoreCase(a.getFullyQualifiedName()))
                            .map(FeatureAssetMap::getMappedFeature)
                            .collect(Collectors.toList()));
        });
        return mappedFeatures;
    }

    /**
     * Returns mappings closely associated with the target asset. This prevents biasing parent folder mappings.
     *
     * @param asset
     * @return
     */
    public List<FeatureAssetMap> getAncestralMappingForAsset(Asset asset) {
        List<FeatureAssetMap> mappings = null;
        if (configuration.isUseHierachicalLabeling()) {
            for (Asset ancestor : asset.getAncestry()) {
                mappings = assetFeatureMap.stream()
                        .filter(mp -> mp.getMappedAsset().getFullyQualifiedName().equalsIgnoreCase(ancestor.getFullyQualifiedName()))
                        .collect(Collectors.toList());

                if (mappings != null && mappings.size() > 0) {
                    break;
                }
            }
        } else {
            mappings = assetFeatureMap.stream()
                    .filter(mp -> mp.getMappedAsset().getFullyQualifiedName().equalsIgnoreCase(asset.getFullyQualifiedName()))
                    .collect(Collectors.toList());
        }

        return mappings;
    }


    public Asset getAssetByQualifiedName(String assetFullyQualifiedName) {
        Optional<Asset> asset = getAssetList().stream().filter(a -> a.getFullyQualifiedName().equalsIgnoreCase(assetFullyQualifiedName)).findFirst();
        if (asset.isPresent()) {
            return asset.get();
        } else {
            return null;
        }
    }

    public List<Asset> getAssetsByType(AssetType assetType) {
        return getAssetList().stream().filter(a -> a.getAssetType() == assetType).collect(Collectors.toList());
    }

    public List<Asset> getAssetsForPredictionComparison(AssetType assetType) {
        List<Asset> assetsWithKnownLabels = getAssetMappedToKnownFeatures();
        return changedAssetsInCurrentCommit.stream()
                .filter(a -> (configuration.isUseOnlyAssetsWithKnownLabelsForExperiment()?assetsWithKnownLabels.contains(a):true) && (assetType == AssetType.FRAGMENT ? (a.getAssetType() == assetType || a.getAssetType() == AssetType.LOC) : a.getAssetType() == assetType)).collect(Collectors.toList());
    }

    public List<String> getFeaturesOfFileSubAssetsByFileName(Asset asset){

        return getAssetFeatureMap().parallelStream()
                .filter(a->a.getMappedAsset().getFullyQualifiedName().contains(asset.getFullyQualifiedName()))
                .map(FeatureAssetMap::getFeatureName)
                .distinct()
                .collect(Collectors.toList());

    }
    public List<FeatureTreeNode> getFeaturesOfChildren(Asset asset){
        List<Asset> allAssets = asset.flattened().collect(Collectors.toList());
        List<FeatureTreeNode> allFeatures = new ArrayList<>();
        for(Asset ass:allAssets){
            allFeatures.addAll(getAssetMappedFeatures(ass));
        }
        return allFeatures.stream().distinct().collect(Collectors.toList());
    }

    public List<FeatureAssetMap> getAssetMappings(Asset asset) {
        return assetFeatureMap.stream().filter(m -> m.getMappedAsset().getFullyQualifiedName().equalsIgnoreCase(asset.getFullyQualifiedName())).distinct().collect(Collectors.toList());
    }

    public List<FeatureTreeNode> getAssetMappedFeatures(Asset asset) {
        return assetFeatureMap.stream().filter(m -> m.getMappedAsset().getFullyQualifiedName().equalsIgnoreCase(asset.getFullyQualifiedName()))
                .map(FeatureAssetMap::getMappedFeature).distinct().collect(Collectors.toList());
    }

    private List<AssetPairwiseComparison> assetPairwiseComparisons;

    public List<AssetPairwiseComparison> getAssetPairwiseComparisons() {
        return assetPairwiseComparisons;
    }

    public void setAssetPairwiseComparisons(List<AssetPairwiseComparison> assetPairwiseComparisons) {
        this.assetPairwiseComparisons = assetPairwiseComparisons;
    }

    public List<FeatureTreeNode> getFeatureAncestry(FeatureTreeNode feature) {
        List<FeatureTreeNode> ancestors = new ArrayList<>();
        ancestors.add(feature);
        if (feature.getParent() != null && !feature.getParent().getFullyQualifiedName().equalsIgnoreCase(rootFeature.getFullyQualifiedName())) {
            return doGetAncestry(feature.getParent(), ancestors);
        } else {
            return ancestors;
        }

    }

    private List<FeatureTreeNode> doGetAncestry(FeatureTreeNode child, List<FeatureTreeNode> ancestors) {
        ancestors.add(child);
        if (child.getParent() != null && !child.getParent().getFullyQualifiedName().equalsIgnoreCase(rootFeature.getFullyQualifiedName())) {
            return doGetAncestry(child.getParent(), ancestors);
        } else {
            return ancestors;
        }
    }

    //&begin [DeveloperScenarios]
    //create scenarios
    private List<DeveloperScenarioMap> developerScenarios;

    public List<DeveloperScenarioMap> getDeveloperScenarios() {
        return developerScenarios;
    }

    private List<Asset> doGetSiblings(Asset asset, Asset ancestor, AssetType assetType) {
        //check that you don't return the same asset as a sibling of itself

        if (assetType == AssetType.FRAGMENT) {
            return ancestor.flattened().filter(a -> ((a.getAssetType() == AssetType.FRAGMENT || a.getAssetType() == AssetType.CLUSTER) &&
                    !a.getFullyQualifiedName().equalsIgnoreCase(asset.getFullyQualifiedName())
//                    &&
//                    (a.getParent().getFullyQualifiedName().equalsIgnoreCase(asset.getParent().getFullyQualifiedName()) ? !(a.getStartLine() >= asset.getStartLine() && a.getStartLine() <= asset.getEndLine()) : true) &&
//                    (a.getParent().getFullyQualifiedName().equalsIgnoreCase(asset.getParent().getFullyQualifiedName()) ? !(a.getEndLine() >= asset.getStartLine() && a.getEndLine() <= asset.getEndLine()) : true)
            )).collect(Collectors.toList());

        } else {
            return ancestor.flattened().filter(a -> a.getAssetType() == assetType && !a.getFullyQualifiedName().equalsIgnoreCase(asset.getFullyQualifiedName())).collect(Collectors.toList());

        }

    }

    public List<Asset> getSiblings(Asset asset, AssetType assetType) {
        List<Asset> siblings = null;
        for (Asset ancestor : asset.getAncestry()) {
            siblings = doGetSiblings(asset, ancestor, assetType);
            if (siblings.size() > 0) {
                break;
            }
        }
        return siblings;
    }

    public List<Asset> getAnnotatedSiblings(List<Asset> siblings) {
        return getAssetFeatureMap().stream().map(FeatureAssetMap::getMappedAsset).filter(siblings::contains).collect(Collectors.toList());
    }

    //&end [DeveloperScenarios]



private int lastRunCommitIndex;

    public int getLastRunCommitIndex() {
        return lastRunCommitIndex;
    }

    public void setLastRunCommitIndex(int lastRunCommitIndex) {
        this.lastRunCommitIndex = lastRunCommitIndex;
    }

    //&begin [Metrics]
    private List<NestingDepthPair> nestingDepthPairs;

    public ConcurrentHashMultiset<NestingDepthPair> getConcurrentNestingDepthPairs() {
        return concurrentNestingDepthPairs;
    }

    public void setConcurrentNestingDepthPairs(ConcurrentHashMultiset<NestingDepthPair> concurrentNestingDepthPairs) {
        this.concurrentNestingDepthPairs = concurrentNestingDepthPairs;
    }

    private ConcurrentHashMultiset<NestingDepthPair> concurrentNestingDepthPairs;

    public List<NestingDepthPair> getNestingDepthPairs() {
        return nestingDepthPairs;
    }

    public void setNestingDepthPairs(List<NestingDepthPair> nestingDepthPairs) {
        this.nestingDepthPairs = nestingDepthPairs;
    }
//&end [Metrics]
}
