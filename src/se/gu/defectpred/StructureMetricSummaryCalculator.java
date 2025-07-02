package se.gu.defectpred;

import se.gu.metrics.structure.FileFeatureMetric;

import java.util.List;
import java.util.Optional;

public class StructureMetricSummaryCalculator implements Runnable{
    private List<FeatureStructreMetric> fileFeatureMetricsSummary;
    private List<FileFeatureMetric> fileFeatureMetrics;
    private String feature;
    //private List<FeatureStructreMetric> previousFeatureStructreMetrics;

    public StructureMetricSummaryCalculator(List<FeatureStructreMetric> fileFeatureMetricsSummary, List<FileFeatureMetric> fileFeatureMetrics, String feature, List<FeatureStructreMetric> previousFeatureStructreMetrics) {
        this.fileFeatureMetricsSummary = fileFeatureMetricsSummary;
        this.fileFeatureMetrics = fileFeatureMetrics;
        this.feature = feature;
        //this.previousFeatureStructreMetrics = previousFeatureStructreMetrics;
    }

    @Override
    public void run() {
        double nestingDepth = fileFeatureMetrics.parallelStream().filter(f -> f.getFeature().equalsIgnoreCase(feature) && f.getNestingDepth() > 0)
                .map(FileFeatureMetric::getNestingDepth)
                .mapToDouble(Double::doubleValue).average().orElse(0);
        double tanglingDegree = fileFeatureMetrics.parallelStream().filter(f -> f.getFeature().equalsIgnoreCase(feature) && f.getTanglingDegreeAcrossFiles() > 0)
                .map(FileFeatureMetric::getTanglingDegreeAcrossFiles)
                .mapToDouble(Double::doubleValue).average().orElse(0);
        double linesOfFeatureCode = fileFeatureMetrics.parallelStream().filter(f -> f.getFeature().equalsIgnoreCase(feature) && f.getLinesOfFeatureCode() > 0)
                .map(FileFeatureMetric::getLinesOfFeatureCode)
                .mapToDouble(Double::doubleValue).average().orElse(0);
        double scatteringDegree = fileFeatureMetrics.parallelStream().filter(f -> f.getFeature().equalsIgnoreCase(feature) && f.getScatteringDegree() > 0)
                .map(FileFeatureMetric::getScatteringDegree)
                .mapToDouble(Double::doubleValue).average().orElse(0);
        tanglingDegree = tanglingDegree + nestingDepth;

        double addedLines=linesOfFeatureCode,deletedLines=0.0;
//get previous code size for feature
//            Optional<FeatureStructreMetric> metric = previousFeatureStructreMetrics.parallelStream().filter(f -> f.getFeature().equalsIgnoreCase(feature)).findFirst();
//            if (metric.isPresent()) {
//                double previousCodeSize = metric.get().getLinesOfFeatureCode();
//                double diff = linesOfFeatureCode - previousCodeSize;
//                 addedLines = diff >= 0.0 ? diff : 0.0;
//                 deletedLines = diff < 0 ? Math.abs(diff) : 0.0;
//
//            }
            FeatureStructreMetric f = new FeatureStructreMetric();
            f.setFeature(feature);
            f.setNestingDepth(nestingDepth);
            f.setScatteringDegree(scatteringDegree);
            f.setTanglingDegreeAcrossFiles(tanglingDegree);
            f.setLinesOfFeatureCode(linesOfFeatureCode);
//            f.setfAddedLines(addedLines);
//            f.setfDeletedLines(deletedLines);
            fileFeatureMetricsSummary.add(f);

    }
}
