package se.gu.ml.preprocessing;

import com.google.common.base.Stopwatch;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import se.gu.assets.AnnotationType;
import se.gu.assets.Asset;
import se.gu.assets.AssetType;
import se.gu.assets.FeatureAssetMap;
import se.gu.defectpred.FeatureStructreMetric;
import se.gu.defectpred.StructureMetricCalculator;
import se.gu.defectpred.StructureMetricSummaryCalculator;
import se.gu.git.Commit;
import se.gu.main.ProjectData;
import se.gu.metrics.structure.AssetMetrics;
import se.gu.metrics.structure.FeatureMetrics;
import se.gu.metrics.structure.FileFeatureMetric;
import se.gu.parser.fmparser.FeatureTreeNode;
import se.gu.utils.CommandRunner;
import se.gu.utils.LocalExecutionRunner;
import se.gu.utils.Utilities;
import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;

import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class FileMetricCalculator {
    private ProjectData projectData;
    private String currentCommit;
    private String startingCommit;
    private String currentRelease;
    private int commitIndex;
    private boolean isFeatureBased;
    private List<String> filesChangedInRelease;
    private LocalExecutionRunner metricsExecutorService;
    private Commit commit;

    public FileMetricCalculator(ProjectData projectData, Commit commit, int commitIndex, List<String> changedFilesInRelease, LocalExecutionRunner metricsExecutorService) {
        this.projectData = projectData;
        this.currentCommit = commit.getCommitHash();
        this.startingCommit = commit.getPreviousCommitHash();
        this.currentRelease = commit.getRelease();
        this.commitIndex = commit.getCommitId();// commitIndex;
        this.filesChangedInRelease = changedFilesInRelease;
        this.isFeatureBased = projectData.getConfiguration().getMetricCalculationBasis().equalsIgnoreCase("Feature");
        this.metricsExecutorService = metricsExecutorService;
        this.commit = commit;
    }

    private void createFeatureMetric(List<FileFeatureMetric> fileFeatureMetrics, ProjectData projectData, boolean isFeatureBased, String feature, Asset file, int featuresInFile) {
        FeatureMetrics featureMetrics = new FeatureMetrics(fileFeatureMetrics, projectData, isFeatureBased, feature, file, featuresInFile);
        createFuture(featureMetrics);
    }

    private void createFuture(Runnable sldMetric) {
        metricsExecutorService.addFuture((Future<?>) metricsExecutorService.submit(sldMetric));
    }

    public void calculateMetrics() throws IOException, SQLException, InterruptedException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        String commitBased = projectData.getConfiguration().isMetricCalculationCommitBased() ? "commitBased" : "";

        String metricsFolder = String.format("%s/FBFMetrics/%s/%s", projectData.getConfiguration().getAnalysisDirectory(), isFeatureBased ? "features" : "files", projectData.getConfiguration().getProjectRepository().getName());
        Utilities.createOutputDirectory(metricsFolder, false);
        File projectMetricsFile = new File(String.format("%s/%s_%s_%s_%s_metrics_detailed.csv", metricsFolder, projectData.getConfiguration().getProjectRepository().getName(), currentRelease, currentCommit, commitBased));
        boolean createprojectMetricsFileHeader = false;
        if (!projectMetricsFile.exists()) {
            createprojectMetricsFileHeader = true;
        }
        File releaseBasedProjectMetricsSummaryFile = new File(String.format("%s/%s_%s_metrics_summary.csv", metricsFolder, projectData.getConfiguration().getProjectRepository().getName(), commitBased));
        File commitBasedProjectMetricsSummaryFile = new File(String.format("%s/%s_%s_metrics_summary.csv", metricsFolder, projectData.getConfiguration().getProjectRepository().getName(), commitBased));
        //File previousCommitBasedProjectMetricsSummaryFile = new File(String.format("%s/%s_%s_previous_metrics_summary.csv", metricsFolder, projectData.getConfiguration().getProjectRepository().getName(), commitBased));

        //File projectMetricsSummaryFile = new File(String.format("%s/%s.csv", metricsFolder, currentCommit));

        boolean createprojectMetricsSummaryFileHeader = false;
        if (projectData.getConfiguration().isMetricCalculationCommitBased() && !commitBasedProjectMetricsSummaryFile.exists()) {
            createprojectMetricsSummaryFileHeader = true;
        }
        if (!projectData.getConfiguration().isMetricCalculationCommitBased() && !releaseBasedProjectMetricsSummaryFile.exists()) {
            createprojectMetricsSummaryFileHeader = true;
        }

        //read existing summary commit metrics=======FOR NOW WE DON"T NEED TO CAACULATE DLETED AND ADDED LINES FOR FEATURE this is done based on actuall extsing commits in DB, see pyhon script
//        List<FeatureStructreMetric> previousFeatureStructreMetrics = new ArrayList<>();
//        if (previousCommitBasedProjectMetricsSummaryFile.exists() && startingCommit != null) {
//            List<String> lines = FileUtils.readLines(previousCommitBasedProjectMetricsSummaryFile, projectData.getConfiguration().getTextEncoding());
//            for (String line : lines) {
//                //only get lines from the previous commit since we want to get thelatest feature size and compare it to the curent and get lines added or deleted from previous commit
//                if (line != null && line.contains(startingCommit)) {
//                    previousFeatureStructreMetrics.add(new FeatureStructreMetric(line));
//                }
//            }
//        }

        List<FileFeatureMetric> fileFeatureMetrics = new CopyOnWriteArrayList<>();
        List<FeatureStructreMetric> fileFeatureMetricsSummary = new CopyOnWriteArrayList<>();
        metricsExecutorService = new LocalExecutionRunner();
        int maxFutures = projectData.getConfiguration().getNumberOfThreads();
        List<Asset> changedFiles = projectData.getChangedAssetsByType(AssetType.FILE);
        if (changedFiles.size() <= 0) {
            return;
        }
        //Get all features mapped to anyone file in the files changed within a release

        //int maxFutures = projectData.getConfiguration().getNumberOfThreads();
        if (isFeatureBased) {
            List<String> allFeatures = new ArrayList<>();
            if (projectData.getConfiguration().isMetricCalculationCommitBased()) {
                if (projectData.getConfiguration().isSaveDataInDataBase()) {
                    allFeatures = projectData.getDataController().getAllMappedFeatures(projectData.getConfiguration().getProjectRepository().getName());
                } else {

                    allFeatures = projectData.getAssetFeatureMap().parallelStream().map(FeatureAssetMap::getFeatureName).filter(f -> !f.contains("::")).distinct().collect(Collectors.toList());
                }
            } else {
                for (String file : filesChangedInRelease) {
                    if (projectData.getConfiguration().isSaveDataInDataBase()) {
                        allFeatures.addAll(projectData.getDataController().getAllMappedFeatures(projectData.getConfiguration().getProjectRepository().getName(), file.replace("\\", "/")));
                    } else {
                        allFeatures.addAll(projectData.getAssetFeatureMap().parallelStream().filter(m -> m.getMappedAsset().getFullyQualifiedName().replace("\\", "/").contains(file))
                                .map(FeatureAssetMap::getFeatureName).collect(Collectors.toList()));
                    }
                }
                allFeatures = allFeatures.parallelStream().filter(f -> !f.contains("::")).distinct().collect(Collectors.toList());
            }


            //int futureCount = 0;
            for (String feature : allFeatures) {

                if(Utilities.getAvailableFutures(metricsExecutorService,maxFutures)==0){
                    Thread.sleep(5);
                }
                StructureMetricCalculator calculator = new StructureMetricCalculator(fileFeatureMetrics,projectData,feature,isFeatureBased);
                Utilities.createFuture(calculator,metricsExecutorService);
                //calculateMetric(fileFeatureMetrics, feature);
            }
            metricsExecutorService.waitForTaskToFinish();
            metricsExecutorService.shutdown();


            //calculate summary
            metricsExecutorService = new LocalExecutionRunner();
            for (String feature : allFeatures) {

                if(Utilities.getAvailableFutures(metricsExecutorService,maxFutures)==0){
                    Thread.sleep(5);
                }
                StructureMetricSummaryCalculator calculator = new StructureMetricSummaryCalculator(fileFeatureMetricsSummary,fileFeatureMetrics,feature,null);
                Utilities.createFuture(calculator,metricsExecutorService);
                //calculateMetric(fileFeatureMetrics, feature);
            }
            metricsExecutorService.waitForTaskToFinish();
            metricsExecutorService.shutdown();

            if (projectData.getConfiguration().isPrintMetricDetails()) {
                //print detailed file
                PrintWriter printWriter = new PrintWriter(new FileWriter(projectMetricsFile, true));
                //create header
                if (createprojectMetricsFileHeader) {
                    printWriter.println("feature;scatteringDegree;tanglingDegreeAcrossFiles;nestingDepth;linesOfFeatureCode");
                    createprojectMetricsFileHeader = false;
                }
                for (FileFeatureMetric fm : fileFeatureMetrics) {
                    printWriter.printf("%s;%.1f;%.1f;%.1f;%.1f\n", fm.getFeature(), fm.getScatteringDegree(), fm.getTanglingDegreeAcrossFiles(),
                            fm.getNestingDepth(), fm.getLinesOfFeatureCode());
                }
                if (printWriter != null) {
                    printWriter.close();
                }
            }
            //remove previous commit file
//            if (previousCommitBasedProjectMetricsSummaryFile.exists()) {
//                Utilities.deleteFile(previousCommitBasedProjectMetricsSummaryFile.getAbsolutePath());
//            }
//            PrintWriter previousCommitWriter = new PrintWriter(new FileWriter(previousCommitBasedProjectMetricsSummaryFile, true));
//            previousCommitWriter.println("releaseID;release;commitID;commitHash;feature;scatteringDegree;tanglingDegreeAcrossFiles;nestingDepth;linesOfFeatureCode;fAddedLines;fDeletedLines");

            //now print summary
            PrintWriter writer = null;

            if (projectData.getConfiguration().isMetricCalculationCommitBased()) {
                writer = new PrintWriter(new FileWriter(commitBasedProjectMetricsSummaryFile, true));

            } else {
                writer = new PrintWriter(new FileWriter(releaseBasedProjectMetricsSummaryFile, true));
            }

            //create header
            if (createprojectMetricsSummaryFileHeader) {
                writer.println("releaseID;release;commitID;commitHash;feature;scatteringDegree;tanglingDegreeAcrossFiles;nestingDepth;linesOfFeatureCode");

                createprojectMetricsSummaryFileHeader = false;
            }
            for (FeatureStructreMetric f : fileFeatureMetricsSummary) {
//                double nestingDepth = fileFeatureMetrics.parallelStream().filter(f -> f.getFeature().equalsIgnoreCase(feature) && f.getNestingDepth() > 0)
//                        .map(FileFeatureMetric::getNestingDepth)
//                        .mapToDouble(Double::doubleValue).average().orElse(0);
//                double tanglingDegree = fileFeatureMetrics.parallelStream().filter(f -> f.getFeature().equalsIgnoreCase(feature) && f.getTanglingDegreeAcrossFiles() > 0)
//                        .map(FileFeatureMetric::getTanglingDegreeAcrossFiles)
//                        .mapToDouble(Double::doubleValue).average().orElse(0);
//                double linesOfFeatureCode = fileFeatureMetrics.parallelStream().filter(f -> f.getFeature().equalsIgnoreCase(feature) && f.getLinesOfFeatureCode() > 0)
//                        .map(FileFeatureMetric::getLinesOfFeatureCode)
//                        .mapToDouble(Double::doubleValue).average().orElse(0);
//                double scatteringDegree = fileFeatureMetrics.parallelStream().filter(f -> f.getFeature().equalsIgnoreCase(feature) && f.getScatteringDegree() > 0)
//                        .map(FileFeatureMetric::getScatteringDegree)
//                        .mapToDouble(Double::doubleValue).average().orElse(0);
//                tanglingDegree = tanglingDegree + nestingDepth;
                writer.printf("%d;%s;%d;%s;%s", commit.getReleaseId(), commit.getRelease(), commit.getCommitId(), commit.getCommitHash(), f.getFeature());
                //previousCommitWriter.printf("%d;%s;%d;%s;%s", commit.getReleaseId(), commit.getRelease(), commit.getCommitId(), commit.getCommitHash(), f.getFeature());
                writer.printf(";%.1f", f.getScatteringDegree());
                //previousCommitWriter.printf(";%.1f", f.getScatteringDegree());
                writer.printf(";%.1f", f.getTanglingDegreeAcrossFiles());
                //previousCommitWriter.printf(";%.1f", f.getTanglingDegreeAcrossFiles());
                writer.printf(";%.1f", f.getNestingDepth());
                //previousCommitWriter.printf(";%.1f", f.getNestingDepth());
                writer.printf(";%.1f\n", f.getLinesOfFeatureCode());
                //previousCommitWriter.printf(";%.1f", f.getLinesOfFeatureCode());


            }
            if (writer != null) {
                writer.close();
            }
            //previousCommitWriter.close();

        } else {
            //int futureCount = 0;
            for (Asset file : changedFiles) {
                List<String> fileFeatures = projectData.getFeaturesOfFileSubAssetsByFileName(file); //projectData.getFeaturesOfChildren(file);
                int featuresInFile = fileFeatures.size();
                for (String feature : fileFeatures) {
//                    if (futureCount < maxFutures) {
//                        createFeatureMetric(fileFeatureMetrics,projectData,isFeatureBased,feature,file,featuresInFile);
//                        futureCount++;
//                    }
//                    if (futureCount == maxFutures) {
//                        metricsExecutorService.waitForTaskToFinish();
//                        futureCount=0;
//                    }

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
            //metricsExecutorService.waitForTaskToFinish();
            //metricsExecutorService.shutdown();

            //print detailed file
            if (projectData.getConfiguration().isPrintMetricDetails()) {
                PrintWriter printWriter = new PrintWriter(new FileWriter(projectMetricsFile, true));
                //create header
                if (createprojectMetricsFileHeader) {
                    printWriter.println("file;feature;scatteringDegree;tanglingDegreeWithinFile;tanglingDegreeAcrossFiles;nestingDepth;linesOfFeatureCode");
                    createprojectMetricsFileHeader = false;
                }
                for (FileFeatureMetric fm : fileFeatureMetrics) {
                    printWriter.printf("%s;%s;%.1f;%.1f;%.1f;%.1f;%.1f\n", fm.getFile(), fm.getFeature(), fm.getScatteringDegree(), fm.getTanglingDegreeWithinFile(), fm.getTanglingDegreeAcrossFiles(),
                            fm.getNestingDepth(), fm.getLinesOfFeatureCode());
                }
                if (printWriter != null) {
                    printWriter.close();
                }
            }
            //now print summary
            PrintWriter writer = new PrintWriter(new FileWriter(releaseBasedProjectMetricsSummaryFile, true));
            //create header
            if (createprojectMetricsSummaryFileHeader) {
                writer.println("commitNo;commitHash;file;scatteringDegree;tanglingDegreeWithinFile;tanglingDegreeAcrossFiles;nestingDepth;linesOfFeatureCode;numberOfFeaturesInFile");
                createprojectMetricsSummaryFileHeader = false;
            }
            for (Asset file : changedFiles) {
                writer.printf("%d;%s;%s", commitIndex, currentCommit, file.getFullyQualifiedName());
                writer.printf(";%.1f", fileFeatureMetrics.parallelStream().filter(f -> f.getFile().equalsIgnoreCase(file.getFullyQualifiedName()) && f.getScatteringDegree() > 0)
                        .map(FileFeatureMetric::getScatteringDegree)
                        .mapToDouble(Double::doubleValue).average().orElse(0));
                writer.printf(";%.1f", fileFeatureMetrics.parallelStream().filter(f -> f.getFile().equalsIgnoreCase(file.getFullyQualifiedName()) && f.getTanglingDegreeWithinFile() > 0)
                        .map(FileFeatureMetric::getTanglingDegreeWithinFile)
                        .mapToDouble(Double::doubleValue).average().orElse(0));
                writer.printf(";%.1f", fileFeatureMetrics.parallelStream().filter(f -> f.getFile().equalsIgnoreCase(file.getFullyQualifiedName()) && f.getTanglingDegreeAcrossFiles() > 0)
                        .map(FileFeatureMetric::getTanglingDegreeAcrossFiles)
                        .mapToDouble(Double::doubleValue).average().orElse(0));
                writer.printf(";%.1f", fileFeatureMetrics.parallelStream().filter(f -> f.getFile().equalsIgnoreCase(file.getFullyQualifiedName()) && f.getNestingDepth() > 0)
                        .map(FileFeatureMetric::getNestingDepth)
                        .mapToDouble(Double::doubleValue).average().orElse(0));
                writer.printf(";%.1f", fileFeatureMetrics.parallelStream().filter(f -> f.getFile().equalsIgnoreCase(file.getFullyQualifiedName()) && f.getLinesOfFeatureCode() > 0)
                        .map(FileFeatureMetric::getLinesOfFeatureCode)
                        .mapToDouble(Double::doubleValue).average().orElse(0));
                writer.printf(";%.1f\n", fileFeatureMetrics.parallelStream().filter(f -> f.getFile().equalsIgnoreCase(file.getFullyQualifiedName()))
                        .map(FileFeatureMetric::getNumberOffeaturesInFile)
                        .mapToDouble(Integer::doubleValue).average().orElse(0));
            }
            if (writer != null) {
                writer.close();
            }
        }
        stopwatch.stop();
        System.out.printf("\ntime for calculating metrics is %s seconds\n", stopwatch.elapsed(TimeUnit.SECONDS));

    }

    private void calculateMetric(List<FileFeatureMetric> fileFeatureMetrics, String feature) throws SQLException {
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

