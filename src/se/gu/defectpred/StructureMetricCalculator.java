package se.gu.defectpred;

import org.apache.commons.lang3.StringUtils;
import se.gu.assets.AnnotationType;
import se.gu.assets.Asset;
import se.gu.assets.AssetType;
import se.gu.assets.FeatureAssetMap;
import se.gu.main.ProjectData;
import se.gu.metrics.structure.FileFeatureMetric;
import se.gu.ml.preprocessing.NestingDepthPair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StructureMetricCalculator implements Runnable{
    private  List<FileFeatureMetric> fileFeatureMetrics;
    private ProjectData projectData;
    private String feature;
    private boolean isFeatureBased;

    public StructureMetricCalculator(List<FileFeatureMetric> fileFeatureMetrics, ProjectData projectData, String feature, boolean isFeatureBased) {
        this.fileFeatureMetrics = fileFeatureMetrics;
        this.projectData = projectData;
        this.feature = feature;
        this.isFeatureBased = isFeatureBased;
    }

    @Override
    public void run() {
        try {
            FileFeatureMetric fileFeatureMetric = new FileFeatureMetric();
            fileFeatureMetric.setFeature(feature);
            double sd = projectData.getConfiguration().isSaveDataInDataBase() ? projectData.getDataController().getScatteringDegree(feature, projectData.getConfiguration().getProjectRepository().getName()) :
                    getScatteringDegree(feature);
            fileFeatureMetric.setScatteringDegree(sd);
            double td = projectData.getConfiguration().isSaveDataInDataBase() ? projectData.getDataController().getTanglingDegree(feature, projectData.getConfiguration().getProjectRepository().getName()) :
                    getTanglingDegreeOfFeatureAcrossFiles(feature);
            fileFeatureMetric.setTanglingDegreeAcrossFiles(td);
            double nloc = projectData.getConfiguration().isSaveDataInDataBase() ? projectData.getDataController().getLinesOfFeatureCode(feature, projectData.getConfiguration().getProjectRepository().getName()) :
                    getLinesOfFeatureCode(feature);
            fileFeatureMetric.setLinesOfFeatureCode(nloc);
            fileFeatureMetric.setNestingDepth(getNestingDepth(feature, null));
            fileFeatureMetric.setTanglingDegreeAcrossFiles(td + fileFeatureMetric.getNestingDepth());
            fileFeatureMetrics.add(fileFeatureMetric);
        }catch (Exception ex){
            ex.printStackTrace();
        }
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
