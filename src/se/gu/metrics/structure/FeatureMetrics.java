package se.gu.metrics.structure;

import org.apache.commons.lang3.StringUtils;
import se.gu.assets.AnnotationType;
import se.gu.assets.Asset;
import se.gu.assets.AssetType;
import se.gu.assets.FeatureAssetMap;
import se.gu.main.ProjectData;
import se.gu.ml.preprocessing.MetricValue;
import se.gu.ml.preprocessing.NestingDepthPair;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class FeatureMetrics implements Runnable, Callable<MetricValue>, Serializable {
    private static final long serialVersionUID = 9117715200600237564L;
    private List<FileFeatureMetric> fileFeatureMetrics;
    private ProjectData projectData;
    private boolean isFeatureBased;
    private String feature;
    private Asset file;
    private int featuresInFile;

    public FeatureMetrics(List<FileFeatureMetric> fileFeatureMetrics, ProjectData projectData, boolean isFeatureBased, String feature, Asset file, int featuresInFile) {
        this.fileFeatureMetrics = fileFeatureMetrics;
        this.projectData = projectData;
        this.isFeatureBased = isFeatureBased;
        this.feature = feature;
        this.file = file;
        this.featuresInFile = featuresInFile;
    }

    @Override
    public void run() {
        if(isFeatureBased){
            FileFeatureMetric fileFeatureMetric = new FileFeatureMetric();
            fileFeatureMetric.setFeature(feature);
            fileFeatureMetric.setScatteringDegree(getScatteringDegree(feature));
            fileFeatureMetric.setTanglingDegreeAcrossFiles(getTanglingDegreeOfFeatureAcrossFiles(feature));
            fileFeatureMetric.setLinesOfFeatureCode(getLinesOfFeatureCode(feature));
            fileFeatureMetric.setNestingDepth(getNestingDepth(feature, null));
            fileFeatureMetrics.add(fileFeatureMetric);
        }else {
            FileFeatureMetric fileFeatureMetric = new FileFeatureMetric();
            fileFeatureMetric.setFile(file.getFullyQualifiedName());
            fileFeatureMetric.setFeature(feature);
            fileFeatureMetric.setScatteringDegree(getScatteringDegree(feature));
            fileFeatureMetric.setTanglingDegreeWithinFile(getTanglingDegreeOfFeatureWithinFile(feature, file.getFullyQualifiedName()));
            fileFeatureMetric.setTanglingDegreeAcrossFiles(getTanglingDegreeOfFeatureAcrossFiles(feature));
            fileFeatureMetric.setLinesOfFeatureCode(getLinesOfFeatureCode(feature));
            fileFeatureMetric.setNestingDepth(getNestingDepth(feature, file.getFullyQualifiedName()));
            fileFeatureMetric.setNumberOffeaturesInFile(featuresInFile);
            fileFeatureMetrics.add(fileFeatureMetric);
        }
    }

    @Override
    public MetricValue call() throws Exception {
        double result = 0.0;
        return new MetricValue(null, result, null);
    }

    public double getScatteringDegree(String featureQualifiedName) {
        return projectData.getAssetFeatureMap().parallelStream()
                .filter(m -> m.getFeatureName().equalsIgnoreCase(featureQualifiedName)
                        && m.getAnnotationType() == AnnotationType.FRAGMENT
                        && m.getMappedAsset().getAssetType() == AssetType.FRAGMENT).distinct().count();
    }

    /**
     * Given a feature and file
     * 1. get list of all feature mappings for the given feature and file and get all the features (presence codntions) in the mappings
     * 2. go through each mapped feature string and split it to get features in the precense condition
     * 3. for all features from the combined precense conditions, count the number of unique feature names
     * 4. subtract 1 to exlude the one feature we supplied to measure the tangling degree for
     * Hence, if only one unique feature is returned from the precense conditions, the tangling degree should be 0.
     *
     * @param featureQualifiedName
     * @param fileQualifiedName
     * @return
     */
    public double getTanglingDegreeOfFeatureWithinFile(String featureQualifiedName, String fileQualifiedName) {
        List<String> mappedfeatureNames =
                projectData.getAssetFeatureMap().parallelStream()
                        .filter(m -> m.getFeatureName().equalsIgnoreCase(featureQualifiedName)
                                && m.getMappedAsset().getParent().getFullyQualifiedName().equalsIgnoreCase(fileQualifiedName)
                                && m.getAnnotationType() == AnnotationType.FRAGMENT
                                && m.getMappedAsset().getAssetType() == AssetType.FRAGMENT)
                        .map(FeatureAssetMap::getFeatureName).collect(Collectors.toList());
        List<String> combinedFeatures = new ArrayList<>();
        for (String feature : mappedfeatureNames) {
            combinedFeatures.addAll(getSplitFeatures(feature));
        }
        return combinedFeatures.parallelStream().distinct().count() - 1;
    }

    /**
     * Given a combination of features e.g., "FeatureA && FeatureB && !FeatureC"
     *
     * @param feature
     * @return a List of individual features
     */
    public List<String> getSplitFeatures(String feature) {
        return Arrays.asList(feature.split(projectData.getConfiguration().getMultipleFeatureRegex())).parallelStream()
                .filter(s -> !StringUtils.isBlank(s))
                .map(s -> s.trim())
                .collect(Collectors.toList());
    }

    public double getTanglingDegreeOfFeatureAcrossFiles(String featureQualifiedName) {
        double tang =
                projectData.getAssetFeatureMap().parallelStream()
                        .filter(m -> m.getFeatureName().equalsIgnoreCase(featureQualifiedName)
                                && m.getAnnotationType() == AnnotationType.FRAGMENT
                                && m.getMappedAsset().getAssetType() == AssetType.FRAGMENT)
                        .map(FeatureAssetMap::getTangled).mapToDouble(Integer::doubleValue).sum();
        return tang;
    }

    //we count all lines of code associated with a feature. This includes all code plus comments
    public double getLinesOfFeatureCode(String featureQualifiedName) {
        double size =
                projectData.getAssetFeatureMap().parallelStream()
                        .filter(m -> m.getFeatureName().equalsIgnoreCase(featureQualifiedName)
                                && m.getAnnotationType() == AnnotationType.FRAGMENT
                                && m.getMappedAsset().getAssetType() == AssetType.FRAGMENT)
                        .map(FeatureAssetMap::getMappedAsset).map(Asset::getNloc).mapToDouble(Integer::doubleValue).sum();
        //iterate all assets and get line ranges

        return size;
    }

    public double getNestingDepth(String featureQualifiedName, String fileQualifiedName) {
        if (isFeatureBased) {
            return projectData.getNestingDepthPairs().parallelStream().filter(n -> n.getFeatureName().equalsIgnoreCase(featureQualifiedName))
                    .map(NestingDepthPair::getNestingDepth).mapToDouble(Integer::doubleValue).max().orElse(0);
        } else {
            return projectData.getNestingDepthPairs().parallelStream().filter(n -> n.getFeatureName().equalsIgnoreCase(featureQualifiedName) && n.getAssetName().equalsIgnoreCase(fileQualifiedName))
                    .map(NestingDepthPair::getNestingDepth).mapToDouble(Integer::doubleValue).max().orElse(0);
        }
    }

}
