package se.gu.main;

import com.google.common.base.Stopwatch;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryparser.classic.ParseException;
import org.repodriller.domain.DiffBlock;
import se.gu.assets.*;
import se.gu.git.*;
import se.gu.git.scenarios.AnnotationPresence;
import se.gu.git.scenarios.DeveloperOperation;
import se.gu.git.scenarios.DeveloperScenario;
import se.gu.git.scenarios.ScenarioHandler;
import se.gu.lucene.LuceneIndexer;
import se.gu.metrics.ged.CallGraphGenerator;
import se.gu.ml.preprocessing.DataGenerator;
import se.gu.ml.preprocessing.FileMetricCalculator;
import se.gu.parser.fmparser.FeatureTreeNode;
import se.gu.reader.AnnotationPair;
import se.gu.reader.AnnotationReader;
import se.gu.utils.CommandRunner;
import se.gu.utils.LocalExecutionRunner;
import se.gu.utils.Utilities;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProjectReader implements Serializable {
    private static final long serialVersionUID = 5042765160211870168L;

    public DiffExtractor getDiffExtractor() {
        return diffExtractor;
    }

    public void setDiffExtractor(DiffExtractor diffExtractor) {
        this.diffExtractor = diffExtractor;
    }

    private DiffExtractor diffExtractor;
    private FeatureAssetMapper assetMapper;

    private ProjectData projectData;
    private ScenarioHandler scenarioHandler;

    public ProjectData getProjectData() {
        return projectData;
    }

    public void setProjectData(ProjectData projectData) {
        this.projectData = projectData;
    }

    public ProjectReader(ProjectData projectData) {

        this.projectData = projectData;
        assetMapper = new FeatureAssetMapper(projectData);
        if (projectData.getConfiguration().isIncludeScenarioCalculation()) {
            scenarioHandler = new ScenarioHandler(projectData); //&line [DeveloperScenarios]
        }
    }

    private String currentCommit = null;
    File diffFilesDirectory;

    public void createAnnotationsWithoutCommits() throws IOException, SQLException, ParseException {
        diffFilesDirectory = Utilities.createOutputDirectory(String.format("%s/%s/%s/%s", projectData.getConfiguration().getAnalysisDirectory(), projectData.getConfiguration().getCodeAbstractionLevel(), projectData.getConfiguration().getProjectRepository().getName(), "diffFiles"), false);

        createFileAssetsFromRepository(null);//no commit supllied since we want to get al annotations from the project as is

        //do luce indexing and searching
//            LuceneIndexer luceneIndexer = new LuceneIndexer(projectData);
//            luceneIndexer.indexAssetsForLucene();
//            luceneIndexer.searchAssets();

        //now create CSV with features in each file
        //createCSVSummaryOfFeaturesInFiles();


    }

    public void createProjectData() throws Exception {
        DiffExtractor diffExtractor = new DiffExtractor(projectData.getConfiguration());
        this.diffExtractor = diffExtractor;
        boolean readEntireRepo = false;

        diffExtractor.setCommitHistory();

        if (projectData.getConfiguration().isOrderCommitsByCloneOrder()) {
            diffExtractor.reorderCommits();//take into account cloning
        }
//        File featuresCounts = new File(String.format("%s/%s_featureCounts.csv",projectData.getConfiguration().getAnalysisDirectory(),projectData.getConfiguration().getProjectRepository().getName()));
//        if(featuresCounts.exists()){
//            FileUtils.forceDelete(featuresCounts);
//        }
//        PrintWriter printWriter = new PrintWriter(new FileWriter(featuresCounts,true));
//        printWriter.println("project;featuresAtCommit;featuresAffectedInCommit;featureLOC;featuresAdded;featuresDeleted");

        //sort the commits according to commit dates

        //go through all projects


        int commulativeCommitCount = 0;//&line [Statistics]
        int previousCommitIndex = -1;


        if (diffExtractor != null) {
            diffFilesDirectory = Utilities.createOutputDirectory(String.format("%s/%s/%s/%s", projectData.getConfiguration().getAnalysisDirectory(), projectData.getConfiguration().getCodeAbstractionLevel(), projectData.getConfiguration().getProjectRepository().getName(), "diffFiles"), false);

            CommandRunner commandRunner = new CommandRunner(projectData.getConfiguration());
            List<Commit> commitHistory = diffExtractor.getCommitHistory();
            List<String> lines = diffExtractor.getCSVCommitLines();
            List<String> featureModelFiles = Arrays.asList(projectData.getConfiguration().getFeatureModelFile().split(","));
            Utilities.writeListFile(String.format("%s/%s.txt", diffFilesDirectory, projectData.getConfiguration().getProjectShortName()), commitHistory.stream().map(Commit::getCommitHash).collect(Collectors.toList()));
            //if(true){return;}
            Asset rootAsset = projectData.getProjectRoot();
            List<ProjectClone> clones = diffExtractor.getClones();


            String previousCommit = null;
            int lastRunCommitIndex = 0;
            if (projectData.getConfiguration().isCalculateMetrics() && !projectData.getConfiguration().isMetricCalculationCommitBased()) {
                lastRunCommitIndex = 0;//for release-based metric claultaion always start from 0
            } else {
                lastRunCommitIndex = projectData.getConfiguration().getStartingCommitIndex();
            }
            int commitsToRun = 0;
            if (projectData.getConfiguration().isCalculateMetrics() && !projectData.getConfiguration().isMetricCalculationCommitBased()) {
                commitsToRun = commitHistory.size();//for release-based metric claultaion always run the number of commit pairs per release
            } else {
                commitsToRun = projectData.getConfiguration().getCommitsToExecute() == 0 ? commitHistory.size() : projectData.getConfiguration().getCommitsToExecute();
            }


            try (ProgressBar pb = new ProgressBar("ML DATA:", commitsToRun)) {
                pb.stepTo(lastRunCommitIndex);
                for (int i = lastRunCommitIndex > 0 ? lastRunCommitIndex + 1 : lastRunCommitIndex; i < commitsToRun; i++) {
                    try {
                        Stopwatch stopwatch = Stopwatch.createStarted();


                        pb.step();
                        commulativeCommitCount++;//&line [Statistics]
                        //&begin [Metrics]


                        //&end [Metrics]
                        //initialize list of all changed assets--important for ML data genration
                        projectData.setChangedAssetsInCurrentCommit(null);
                        projectData.setChangedAssetsInCurrentCommit(new CopyOnWriteArrayList<>());
                        projectData.setAssetPairwiseComparisons(null);
                        projectData.setAssetPairwiseComparisons(new CopyOnWriteArrayList<>());
                        projectData.setUnlabledAssetPairwiseComparisons(null);
                        projectData.setUnlabledAssetPairwiseComparisons(new CopyOnWriteArrayList<>());
                        projectData.setUnlabledMLDataSet(null);
                        projectData.setUnlabledMLDataSet(new ConcurrentHashMap<>());
                        projectData.setNestingDepthPairs(null);
                        projectData.setNestingDepthPairs(new CopyOnWriteArrayList<>());//&line [Metrics]


                        projectData.setPreviousFeatureNames(projectData.getFeatureNames());//set previous feature names to be used to check for assets with features not learnt yet
                        if (projectData.getConfiguration().isReadAllAssetsFromEachCommit()) {
                            //reset mappings, assets and feature list
                            projectData.setAssetList(null);
                            projectData.setAssetFeatureMap(null);
                            projectData.setFeatureNames(null);
                            projectData.setAssetList(new CopyOnWriteArrayList<>());
                            projectData.setAssetFeatureMap(new CopyOnWriteArrayList<>());
                            projectData.setFeatureNames(new CopyOnWriteArrayList<>());
                        }

                        currentCommit = commitHistory.get(i).getCommitHash();
//                    if (!lines.contains(currentCommit)) {
//                        continue;
//                    }
                        pb.setExtraMessage(currentCommit);
                        if (i!=lastRunCommitIndex+1&& projectData.getConfiguration().isCalculateMetrics() && projectData.getConfiguration().isMetricCalculationCommitBased() && commitHistory.get(i).isSkipCommit()) {
                            continue;
                        }

                        //commandRunner.getConfiguration().setProjectRepository(commitHistory.get(i).getProject());
//                if (currentCommit.equalsIgnoreCase("cf3970ec70fb366dc159642ce7c6504c00db643f")) {
//                    continue;
//                }
//                        if (i <797) {
//                            continue;//for now stop at 50 commits so that we assess label data
//                        }

                        if (!projectData.getConfiguration().isGetAnnotationsWithoutCommits()) {
                            commandRunner.checkOutCommit(currentCommit);
                        }
                        //now run call graph generator@
                        BufferedReader bufferedReader = null;

                        List<String> filesChangedInRelease = null;//&line [Metrics]

                        //output diff

                        LocalExecutionRunner callGraphService = new LocalExecutionRunner();
                        if (projectData.getConfiguration().isUseGraphEditDistanceMetric()) {
                            Runnable runnable = new CallGraphGenerator(projectData.getConfiguration().getProjectRepository().getAbsolutePath());
                            Future<?> future = callGraphService.submit(runnable);
                            callGraphService.addFuture(future);
                        }
                        //System.out.println("PRINTInG OUT DIFF for:" + currentCommit);
                        previousCommit = i == 0 ? null : commitHistory.get(i - 1).getCommitHash();

                        previousCommitIndex = i == 0 ? -1 : i - 1;
                        List<DiffEntry> diffEntries = null;
                        if (!projectData.getConfiguration().isGetAnnotationsWithoutCommits()) {
                            bufferedReader = commandRunner.getDiffFromOneCommit(currentCommit);

                            diffExtractor.setDiffEntries(bufferedReader);
                            diffEntries = diffExtractor.getDiffEntries();
//                        List<String> diff = Utilities.getTextFromBufferedreader(bufferedReader);
//                        extractDiffs(diff);
//                        if(diffEntries==null){
//                            continue;
//                        }

                            //Get only one feature model before other entries
//                        List<DiffEntry> featureModelDiffEntries = diffEntries.stream()
//                                .filter(df -> featureModelFiles.contains(df.getAddedFileName()))
//                                .collect(Collectors.toList());

//                        for (DiffEntry featureModelDiffEntry : featureModelDiffEntries) {
//                            //System.out.println(featureModelDiffEntry.getDeletedLines());
//                            //System.out.println(featureModelDiffEntry.getAddedLines());
//                            File diffEntryFile = new File(featureModelDiffEntry.getAddedFullyQualifiedName());
//                            List<String> fileTextAsList = FileUtils.readLines(diffEntryFile, projectData.getConfiguration().getTextEncoding());
//                            //Create Feature Model
//                            List<FeatureTreeNode> featureTree = IndentedTextToTreeParser.parse(fileTextAsList, 0);
//                            List<FeatureTreeNode> featureList = Utilities.getFlatFeatureList(featureTree);
//                            updateFeatureModel(featureModelDiffEntry, featureList);
//
////                            projectData.setFeatureList(featureList);
////                            projectData.setFeatureTree(featureTree);
//                            //now set root feature
//                            //System.out.println(IndentedTextToTreeParser.dump(featureTree));
//                        }
                        }
                        if (projectData.getConfiguration().isIncludeScenarioCalculation()) {
                            updateScenariosForAnnotationFiles(diffEntries);//&line [DeveloperScenarios]
                        }
                        //now process other kinds of diffs
                        //run this if generating metrics for release based defetct prediction
                        if (projectData.getConfiguration().isCalculateMetrics() && !projectData.getConfiguration().isMetricCalculationCommitBased()) {

                            List<String> allowedExtensions = Arrays.asList(projectData.getConfiguration().getAllowedFileExtensions().split(","))
                                    .stream().map(s -> s.replace(".", "")).collect(Collectors.toList());
                            filesChangedInRelease = getFilesThatChangedInRelease(commandRunner, commitHistory.get(i).getPreviousCommitHash(), currentCommit);
                            filesChangedInRelease = filesChangedInRelease.parallelStream().filter(s -> allowedExtensions.contains(FilenameUtils.getExtension(s))).collect(Collectors.toList());
                            List<String> absolutePaths = new ArrayList<>();
                            for (String path : filesChangedInRelease) {
                                File f = new File(String.format("%s/%s", projectData.getConfiguration().getProjectRepository(), path));
                                if (f.exists()) {
                                    absolutePaths.add(f.getAbsolutePath());
                                }
                            }
                            generateAssetsFromPaths(absolutePaths,null);

                        } else {
                            //if starting commit >0 then read all assets at current commit
                            if (projectData.getConfiguration().isReadAllAssetsFromEachCommit()) {
                                createFileAssetsFromRepository(currentCommit);
                            } else {
                                if(i==0){
                                    createFileAssetsFromRepository(currentCommit);//when starting just read all files at this state of the repo
                                }
                                else if (lastRunCommitIndex > 0 && i == lastRunCommitIndex + 1) {
                                    createFileAssetsFromRepository(currentCommit);
                                } else {
                                    LocalExecutionRunner executionRunner = new LocalExecutionRunner();
                                    int maxFutures = projectData.getConfiguration().getNumberOfThreads();
                                    if(currentCommit.equalsIgnoreCase("8167c084d933cdd26895547ae721188a6ad99625")){
                                        System.out.println("STOP HERE");
                                    }
                                    for (DiffEntry diffEntry : diffEntries) {
                                        //skip those not added..not handling deletions yet
                                        if (Utilities.getAvailableFutures(executionRunner, maxFutures) == 0) {
                                            Thread.sleep(10);
                                        }
                                        CommitBasedAssetGenerator gen = new CommitBasedAssetGenerator(projectData, diffEntry, assetMapper, commitHistory.get(i), featureModelFiles);
                                        Utilities.createFuture(gen, executionRunner);


                                    }
                                    executionRunner.waitForTaskToFinish();
                                    executionRunner.shutdown();
                                    //createFileAssetsFromRepository(diffFilesDirectory);
                                }
                            }
                        }


                        if (projectData.getConfiguration().isPrintAssetMappings() && !projectData.getConfiguration().isCalculateMetrics()) {
                            Utilities.writeStringFile(String.format("%s/%d_%s_%s.txt", diffFilesDirectory, i + 1, currentCommit, commitHistory.get(i).getProject().getName()), rootAsset.dump());
                            Utilities.writeStringFile(String.format("%s/%d_%s-MAP_%s.txt", diffFilesDirectory, i + 1, currentCommit, commitHistory.get(i).getProject().getName()), projectData.getAssetFeatureMappingsAsStringOutput());
                        }
                        //&begin [Statistics]
                        //write summary stats of project thus far
                        if (i == commitsToRun - 1 && !projectData.getConfiguration().isCalculateMetrics()) {//only do so for last commit
//                        StringBuilder stringBuilder = new StringBuilder();
//                        stringBuilder.append(String.format("Commits: %d\n", commulativeCommitCount));
//                        stringBuilder.append(System.lineSeparator());
//                        setStat(stringBuilder, AssetType.FOLDER, AnnotationType.FOLDER);
//                        setStat(stringBuilder, AssetType.FILE, AnnotationType.FILE);
//                        setStat(stringBuilder, AssetType.FRAGMENT, AnnotationType.FRAGMENT);
//                        setStat(stringBuilder, AssetType.LOC, AnnotationType.LINE);
//                        Utilities.writeStringFile(String.format("%s/%d_%s-STATS_%s.txt", diffFilesDirectory, i + 1, currentCommit, commitHistory.get(i).getProject().getName()), stringBuilder.toString());

                        }
                        //&end [Statistics]

//save projectdata state
                        if (projectData.getConfiguration().isPersistProjectData()) {
                            Utilities.createOutputDirectory(String.format("%s/projectData", projectData.getConfiguration().getAnalysisDirectory()), false);
                            String fileName = projectData.getConfiguration().getProjectDataFileName();
                            File dataFile = new File(fileName);
                            if (dataFile.exists()) {
                                FileUtils.copyFile(dataFile, new File(projectData.getConfiguration().getProjectDataBackupFileName()));
                            }


                            FileOutputStream fileOutputStream = new FileOutputStream(fileName);
                            ObjectOutputStream outputStream = new ObjectOutputStream(fileOutputStream);
                            try {
                                projectData.setLastRunCommitIndex(i);
                                outputStream.writeObject(projectData);
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                outputStream.close();
                                fileOutputStream.close();
                            }

                        }

                        if (projectData.getConfiguration().isGenerateARRFFiles()) {
                            //now genrate ML data
//                System.out.print("DATA GENERATION START: ");
//                CurrentTimeDateCalendar.printCurrentTimeUsingCalendar();
                            if (projectData.getAssetFeatureMap().size() > 0) {
                                //labeled data
                                LocalExecutionRunner metricsExecutorService = new LocalExecutionRunner();
                                DataGenerator dataGenerator = new DataGenerator(i, previousCommitIndex, currentCommit, previousCommit, projectData, metricsExecutorService, null);
                                dataGenerator.createMLData();
                                //create arff
                                if (projectData.getMlDataSet().size() > 0) {
                                    dataGenerator.createARFF();
                                }
                                //UNLABLED DATA
//                        metricsExecutorService = new LocalExecutionRunner();
//                        dataGenerator = new DataGenerator(i, previousCommitIndex, currentCommit, previousCommit, projectData, metricsExecutorService,null);
//                        dataGenerator.createUnLabeledData();
                                if (projectData.getUnlabledMLDataSet() != null && projectData.getUnlabledMLDataSet().size() > 0) {
                                    dataGenerator.createUnlabledARFF();
                                }
                            }
//                System.out.print("DATA GENERATION END: ");
//                CurrentTimeDateCalendar.printCurrentTimeUsingCalendar();
                        }
                        //generate metrics
                        //&begin [Metrics]
                        if (projectData.getConfiguration().isCalculateMetrics()) {
                            //LocalExecutionRunner metricsExecutorService = new LocalExecutionRunner();
                            Commit cCommit = commitHistory.get(i);
                            Commit pCommit = i - 1 < 0 ? null : commitHistory.get(i - 1);

                            cCommit.setPreviousCommitHash(pCommit == null ? null : pCommit.getCommitHash());
                            if (!projectData.getConfiguration().isSaveDataInDataBase()) {
                                if (projectData.getAssetFeatureMap().size() > 0) {
                                    FileMetricCalculator fileMetricCalculator = new FileMetricCalculator(projectData, cCommit, i + 1, filesChangedInRelease, null);
                                    fileMetricCalculator.calculateMetrics();
                                }
                            } else {
                                FileMetricCalculator fileMetricCalculator = new FileMetricCalculator(projectData, cCommit, i + 1, filesChangedInRelease, null);
                                fileMetricCalculator.calculateMetrics();
                            }


                        }

//                    if(projectData.getConfiguration().isPerformFeatureCounts()){
//                        List<Asset> changedAssets = projectData.getAssetsChangedInCommit(currentCommit);
//                        List<String> allAssetsModifiedWithAsset = changedAssets.parallelStream().map(Asset::getFullyQualifiedName).collect(Collectors.toList());
//                   List<String> featuresModifiedWithAssets =     projectData.getAssetFeatureMap().parallelStream().filter(m -> allAssetsModifiedWithAsset.contains(m.getMappedAsset().getFullyQualifiedName())).map(FeatureAssetMap::getFeatureName).distinct().collect(Collectors.toList());
//                   long floc = changedAssets.parallelStream().filter(a->a.getAssetType() == AssetType.LOC).count();
//
//
//                        printWriter.printf("%s;%d;%d;%d\n"
//                                ,projectData.getConfiguration().getProjectRepository().getName()
//                                ,projectData.getFeatureNames().size()
//                                ,featuresModifiedWithAssets.size()
//                                ,floc
//                        );
//
//
//                    }


                        //&end [Metrics]

                        if (projectData.getConfiguration().isGenerateLuceneData()) {

                            LuceneIndexer luceneIndexer = new LuceneIndexer(projectData, i, currentCommit);
                            luceneIndexer.indexAssetsForLucene();
                            luceneIndexer.searchAssets();


                        }
                        stopwatch.stop();
                        System.out.printf("\ntime for processing commitIndex %d, commit %s is %s seconds\n", i, currentCommit, stopwatch.elapsed(TimeUnit.SECONDS));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }


        //dataset statistics


        if (projectData.getConfiguration().isIncludeScenarioCalculation()) {
            scenarioHandler.printScenarios();//&line [DeveloperScenarios]
        }
    }

    private void addChangedAssetFromDiffEntry(DiffEntry diffEntry, String relativePath, File diffFile, Commit currentCommit) {
        List<String> allowedExtensions = Arrays.asList(projectData.getConfiguration().getAllowedFileExtensions().split(",")).parallelStream().map(e -> e.replace(".", "")).collect(Collectors.toList());

        //List<AssetChanged> changedAssets = new ArrayList<>();
        String fileName = diffFile.getAbsolutePath();

        List<Integer> addedLines = new ArrayList<>();
        for (CodeChange codeChange : diffEntry.getCodeChanges()) {
            int start = codeChange.getAddedLinesStart();
            int end = start + codeChange.getAddedLines();
            for (int i = start; i <= end; i++) {
                addedLines.add(i);
            }
        }
        projectData.getAssetChangedList().add(new AssetChanged(fileName, relativePath, addedLines, currentCommit.getCommitHash(), currentCommit.getAuthor()));

    }

    private List<DiffBlock> extractDiffs(List<String> diffLines) {
        //pull out all files changed
        String diffRegex = "\\s*diff\\s*--.";
        Pattern filePattern = Pattern.compile(diffRegex);
        Matcher fileMatcher;
        for (int i = 0; i < diffLines.size(); i++) {
            fileMatcher = filePattern.matcher(diffLines.get(i));
            if (fileMatcher.find()) {
                System.out.println(diffLines.get(i));
                System.out.printf("file is: %s", diffLines.get(i + 2).split("--- a/")[0]);
            }
        }

        return new ArrayList<>();

    }

    private void createFileAssetsFromRepository(String currentCommit) {
        List<String> allowedExtensions = Arrays.asList(projectData.getConfiguration().getAllowedFileExtensions().split(","))
                .stream().map(s -> s.replace(".", "")).collect(Collectors.toList());
        File dir = projectData.getConfiguration().getProjectRepository();

        Path start = Paths.get(dir.getAbsolutePath());
        try (Stream<Path> stream = Files.walk(start, Integer.MAX_VALUE)) {
            List<String> filePaths = stream
                    .map(String::valueOf)
                    .filter(s -> allowedExtensions.contains(FilenameUtils.getExtension(s)) ||
                            s.endsWith(projectData.getConfiguration().getFolderMappingFile()) ||
                            s.endsWith(projectData.getConfiguration().getFileMappingFile()))
                    .sorted()
                    .collect(Collectors.toList());
            generateAssetsFromPaths(filePaths,currentCommit);

        } catch (Exception ex) {
            ex.printStackTrace();
        }


    }

    public void generateAssetsFromPaths(List<String> filePaths,String currentCommit) {
        //create futures
        LocalExecutionRunner executionRunner = new LocalExecutionRunner();
        int maxFutures = projectData.getConfiguration().getNumberOfThreads();
        try (ProgressBar pb = new ProgressBar("creating assets:", filePaths.size())) {
            for (String filePath : filePaths) {

                pb.step();
                pb.setExtraMessage(FilenameUtils.getName(filePath));
                while (Utilities.getAvailableFutures(executionRunner, maxFutures) == 0) {
                    //wait 5 seconds
                    Thread.sleep(1000);
                }
                AssetGenerator gen = new AssetGenerator(new File(filePath), projectData, assetMapper,currentCommit);
                Utilities.createFuture(gen, executionRunner);

                //createFileAssetsForMetrics(new File(filePath));==OSBOLTE NOW USING THEADED ONE

            }
            executionRunner.waitForTaskToFinish();
            executionRunner.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    //&begin [Statistics]
    private void setStat(StringBuilder stringBuilder, AssetType assetType, AnnotationType annotationType) {
        stringBuilder.append(String.format("%sS: %d\n", assetType, projectData.getAssetCount(assetType)));
        stringBuilder.append(System.lineSeparator());
        stringBuilder.append(String.format("%s ANNOTATIONS: %d\n", assetType, projectData.getAnnotationCount(assetType, annotationType)));
        stringBuilder.append(System.lineSeparator());
    }
    //&end [Statistics]

    //&begin [DeveloperScenarios]
    //check if all changed files in this commit are only annotation files i.e. .vp-project, .vp-files, .vp-folder
    private void updateScenariosForAnnotationFiles(List<DiffEntry> diffEntries) {
        List<String> featureModelFiles = Arrays.asList(projectData.getConfiguration().getFeatureModelFile().split(","));
        String folderMappingName = projectData.getConfiguration().getFolderMappingFile();
        String fileMappingName = projectData.getConfiguration().getFileMappingFile();
        int allChangedFiles = diffEntries.size();
        int annotationFileCount = 0;
        for (DiffEntry diffEntry : diffEntries) {
            if (featureModelFiles.contains(diffEntry.getDeletedFileName()) ||
                    featureModelFiles.contains(diffEntry.getAddedFileName()) ||
                    folderMappingName.equalsIgnoreCase(diffEntry.getDeletedFileName()) ||
                    folderMappingName.equalsIgnoreCase(diffEntry.getAddedFileName()) ||
                    fileMappingName.equalsIgnoreCase(diffEntry.getDeletedFileName()) ||
                    fileMappingName.equalsIgnoreCase(diffEntry.getAddedFileName())
            ) {
                annotationFileCount++;
            }
        }

        if (allChangedFiles == annotationFileCount) {
            //log files changed under this scenario
            for (DiffEntry diffEntry : diffEntries) {
                if (diffEntry.getAddedFileName().equalsIgnoreCase(diffEntry.getDeletedFileName())) {
                    scenarioHandler.updateScenario(AssetType.ANNOTATION_FILE, DeveloperOperation.ADDorEDIT, currentCommit, diffEntry.getAddedFullyQualifiedName());
                } else {
                    scenarioHandler.updateScenario(AssetType.ANNOTATION_FILE, DeveloperOperation.ADDorEDIT, currentCommit, diffEntry.getAddedFullyQualifiedName());
                    scenarioHandler.updateScenario(AssetType.ANNOTATION_FILE, DeveloperOperation.ADDorEDIT, currentCommit, diffEntry.getDeletedFullyQualifiedName());
                }
            }

        }
    }
    //&end [DeveloperScenarios]

    private String getHarmonizedFullName(String fullyQualifiedName) {
        String sep = projectData.getConfiguration().getFeatureQualifiedNameSeparator();
        return String.format("%s%s%s", projectData.getRootFeature().getText(), sep, fullyQualifiedName.substring(fullyQualifiedName.indexOf(sep) + sep.length()));
    }

    private void updateFeatureModel(DiffEntry featureModelDiffEntry, List<FeatureTreeNode> latestFeatureModel) {
        String featureSeparator = projectData.getConfiguration().getFeatureQualifiedNameSeparator();
        List<String> oldShortNames = projectData.getFeatureList().stream().map(FeatureTreeNode::getText).collect(Collectors.toList());
        List<String> newFullyQualifiedNames = latestFeatureModel.stream().map(FeatureTreeNode::getFullyQualifiedName).collect(Collectors.toList());
        List<String> newShortNames = latestFeatureModel.stream().filter(f -> f.getParent() != null).map(FeatureTreeNode::getText).collect(Collectors.toList());//leave out root feature
        List<FeatureTreeNode> newFeatures = latestFeatureModel.stream().filter(f -> !oldShortNames.contains(f.getText())).collect(Collectors.toList());
        List<FeatureTreeNode> deletedFeatures = projectData.getFeatureList().stream()
                .filter(f -> !newShortNames.contains(f.getText())
                        && featureModelDiffEntry.getDeletedLines().stream().filter(l -> l.contains(f.getText())).count() > 0
                        && featureModelDiffEntry.getAddedLines().stream().filter(l -> l.contains(f.getText())).count() <= 0)
                .collect(Collectors.toList());

        //handle new features
        for (FeatureTreeNode feature : newFeatures) {
            //skip root features of individual projects
            if (!feature.getFullyQualifiedName().contains(featureSeparator)) {
                continue;
            }
            projectData.addFeatureToList(getHarmonizedFullName(feature.getFullyQualifiedName()));
        }
        //handle old features deleted
        List<String> deletedFeatureNames = deletedFeatures.stream().map(FeatureTreeNode::getFullyQualifiedName).collect(Collectors.toList());
        //remove all mappings for deleted features
        projectData.getAssetFeatureMap().removeIf(a -> deletedFeatureNames.contains(a.getMappedFeature().getFullyQualifiedName()));
        for (FeatureTreeNode deletedFeature : deletedFeatures) {
            deletedFeature.getParent().getChildren().remove(deletedFeature);
            //&begin [DeveloperScenarios]
            if (projectData.getConfiguration().isIncludeScenarioCalculation()) {
                scenarioHandler.updateScenario(AssetType.FEATURE, DeveloperOperation.DELETE, currentCommit, deletedFeature.getFullyQualifiedName());
            }
            //&end [DeveloperScenarios]
        }
        //now delete from featurelist
        projectData.getFeatureList().removeIf(f -> deletedFeatureNames.contains(f.getFullyQualifiedName()));

        //handle old features moved
        List<String> harmonizedNewFullyQuealifiedNames = new ArrayList<>();
        newFullyQualifiedNames.stream().forEach(n -> {
            if (n.contains(featureSeparator)) {
                harmonizedNewFullyQuealifiedNames.add(getHarmonizedFullName(n));
            }
        });
        List<FeatureTreeNode> movedFeatures = projectData.getFeatureList().stream()
                .filter(f -> newShortNames.contains(f.getText()) && oldShortNames.contains(f.getText()) && !harmonizedNewFullyQuealifiedNames.contains(f.getFullyQualifiedName()))
                .collect(Collectors.toList());//moved features are those present in old list of features and new list of features but fully qualified names have changed
        for (FeatureTreeNode movedFeature : movedFeatures) {
            //remove from old parent
            movedFeature.getParent().getChildren().remove(movedFeature);
            if (projectData.getConfiguration().isIncludeScenarioCalculation()) {
                scenarioHandler.updateScenario(AssetType.FEATURE, DeveloperOperation.MOVE, currentCommit, movedFeature.getFullyQualifiedName());//&line [DeveloperScenarios]
            }
            //get new parent

            Optional<FeatureTreeNode> newParent = latestFeatureModel.stream().filter(f -> f.getText().equalsIgnoreCase(movedFeature.getText())).map(FeatureTreeNode::getParent).findFirst();
//add the new parent to old feature model
            FeatureTreeNode addedParent = projectData.addFeatureToList(newParent.get().getFullyQualifiedName());
            //set parent
            movedFeature.setParent(addedParent);
            addedParent.getChildren().add(movedFeature);
            String newQualifiedName = movedFeature.getNewFullyQualifiedName(projectData.getConfiguration().getFeatureQualifiedNameSeparator());
            movedFeature.setFullyQualifiedName(newQualifiedName);
        }
    }

    private void mapVPFile(String vpFilePath) throws IOException {
        File vpFile = new File(vpFilePath);
        if (vpFile.exists()) {
            List<String> fileTextAsList = FileUtils.readLines(vpFile, projectData.getConfiguration().getTextEncoding());
            int records = fileTextAsList.size();
            for (int j = 0; j < records; j += 2) {
                String[] fileNames = fileTextAsList.get(j).split(projectData.getConfiguration().getMultiFileMappingSeperator());
                String[] features = fileTextAsList.get(j + 1).split(projectData.getConfiguration().getMultiFileMappingSeperator());
                //map files to features
                for (String fileName : fileNames) {
                    File file = new File(vpFile.getParentFile().getAbsolutePath() + "/" + fileName);
                    mapFileAssets(file, Arrays.asList(features), AnnotationType.FILE);
                }
            }
        }
    }

    //&begin [DeveloperScenarios]
    private void addFileScenarios(Asset asset, boolean exists, boolean withMappings) {

        if (exists) {
            //was added without mappings
            scenarioHandler.updateScenario(asset.getAssetType(), DeveloperOperation.MAPFEATURE, currentCommit, asset.getFullyQualifiedName());
        } else if (!exists) {
            //being added for the first time
            //folder
            if (asset.getAssetType() == AssetType.FOLDER) {
                DeveloperScenario scenario = new DeveloperScenario(asset.getAssetType(), DeveloperOperation.ADD, withMappings ? AnnotationPresence.YES : AnnotationPresence.NO,
                        AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED);

                scenarioHandler.updateScenario(scenario, asset, currentCommit);
            }


        }
    }
    //&end [DeveloperScenarios]

    private void mapVpFolder(String vpFolderPath) throws IOException {

        File vpFolderFile = new File(vpFolderPath);
        if (vpFolderFile.exists()) {
            File vpFolder = vpFolderFile.getParentFile();
            if (vpFolder.exists()) {
                //&begin [DeveloperScenarios]
                //check if folder exissts in list of assets, and if there any existing annotations; if not it was added without annotations else we are updating list of annotations
                boolean doScenarios = projectData.getConfiguration().isIncludeScenarioCalculation();
                boolean exists = false;
                if (doScenarios) {
                    exists = projectData.getAssetList().stream().anyMatch(a -> a.getFullyQualifiedName().equalsIgnoreCase(vpFolder.getAbsolutePath()));
                }
                //&end [DeveloperScenarios]
                Asset vpAsset = projectData.addFileAsset(vpFolder);//get folder referenced by vp-folderfile
                List<String> fileTextAsList = FileUtils.readLines(vpFolderFile, projectData.getConfiguration().getTextEncoding());
                mapAssetToFeatures(vpAsset, fileTextAsList, AnnotationType.FOLDER);
                logChangedAsset(vpAsset);

                if (doScenarios) {
                    addFileScenarios(vpAsset, exists, true);//&line [DeveloperScenarios]
                }
            }
        }
    }

    private void mapVPFileContents(File directory) throws IOException {
        File[] projectFiles = directory.listFiles();
        for (File projectFile : projectFiles) {
            if (projectFile.isDirectory()) {
                mapVPFileContents(projectFile);
            } else {
                if (projectFile.getName().equalsIgnoreCase(projectData.getConfiguration().getFolderMappingFile())) {
                    mapVpFolder(projectFile.getAbsolutePath());
                } else if (projectFile.getName().equalsIgnoreCase(projectData.getConfiguration().getFileMappingFile())) {
                    mapVPFile(projectFile.getAbsolutePath());
                } else {
                    continue;
                }
            }
        }
    }

    //&begin [Metrics]
    public List<String> getFilesThatChangedInRelease(CommandRunner commandRunner, String startingCommit, String endingCommit) throws IOException {
        BufferedReader reader = commandRunner.getFilesThatChangedBetween(startingCommit, endingCommit);
        List<String> fileNames = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            fileNames.add(line);
        }

        return fileNames;
    }

    private void createFileAssetsForMetrics(File file) throws IOException, SQLException {
        if (file.getName().contains(projectData.getConfiguration().getFolderMappingFile())) {
            assetMapper.mapFolder(file.getAbsolutePath());
        } else if (file.getName().contains(projectData.getConfiguration().getFileMappingFile())) {
            mapVPFile(file.getAbsolutePath());

        } else {
            Asset fileAsset = createFileAndSubAssets(file, false);
            List<String> lines = FileUtils.readLines(file, projectData.getConfiguration().getTextEncoding());
            AnnotationReader reader = new AnnotationReader(projectData, fileAsset, lines);
            List<AnnotationPair> annotationPairs = reader.readFragmentAnnotations();


            FeatureAssetMap featureAssetMap;
            for (AnnotationPair annotationPair : annotationPairs) {
                String annotatedFeatures[] = annotationPair.getFeatureName().split(projectData.getConfiguration().getMultiFeatureAnnotationSeparator());
                //add extra asset to fileAsset for eah annotation pair
                addAnnotationAsAssetToFile(fileAsset, annotationPair, Arrays.asList(annotatedFeatures));

            }
        }
    }


    public void createCSVSummaryOfFeaturesInFiles() throws IOException {
        String folderName = String.format("%s/featureSummaries", projectData.getConfiguration().getAnalysisDirectory());
        Utilities.createOutputDirectory(folderName, false);
        File outputFile = new File(String.format("%s/%s.csv", folderName, projectData.getConfiguration().getProjectRepository().getName()));
        if (outputFile.exists()) {
            FileUtils.forceDelete(outputFile);
        }
        PrintWriter printWriter = new PrintWriter(new FileWriter(outputFile, true));
        //header
        printWriter.println("fileName;lineRange;featureName;annotationGranualarity");
        List<Asset> files = projectData.getAssetsByType(AssetType.FILE);
        for (Asset file : files) {
            //get mappings with this file included
            List<FeatureAssetMap> mappings = projectData.getAssetFeatureMap().stream().filter(a -> a.getMappedAsset().getParent().equals(file)).collect(Collectors.toList());
            for (FeatureAssetMap map : mappings) {
                printWriter.printf("%s;%d-%d;%s;%s\n", file.getFullyQualifiedName(), map.getMappedAsset().getStartLine(), map.getMappedAsset().getEndLine(),
                        map.getMappedFeature().getText(), map.getAnnotationType());
            }

        }

        printWriter.close();

    }

    //&end [Metrics]


    private void setCommitCount(Asset fileAsset, DiffEntry diffEntry) {
        projectData.addAssetChangeCount(fileAsset);
        //now set lines
        List<Integer> changedLineNumbers = new ArrayList<>();//list of all lines changed
        //for manually added files (i.e. files added from function calls, diffEntry will be null whcihch means all lines have to be registered as changed.
        if (diffEntry == null) {
            projectData.addAssetChangeCount(fileAsset.getLinesOfCode());
            changedLineNumbers.addAll(fileAsset.getLinesOfCode().stream().map(Asset::getLineNumber).collect(Collectors.toList()));
        } else {
            for (CodeChange codeChange : diffEntry.getCodeChanges()) {
                int endLineAdd = codeChange.getAddedLinesStart() + codeChange.getAddedLines();
                int endLineDelete = codeChange.getDeletedLinesStart() + codeChange.getDeletedLines();
                List<Asset> linesOfCode = fileAsset.getLinesOfCode().parallelStream().filter(l -> (l.getLineNumber() >= codeChange.getAddedLinesStart() && l.getLineNumber() <= endLineAdd) || (l.getLineNumber() >= codeChange.getDeletedLinesStart() && l.getLineNumber() <= endLineDelete))
                        .collect(Collectors.toList());
                changedLineNumbers.addAll(linesOfCode.stream().map(Asset::getLineNumber).collect(Collectors.toList()));
                projectData.addAssetChangeCount(linesOfCode);
            }
        }

        //now do it for the parents to file
        List<Asset> ancestry = fileAsset.getAncestry();
        projectData.addAssetChangeCount(ancestry);

        //now do it for fragments
        for (Asset fragment : fileAsset.getCodeFragments()) {
            //check if any of the lines in this fragment is among the lines changed in a file
            boolean changed = changedLineNumbers.parallelStream().anyMatch(l -> l >= fragment.getStartLine() && l <= fragment.getEndLine());
            if (changed) {
                projectData.addAssetChangeCount(fragment);
            }
        }
    }

    private void addAnnotationAsAssetToFile(Asset fileAsset, AnnotationPair annotationPair, List<String> mappedFeatures) {
        Asset fragmentAsset = new Asset(annotationPair.getStartLine(), annotationPair.getEndLine(), annotationPair.getAnnotationType(), String.format("%d-%d", annotationPair.getStartLine(), annotationPair.getEndLine()), String.format("%s%s%d-%d", fileAsset.getFullyQualifiedName(), projectData.getConfiguration().getFeatureQualifiedNameSeparator(), annotationPair.getStartLine(), annotationPair.getEndLine()), AssetType.FRAGMENT, fileAsset);
        fileAsset.getChildren().add(fragmentAsset);
        projectData.getAssetList().add(fragmentAsset);
        mapAssetToFeatures(fragmentAsset, mappedFeatures, annotationPair.getAnnotationType());
        if (!projectData.getConfiguration().isGetAnnotationsWithoutCommits()) {
            logChangedAsset(fragmentAsset);
        }
    }

    private void mapFileAssets(File file, List<String> features, AnnotationType annotationType) throws IOException {
        //skip non-existent file
        if (!Utilities.extensionAllowed(file, projectData.getConfiguration())) {
            return;
        }
        Asset fileAsset = createFileAndSubAssets(file, true);
        mapAssetToFeatures(fileAsset, features, annotationType);
//        for (Asset loc : fileAsset.getLinesOfCode()) {
//            mapAssetToFeatures(loc, features, annotationType);
//        }

    }


    private void logChangedAsset(Asset asset) {
        projectData.getChangedAssetsInCurrentCommit().add(asset);
    }

    private Asset createFileForFragmentSubAssets(File file) throws IOException {

        Asset fileAsset = projectData.addFileAsset(file);
        logChangedAsset(fileAsset);
        //reset children
        resetChildren(fileAsset);
        //now add the content also as an asset
        fileAsset.setAssetContent(FileUtils.readFileToString(file, projectData.getConfiguration().getTextEncoding()));
        return fileAsset;
    }

    /**
     * Creates a file and its lines of code as sub-assets
     * Makes use of a seprate thread to
     *
     * @param file
     * @return
     */
    private Asset createFileAndSubAssets(File file, boolean withMappings) throws IOException {
        boolean exists = false;
        if (projectData.getConfiguration().isIncludeScenarioCalculation()) {
            exists = projectData.getAssetList().stream().anyMatch(a -> a.getFullyQualifiedName().equalsIgnoreCase(file.getAbsolutePath()));
        }
        Asset fileAsset = projectData.addFileAsset(file);
        if (!projectData.getConfiguration().isGetAnnotationsWithoutCommits()) {
            logChangedAsset(fileAsset);
        }
        //reset children
        if (!projectData.getConfiguration().isGetAnnotationsWithoutCommits()) {
            resetChildren(fileAsset);
        }
        if (projectData.getConfiguration().isIncludeScenarioCalculation()) {
            addFileScenarios(fileAsset, exists, true);//&line [DeveloperScenarios]
        }
        //lines of code
        List<String> fileLines = FileUtils.readLines(file, projectData.getConfiguration().getTextEncoding());
        int linesCount = fileLines.size();
        StringBuilder stringBuilder = new StringBuilder();
        String line = null;
        for (int l = 0; l < linesCount; l++) {
            fileLines.get(l).replaceAll("[^a-zA-Z0-9\\s]", "");//remove non word characters except white spaces
            //skip all blank lines
            if (StringUtils.isBlank(fileLines.get(l))) {
                continue;
            }
            Asset lineAsset = new Asset(l + 1, String.format("%d", l + 1), String.format("%s%s%d", fileAsset.getFullyQualifiedName(), projectData.getConfiguration().getFeatureQualifiedNameSeparator(), l + 1), AssetType.LOC, fileAsset);
            lineAsset.setAssetContent(fileLines.get(l));
            fileAsset.getLinesOfCode().add(lineAsset);
            fileAsset.getChildren().add(lineAsset);
            projectData.getAssetList().add(lineAsset);
            stringBuilder.append(lineAsset.getAssetContent());
            stringBuilder.append(System.lineSeparator());
            if (!projectData.getConfiguration().isGetAnnotationsWithoutCommits()) {
                logChangedAsset(lineAsset);
            }

        }
        //now add the content also as an asset
        fileAsset.setAssetContent(stringBuilder.toString());
        return fileAsset;
    }

    private void resetChildren(Asset fileAsset) {
        //remove all existing mappings
        List<String> childrenFullyQualifiedNames = fileAsset.getChildrenFullyQualifiedNames();
        projectData.getAssetFeatureMap().removeIf(a -> childrenFullyQualifiedNames.contains(a.getMappedAsset().getFullyQualifiedName()));
        projectData.getMlData().removeIf(a -> childrenFullyQualifiedNames.contains(a.getAssetName()));
        projectData.getAssetList().removeIf(a -> childrenFullyQualifiedNames.contains(a.getFullyQualifiedName()));
        //now get rid of children
        fileAsset.setChildren(null);
        fileAsset.setChildren(new ArrayList<>());
        fileAsset.setLinesOfCode(null);
        fileAsset.setLinesOfCode(new ArrayList<>());


    }

    private void mapFolderAssets(File[] folderFiles, Asset parentAsset, List<String> featureNames, LocalExecutionRunner assetMappingService) throws IOException {
        for (File file : folderFiles) {
            if (file.isDirectory()) {
                //Create folder asset
                Asset folderAsset = projectData.addFileAsset(file);
                mapAssetToFeatures(folderAsset, featureNames, AnnotationType.FOLDER);
                mapFolderAssets(file.listFiles(), folderAsset, featureNames, assetMappingService);
            } else {
                //process file
                mapFileAssets(file, featureNames, AnnotationType.FOLDER);


            }
        }
    }

    public void mapAssetToFeatures(Asset asset, List<String> featureNames, AnnotationType annotationType) {
        for (String featureName : featureNames) {
            if (StringUtils.isBlank(featureName)) {
                continue;
            }
            FeatureTreeNode feature = projectData.addFeatureToList(featureName);

            //create mapping
            // List<FeatureTreeNode> ancenstry = feature.getAncestry();
            // for(FeatureTreeNode featureTreeNode:ancenstry) {
            FeatureAssetMap featureAssetMap = new FeatureAssetMap(feature.getFullyQualifiedName(), asset, annotationType);
            if (!projectData.getAssetFeatureMap().contains(featureAssetMap)) {
                projectData.getAssetFeatureMap().add(featureAssetMap);
            }
        }

        //}

    }
}
