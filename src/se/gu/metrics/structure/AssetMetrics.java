package se.gu.metrics.structure;

import com.google.common.base.Stopwatch;
import org.apache.commons.lang3.StringUtils;
import org.repodriller.scm.BlamedLine;
import se.gu.assets.Asset;
import se.gu.assets.AssetChanged;
import se.gu.assets.AssetType;
import se.gu.assets.FeatureAssetMap;
import se.gu.main.ProjectData;
import se.gu.ml.preprocessing.DataInstance;
import se.gu.ml.preprocessing.MetricValue;
import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AssetMetrics implements Runnable, Callable<MetricValue>, Serializable {

    private static final long serialVersionUID = 6885387724352014893L;
    private Asset asset;
    private AbstractStringMetric metric;
    private ProjectData projectData;
    private String currentCommit;
    private boolean isUnlabaled;
    private String lastCommitForAsset;



    private int assetCount;

    private List<Asset> assetsChangedInCommit;//must be prefiltered to match type of current asset

    public AssetMetrics(Asset asset, AbstractStringMetric metric, ProjectData projectData, String currentCommit, List<Asset> assetsChangedInCommit, boolean isUnlabaled,int assetCount) {
        this.asset = asset;
        this.metric = metric;
        this.projectData = projectData;
        this.currentCommit = currentCommit;
        this.assetsChangedInCommit = assetsChangedInCommit;
        this.isUnlabaled = isUnlabaled;
        this.assetCount=assetCount;
    }

    public void createMetricsForAsset() {
        int changedCount = assetsChangedInCommit.size();
        Stopwatch stopwatch = Stopwatch.createStarted();
        List<String> featuresMappedToAsset = null;

            featuresMappedToAsset = projectData.getAssetFeatureMap().stream().filter(m -> m.getMappedAsset().getFullyQualifiedName().equalsIgnoreCase(asset.getFullyQualifiedName())).map(FeatureAssetMap::getFeatureName).distinct().collect(Collectors.toList());
            if (featuresMappedToAsset.size() <= 0) {
                return;
            }

        List<AssetChanged> assetModifications = projectData.getAssetModificationList(asset);
        List<String> allDevs = getAllDevs(assetModifications);
        List<String> commits = getAllCommits(assetModifications);
        Map<String, Double> contributions = getDeveloperContributions(allDevs,assetModifications);
        Map<String, List<Asset>> commitAssets = new HashMap<>();
        setCommitAssets(commits, commitAssets);
        //lastCommitForAsset = commitAssets.keySet().toArray()[commitAssets.keySet().size()-1].toString();
        List<Asset> allAssetsModifiedWithAsset = getAllAssetsChangedWithThisAsset(commitAssets);
        List<String> allModifiedAssetNames = allAssetsModifiedWithAsset.stream().map(Asset::getFullyQualifiedName).collect(Collectors.toList());
        List<String> assetNamesModifiedInCurrentCommit = commitAssets.get(currentCommit).stream().map(Asset::getFullyQualifiedName).collect(Collectors.toList());
        List<String> allFeaturesModifiedWithAsset = getFeatresChangedWithAsset(allModifiedAssetNames);
        List<String> featuresModifiedInCurrentCommit = getFeatresChangedWithAsset(assetNamesModifiedInCurrentCommit);
        if(isUnlabaled){
            //System.out.println("STOP HERE");
        }


        double csvDev = csDev(asset.getFileRelativePath(), allDevs, metric);
        double ddev = allDevs.size();
        double comm = commits.size();
        double dcont = contributions.values().parallelStream().mapToDouble(Double::doubleValue).average().getAsDouble();
        double hdcont = contributions.values().parallelStream().mapToDouble(Double::doubleValue).max().getAsDouble();
        double ccc = getCCC(commitAssets.get(currentCommit));
        double accc = getACCC(commitAssets);
        double nloc = asset.getNloc();
        double dnfma = allFeaturesModifiedWithAsset.size();
        double nfma = featuresModifiedInCurrentCommit.size();
        double nff = getFeaturesWithinFile();// getFeatureNameSimilarities(assetNamesModifiedInCurrentCommit);
        //double acsfma = -1.0;//getFeatureNameSimilarities(allModifiedAssetNames);
        saveDataInstance(asset.getFullyQualifiedName(),csvDev,ddev,comm,dcont,hdcont,ccc,accc,nloc,dnfma,nfma,nff,featuresMappedToAsset);
        stopwatch.stop(); // optional
        System.out.printf("%d/%d: %s, Time elapsed: %s, ChangedAssetCount=%d\n",(assetCount+1),changedCount, asset.getFullyQualifiedName(), stopwatch.elapsed(TimeUnit.SECONDS),projectData.getAssetChangedList().size());

    }

    private double getFeaturesWithinFile(){
        Asset file = null;

        if(asset.getAssetType() == AssetType.FRAGMENT){
            file = asset.getParent();
        }else if(asset.getAssetType() == AssetType.LOC){
            file = asset.getParent().getParent();
        }else if(asset.getAssetType() == AssetType.FILE){
            file = asset;
        }
        Asset parent = file;
        double featuresInFile  = projectData.getAssetFeatureMap().parallelStream().filter(a->a.getMappedAsset().getFullyQualifiedName().contains(parent.getFullyQualifiedName()))
                .map(FeatureAssetMap::getFeatureName).distinct().count();
        return featuresInFile;
    }

    private void saveDataInstance(String assetName, double csdev, double ddev, double comm, double dcont, double hdcont, double ccc, double accc, double nloc, double dnfma, double nfma, double nff, List<String> assetFeatures){
        //set metric values first
        Map<String,Double> attributeValues = new HashMap<>();//  projectData.getAttributeValues();
        attributeValues.put("CSDEV",csdev);
        attributeValues.put("COMM",comm);
        attributeValues.put("DDEV",ddev);
        attributeValues.put("DCONT",dcont);
        attributeValues.put("HDCONT",hdcont);
        attributeValues.put("CCC",ccc);
        attributeValues.put("ACCC",accc);
        attributeValues.put("NLOC",nloc);
        attributeValues.put("DNFMA",dnfma);
        attributeValues.put("NFMA",nfma);
        attributeValues.put("NFF",nff);
        //attributeValues.put("ACSFMA",acsfma);
        //set features
        //first add this to test dataset for previous training dataset
        //if(lastCommitForAsset!=null&&lastCommitForAsset.equalsIgnoreCase(currentCommit)) {
            projectData.getUnlabledMLDataSet().put(assetName, attributeValues);
        //}
        //now add it as part of trianing data for this commit
            assetFeatures.parallelStream().forEach(f -> attributeValues.put(f, 1.0));
            projectData.getMlDataSet().put(assetName, attributeValues);





    }

    private String cleanFeature(String feature){
        return Arrays.stream(feature.split(projectData.getConfiguration().getCamelCaseSplitRegex())).collect(Collectors.joining(" "));
    }
    public double getFeatureNameSimilarities(List<String> assetNames) {
        List<String> featureNames = new ArrayList<>();
        List<Double> similarities = new ArrayList<>();
        for (String assetName : assetNames) {
            featureNames.add(projectData.getAssetFeatureMap().parallelStream().filter(m -> m.getMappedAsset().getFullyQualifiedName().equalsIgnoreCase(assetName)).map(FeatureAssetMap::getFeatureName).collect(Collectors.joining(" ")));
        }
        for(int i=0;i<featureNames.size();i++){
            if(StringUtils.isBlank(featureNames.get(i).trim())){
                continue;
            }
            for(int j=i+1;j<featureNames.size();j++){
                double value = metric.getSimilarity(cleanFeature(featureNames.get(i)),cleanFeature(featureNames.get(j)));
                similarities.add(value);
            }
        }
        double result = similarities.parallelStream().mapToDouble(Double::doubleValue).average().orElse(0);
        result = Double.isNaN(result)||Double.isInfinite(result)?-1:result;
        return result;

    }

    private List<String> getAllCommits(List<AssetChanged> assetModifications) {
        return assetModifications.parallelStream().map(AssetChanged::getCommitHash).distinct().collect(Collectors.toList());
    }


    private List<String> getAllDevs(List<AssetChanged> assetModifications) {
        return assetModifications.parallelStream().map(AssetChanged::getAuthor).distinct().collect(Collectors.toList());
    }



    private void setCommitAssets(List<String> commits, Map<String, List<Asset>> commitAssets) {
//        List<Asset> assetsInNowCommit = new ArrayList<>();
//        assetsInNowCommit.addAll(projectData.getAssetsChangedInCommit(currentCommit).parallelStream().filter(a->a.getAssetType()==asset.getAssetType()).collect(Collectors.toList()));
//        assetsInNowCommit.addAll(assetsChangedInCommit);
        commitAssets.put(currentCommit, assetsChangedInCommit);
        for (String commit : commits) {
            if (commit.equalsIgnoreCase(currentCommit)) {
                continue;
            }
            commitAssets.put(commit, projectData.getAssetsChangedInCommit(commit).parallelStream().filter(a -> a.getAssetType() == asset.getAssetType()).collect(Collectors.toList()));
        }
    }


    private List<String> getFeatresChangedWithAsset(List<String> allAssetsModifiedWithAsset) {
        return projectData.getAssetFeatureMap().parallelStream().filter(m -> allAssetsModifiedWithAsset.contains(m.getMappedAsset().getFullyQualifiedName())).map(FeatureAssetMap::getFeatureName).distinct().collect(Collectors.toList());
    }

    private List<Asset> getAllAssetsChangedWithThisAsset(Map<String, List<Asset>> commitAssets) {
        List<Asset> allAssetsChangedInCOmmits = new ArrayList<>();

        for (String commit : commitAssets.keySet()) {
            allAssetsChangedInCOmmits.addAll(commitAssets.get(commit));
        }
        return allAssetsChangedInCOmmits.parallelStream().distinct().collect(Collectors.toList());
    }

    /**
     * Get Average number of assets modified together with this asset
     * @param commitAssets
     * @return
     */
    private double getACCC(Map<String, List<Asset>> commitAssets) {
        List<Double> changeCounts = new ArrayList<>();
        for (String commit : commitAssets.keySet()) {
            if (commit.equalsIgnoreCase(currentCommit)) {
                changeCounts.add(getCCC(assetsChangedInCommit));
            } else {
                changeCounts.add(getCCC(projectData.getAssetsChangedInCommit(commit)));
            }

        }
        return changeCounts.parallelStream().mapToDouble(Double::doubleValue).average().getAsDouble();

    }

    /**
     * Collective change count—the number of assets modified together with this asset in the current commit  ccc(a,c).
     *
     * @return
     */
    private double getCCC(List<Asset> changedAssets) {
        return changedAssets.parallelStream().filter(a -> a.getAssetType() == asset.getAssetType()).count();
    }

    /**
     * CSDEV
     * get cosine similarity between all developers controbuting to asset
     */

    private double csDev(String fileName, List<String> allDevs, AbstractStringMetric stringMetric) {
        double csDevValue = 0;
        if (true) {
            //get all deves who contributed to the file

            //compare developer names
            if (allDevs.size() == 1) {
                csDevValue = 1;
            } else {

                List<Double> values = new ArrayList<>();
                for (int i = 0; i < allDevs.size(); i++) {
                    for (int j = i + 1; j < allDevs.size(); j++) {
                        double value = stringMetric.getSimilarity(allDevs.get(i), allDevs.get(j));
                        values.add(value);

                    }

                }
                csDevValue = values.parallelStream().filter(d -> d < 0.0).mapToDouble(Double::doubleValue).average().orElse(0);
            }
        }
        return csDevValue;
    }

    private Map<String, Double> getDeveloperContributions(List<String> allDevs, List<AssetChanged> assetModifications) {
        Map<String, Double> devExp = new HashMap<>();

        for (String dev : allDevs) {
            List<AssetChanged> devLines = assetModifications.parallelStream().filter(b -> b.getAuthor().equalsIgnoreCase(dev)).collect(Collectors.toList());
            double lines = (double)devLines.size() / (double)assetModifications.size();
            devExp.put(dev, lines);
        }
        return devExp;
    }




    @Override
    public MetricValue call() throws Exception {
        double result = 0.0;
        return new MetricValue(null, result, null);
    }

    @Override
    public void run() {
            createMetricsForAsset();
    }




}
