package se.gu.utils;

import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import se.gu.assets.AnnotationType;
import se.gu.assets.Asset;
import se.gu.assets.AssetType;
import se.gu.assets.ProjectAnnotationType;
import se.gu.main.Configuration;
import se.gu.metrics.ged.CallNode;
import se.gu.metrics.ged.FunctionCall;
import se.gu.ml.preprocessing.AbstractionLevel;
import se.gu.parser.fmparser.FeatureTreeNode;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * @Author: Mukelabai Mukelabai
 * @DateCreated: 2019-June-04
 * @DateLastModified: 2019-Jun-12
 */
public class Utilities  implements Serializable {

    private static final long serialVersionUID = -1277272349027929065L;

    //&begin [FileManager]
    public static void deleteFolder(File folder) throws IOException {

        FileUtils.deleteDirectory(folder);
//         deleteFiles(folder);
//         folder.delete();
    }

    public static void deleteFolderContents(File folder) throws IOException {
        //deleteFiles(folder);
        FileUtils.cleanDirectory(folder);
    }

    public static void deleteFile(String path) throws IOException {
        File fsSummary = new File(path);
        if (fsSummary.exists()){
            FileUtils.forceDelete(fsSummary);
        }
    }

    private static void deleteFiles(File folder) {
//        File[] files = folder.listFiles();
//        if (files != null) { //some JVMs return null for empty dirs
//            for (File f : files) {
//                if (f.isDirectory()) {
//                    deleteFolder(f);
//                } else {
//                    f.delete();
//                }
//            }
//        }
    }

    public static File createDirectoryAndDeleteOldFiles(String path) throws IOException {
        File directory = new File(path);
        if (!directory.exists()) {
            directory.mkdir();
        } else {
            FileUtils.cleanDirectory(directory);
        }

        return directory;
    }

    public static void writeOutputFile(String outputFileName, BufferedReader bufferedReader) throws IOException {
        File file;
        FileWriter fileWriter;
        BufferedWriter writer;
        String line;
        file = new File(outputFileName);
        fileWriter = new FileWriter(file.getAbsoluteFile(), false);
        writer = new BufferedWriter(fileWriter);

        while ((line = bufferedReader.readLine()) != null) {
            writer.write(line);
            writer.newLine();
        }
        writer.close();
        fileWriter.close();
    }

    public static void writeListFile(String outputFileName, List<String> list) throws IOException {
        File file;
        FileWriter fileWriter;
        BufferedWriter writer;
        file = new File(outputFileName);
        fileWriter = new FileWriter(file.getAbsoluteFile(), false);
        writer = new BufferedWriter(fileWriter);

        for (String line : list) {
            writer.write(line);
            writer.newLine();
        }
        writer.close();
        fileWriter.close();
    }

    public static File createOutputDirectory(String outputDirectory,boolean cleanDirectoryIfExists) throws IOException {


        File dotFilesDirectory = new File(outputDirectory);
        if (!dotFilesDirectory.exists()) {
            FileUtils.forceMkdir(dotFilesDirectory);
        } else {
            if(cleanDirectoryIfExists){
                FileUtils.cleanDirectory(dotFilesDirectory);
            }
        }


        return dotFilesDirectory;
    }

    public static Path createOutputDirectory(String folderName) throws IOException {
        // Get the user's home directory in a platform-independent way
        String userHome = System.getProperty("user.home");

        // Define the output directory path
        Path outputPath = Paths.get(userHome, folderName);

        // Create the directory if it doesn't exist
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath); // Creates parent dirs if needed
        }

        return outputPath;
    }

    public static void writeStringFile(String outputFileName, String textToWrite) throws IOException {
        File file = new File(outputFileName);
        FileWriter fileWriter = new FileWriter(file.getAbsoluteFile(), false);
        fileWriter.write(textToWrite);
        fileWriter.close();
    }
    public static void writeStringFile(String outputFileName, String textToWrite,boolean append) throws IOException {
        File file = new File(outputFileName);
        FileWriter fileWriter = new FileWriter(file.getAbsoluteFile(), append);
        fileWriter.write(textToWrite);
        fileWriter.close();
    }

    //&end [FileManager]
    //&begin [StringManager]
    public static String removeLeadingSpaces(String param) {
        if (param == null) {
            return null;
        }

        if (param.isEmpty()) {
            return "";
        }

        int arrayIndex = 0;
        while (true) {
            if (!Character.isWhitespace(param.charAt(arrayIndex++))) {
                break;
            }
        }
        return param.substring(arrayIndex - 1);
    }

    public static List<String> textToList(String text, String regex) {
        return new ArrayList<String>(Arrays.asList(text.split(regex)));
    }

    //&end [StringManager]
    //&begin [FeatureFinder]
    public static boolean featuresHaveIntersection(String existingFeature, String testFeature, String parentSeperator) {

        Set<String> list1 = Sets.newHashSet(existingFeature.split(parentSeperator));
        Set<String> list2 = Sets.newHashSet(testFeature.split(parentSeperator));

        Sets.SetView<String> intersection = Sets.intersection(list1, list2);

        return intersection.size() > 0;
    }

    /**
     * Checks whether @testFeature macthes @existingFeature
     * Given list of existing features in the store, we want to check if a given feature exists or not; at which point we would add it to the list of features
     * To check existence, we use the fullyQualified name of the existing feature. Both parameters are converted to SETS then we check that the @testFeature is a subset of @existingFeature
     * We also ensure that the last name in the two sets match; this is to prevent matching any subsets as iluustrated in the example below
     *
     * @param existingFeature
     * @param testFeature
     * @param parentSeperator
     * @return
     * @Example String existing  = "ClaferMooVisualizer::server::polling";
     * String existing1  = "ClaferMooVisualizer::processManagement::polling";
     * String test = "processManagement";
     * String test1 = "server::polling";
     * Note that if we wanted to match @test only using isSubset, we would end up matching feature @existing1 which is incorrect
     * If we wanted to match @test1 only by using the last element in the name, we will get  results matching two features @existing and @existing1
     * Hence we use both isSubset and ensure that the last elements also match; this way, @test1 would match with feature @existing
     */
    public static boolean featureNamesMatch(String existingFeature, String testFeature, String parentSeperator) {
        if (StringUtils.isBlank(existingFeature) || StringUtils.isBlank(testFeature)) {
            return false;
        }
        String[] fA = existingFeature.split(parentSeperator);
        String[] fB = testFeature.split(parentSeperator);
        Set<String> setA = Sets.newHashSet(fA);
        Set<String> setB = Sets.newHashSet(fB);
        boolean isSub = isSubset(setB, setA);
        boolean lastElementIsTheSame = fA[fA.length - 1].equalsIgnoreCase(fB[fB.length - 1]);
        return isSub && lastElementIsTheSame;
    }

    public static <T> boolean isSubset(Set<T> setA, Set<T> setB) {
        return setB.containsAll(setA);
    }
    //&end [FeatureFinder]

    //&begin MLDataGenerator
    public static boolean assetListsHaveIntersection(List<Asset> assetList1, List<Asset> assetList2) {

        Set<Asset> list1 = Sets.newHashSet(assetList1);
        Set<Asset> list2 = Sets.newHashSet(assetList2);

        Sets.SetView<Asset> intersection = Sets.intersection(list1, list2);

        return intersection.size() > 0;
    }

    public static <T> boolean intersectionExists(List<T> assetList1, List<T> assetList2) {

        Set<T> list1 = Sets.newHashSet(assetList1);
        Set<T> list2 = Sets.newHashSet(assetList2);

        Sets.SetView<T> intersection = Sets.intersection(list1, list2);

        return intersection.size() > 0;
    }
    public static <T> int getIntersection(List<T> assetList1, List<T> assetList2) {

        Set<T> list1 = Sets.newHashSet(assetList1);
        Set<T> list2 = Sets.newHashSet(assetList2);

        Sets.SetView<T> intersection = Sets.intersection(list1, list2);

        return intersection.size();
    }
    public static List<String> getTextFromBufferedreader(BufferedReader bufferedReader) throws IOException {
        List<String> stringBuilder = new ArrayList<>();
        String line=null;
        while((line= bufferedReader.readLine())!=null){
            stringBuilder.add(line);
        }
        return stringBuilder;
    }

    public static String getBracketedString(List<FunctionCall> functionCalls) {
        String bracketedString = null;
        CallNode rootNode;
        //first find root node
        Optional<FunctionCall> rootCall = functionCalls.stream().filter(fc -> fc.getSourceNode().getCallers().size() == 0 || fc.getTargetNode().getCallers().size() == 0).findFirst();
        if (rootCall.isPresent()) {
            rootNode = rootCall.get().getSourceNode().getCallers().size() == 0 ? rootCall.get().getSourceNode() : rootCall.get().getTargetNode();
            bracketedString = String.format("{%s}", rootNode.getFullyQualifiedName());
        } else {
            bracketedString = "{ROOTCall}";
        }
        //now go through all pairs
        for (FunctionCall functionCall : functionCalls) {
            bracketedString = setBracketedString(bracketedString, functionCall.getSourceNode());
            bracketedString = setBracketedString(bracketedString, functionCall.getTargetNode());
        }
        return bracketedString;
    }

    private static String setBracketedString(String bracketedString, CallNode callNode) {
        if (!callNode.getFullyQualifiedName().equalsIgnoreCase("ROOTCall")) {
            String nodeString = String.format("{%s}", callNode.getFullyQualifiedName());
            if (bracketedString.contains(nodeString)) {
                bracketedString = bracketedString.replace(nodeString, callNode.getBracketedString());
            } else {
                StringBuffer sb = new StringBuffer(bracketedString);
                sb.insert(bracketedString.lastIndexOf("}"),callNode.getBracketedString());
                bracketedString = sb.toString();
            }

        }
        return bracketedString;
    }
    //&end MLDataGenerator

    public static AssetType getAssetType(AbstractionLevel abstractionLevel) {
        if (abstractionLevel == AbstractionLevel.FOLDER) {
            return AssetType.FOLDER;
        } else if (abstractionLevel == AbstractionLevel.FILE) {
            return AssetType.FILE;
        } else if (abstractionLevel == AbstractionLevel.FRAGMENT) {
            return AssetType.FRAGMENT;
        } else {
            return AssetType.LOC;
        }
    }
    public static AnnotationType getAnnotationTypeFromAbstractionLevel(AbstractionLevel abstractionLevel) {
        if (abstractionLevel == AbstractionLevel.FOLDER) {
            return AnnotationType.FOLDER;
        } else if (abstractionLevel == AbstractionLevel.FILE) {
            return AnnotationType.FILE;
        } else if (abstractionLevel == AbstractionLevel.FRAGMENT) {
            return AnnotationType.FRAGMENT;
        } else {
            return AnnotationType.LINE;
        }
    }

    public static List<Asset> getLinesOfCodeInFragement(Asset fragment){
        return fragment.getParent().getLinesOfCode().stream().filter(l->l.getLineNumber()>=fragment.getStartLine()&&l.getLineNumber()<=fragment.getEndLine()).collect(Collectors.toList());
    }

    //&begin [ML::PreProcessing]
    public static boolean isARFFFile(File file){
        return FilenameUtils.getExtension(file.getAbsolutePath()).equalsIgnoreCase("arff");
    }
    public static boolean isXMLFile(File file){
        return FilenameUtils.getExtension(file.getAbsolutePath()).equalsIgnoreCase("xml");
    }
    public static boolean isDataFile(File file){
        return FilenameUtils.getExtension(file.getAbsolutePath()).equalsIgnoreCase("data");
    }
    //&end [ML::PreProcessing]
    public static List<FeatureTreeNode> getFlatFeatureList(List<FeatureTreeNode> roots){
        List<FeatureTreeNode> features = new ArrayList<>();
        for(FeatureTreeNode root:roots){

            features.addAll(root.flattened().collect(toList()));
        }
        return features;
    }

    public static String getFormattedDurationInMinSec(long milliseconds){
        Duration d =  Duration.ofMillis(milliseconds);
        long minutesPart = d.toMinutes();
        long secondsPart = d.minusMinutes( minutesPart ).getSeconds() ;
        return String.format("%d min %d sec",minutesPart,secondsPart);
    }

    public static boolean isFileContainsAnnotations(String fileText, String allAnnotationPattern) {
        Pattern allAnnotationsPattern = Pattern.compile(allAnnotationPattern);
        Matcher allAnnotationsMatcher = allAnnotationsPattern.matcher(fileText);
        return allAnnotationsMatcher.find();
    }

    public static Matcher GetAnnotationsInFile(String fileText, String allAnnotationPattern) {
        Pattern allAnnotationsPattern = Pattern.compile(allAnnotationPattern);
        Matcher allAnnotationsMatcher = allAnnotationsPattern.matcher(fileText);
        return allAnnotationsMatcher;
    }
    public static Matcher GetAnnotationsInFile(List<String> fileText, String allAnnotationPattern) {
        StringBuilder builder = new StringBuilder();
        for(String line:fileText){
            builder.append(String.format("%s\n",line));
        }
        Pattern allAnnotationsPattern = Pattern.compile(allAnnotationPattern);
        Matcher allAnnotationsMatcher = allAnnotationsPattern.matcher(builder.toString());
        return allAnnotationsMatcher;
    }
    public static String getFeatureFromAnnotation(String annotation, Configuration configuration) {
        String[] array;
        String feature = null;
        if (configuration.getProjectAnnotationType() == ProjectAnnotationType.EMBEDDED_ANNOTATION) {
            array = annotation.split(configuration.getEAGetFeatureRegex());//"((.*)//\\&line)|((.*)//\\&begin)|((.*)//\\&end)"
            if (array.length < 2) {
                feature = array[0].substring(array[0].indexOf("[") + 1, array[0].indexOf("]"));
            } else {
                feature = array[1].substring(array[1].indexOf("[") + 1, array[1].indexOf("]"));
            }
        } else {
            array = annotation.split(configuration.getIfDefGetFeatureRegex());
            feature = Arrays.stream(array).filter(str -> !StringUtils.isBlank(str)).collect(Collectors.joining());
            //System.out.println(feature);
//            if (featureNotFit(feature,configuration)) {
//                return null;
//            }

            feature = cleanFeature(feature);

        }
        if(annotation.contains("ifndef")){
            feature = "not_"+feature;
        }
        return StringUtils.isBlank(feature)?null: feature;


    }
    public static String cleanFeature(String feature){
        
        feature = feature.split(Pattern.quote("/*"))[0];
       
        feature = feature.split(Pattern.quote("*/"))[0];
        feature = feature.split(Pattern.quote("//"))[0];
        feature = feature.split(Pattern.quote("\\\\"))[0];
        feature = feature.split("\\n")[0];
        feature = feature.split(";")[0];
        feature = feature.replace("\"","");
        feature = feature.replace("$","");
        feature = feature.replace(">=","GOEQ");
        feature = feature.replace("<=","LOEQ");
        feature = feature.replace("<","LT");
        feature = feature.replace(">","GT");
        feature = feature.replace("==","EQ");
        feature = feature.replace("!=","NEQ");
        feature = feature.replace("(","_OPBRA_");
        feature = feature.replace(")","_CLBRA_");
        feature = feature.replace("[","_OSQBRA_");
        feature = feature.replace("]","_CSQBRA_");
        feature = feature.replace(",","_COMMA_");
        feature = feature.replace(":","_COLON_");
        feature = feature.replace("&&","AND");
        feature = feature.replace("||","OR");
        feature = feature.replace("!","NOT_");
        feature = feature.replace("@","");
        feature = feature.replace(".","");
        feature = feature.replace("\"","");
        feature = feature.replace("|","");
        feature = feature.replace("","");
        feature = feature.trim();
        feature = feature.replace(" ","_");
        feature = feature.trim();
        return feature;
    }
    
    public static boolean featureNotFit(String feature, Configuration configuration) {
        String[] unfitStrings = configuration.getUnFitFeatureStartString().split(",");
        boolean unfit = false;
        for (String string : unfitStrings) {
            if (feature.contains(string)) {
                unfit = true;
                break;
            }
        }
        return unfit;
    }
    public static boolean extensionAllowed(File file,Configuration configuration) {
        if (file == null || !file.getName().contains(".")) {
            return false;//return false for all files without extensions
        }
        String extension = file.getName().substring(file.getName().lastIndexOf("."));
        List<String> allowedFileExtensions = Arrays.asList(configuration.getAllowedFileExtensions().split(","));
        return file.exists() && allowedFileExtensions.contains(extension);

    }
    public static int getAvailableFutures(LocalExecutionRunner executionRunner,int maxFutures){
        int available = 0;
//check which futres are done
        for(Future f : executionRunner.getFutures()){
            if(f.isDone()||f.isCancelled()){
                available++;
            }
        }
//add the remaining spaces
        available += maxFutures- executionRunner.getFutures().size();

        return available;
    }
    public static void createFuture(Runnable task, LocalExecutionRunner executionRunner) {
        executionRunner.addFuture((Future<?>) executionRunner.submit(task));
    }

    public static Configuration getConfiguration(Properties properties, File analyisDirectory) {

        Configuration configuration = Configuration.getInstance();
        configuration.setCopiedGitRepositories(new ArrayList<>());
        configuration.setAnalysisDirectory(analyisDirectory);
        //configuration.setProjectRepository(gitRepository);
        configuration.setClonedRepositories(properties.getProperty("ProjectRepository"));
        configuration.setProjectType(properties.getProperty("ProjectType"));
        configuration.setFeatureQualifiedNameSeparator(properties.getProperty("FeatureQualifiedNameSeparator"));
        configuration.setMultiFileMappingSeperator(properties.getProperty("MultiFileMappingSeperator"));
        configuration.setTextEncoding(properties.getProperty("TextEncoding"));
        configuration.setFeatureModelFile(properties.getProperty("FeatureModelFile"));
        configuration.setFolderMappingFile(properties.getProperty("FolderMappingFile"));
        configuration.setFileMappingFile(properties.getProperty("FileMappingFile"));
        configuration.setFragmentAnnotationBegin(properties.getProperty("FragmentAnnotationBegin"));
        configuration.setFragmentAnnotationEnd(properties.getProperty("FragmentAnnotationEnd"));
        configuration.setLineAnnotation(properties.getProperty("LineAnnotation"));
        configuration.setSimulationCommitAuthors(properties.getProperty("SimulationCommitAuthors"));
        configuration.setAllowedFileExtensions(properties.getProperty("AllowedFileExtensions"));
        configuration.setSimulationCommitsGitLogCommand(properties.getProperty("SimulationCommitsGitLogCommand"));
        configuration.setCommitCheckoutCommand(properties.getProperty("CommitCheckoutCommand"));
        configuration.setMultiFeatureAnnotationSeparator(properties.getProperty("MultiFeatureAnnotationSeparator"));
        configuration.setCodeAbstractionLevel(properties.getProperty("CodeAbstractionLevel"));
        configuration.setCallGraphNonLabeledNodeName(properties.getProperty("CallGraphNonLabeledNodeName"));
        configuration.setCallGraphNonLabeledLineName(properties.getProperty("CallGraphNonLabeledLineName"));
        configuration.setProjectLanguage(properties.getProperty("ProjectLanguage"));
        configuration.setUseGraphEditDistanceMetric(properties.getProperty("UseGraphEditDistanceMetric"));
        configuration.setUseFullFeatureNamesInMLDataFile(properties.getProperty("UseFullFeatureNamesInMLDataFile"));
        configuration.setExperimentDataFolder(properties.getProperty("ExperimentDataFolder"));
        configuration.setExecutionMethod(properties.getProperty("ExecutionMethod"));
        configuration.setMlMethod(properties.getProperty("MLMethods"));
        configuration.setEvaluationMethod(properties.getProperty("EvaluationMethod"));
        configuration.setkFolds(properties.getProperty("KFolds"));
        configuration.setChainIterations(properties.getProperty("ChainIterations"));
        configuration.setUnlabledAssetComparision(properties.getProperty("UnlabledAssetComparision"));
        configuration.setUnlabledAssetComparisionKNN(properties.getProperty("UnlabledAssetComparisionKNN"));
        configuration.setEvaluationModels(properties.getProperty("EvaluationModels"));
        configuration.setPredictionModels(properties.getProperty("PredictionModels"));
        configuration.setFinalPredictionModels(properties.getProperty("FinalPredictionModels"));
        configuration.setRankRelevantLabelsOnly(properties.getProperty("RankRelevantLabelsOnly"));
        configuration.setFragmentClusterThreshold(properties.getProperty("FragmentClusterThreshold"));
        configuration.setIncludeScenarioCalculation(properties.getProperty("IncludeScenarioCalculation"));
        configuration.setmLSMOTELabelCombination(properties.getProperty("MLSMOTELabelCombination"));
        configuration.setUseDataBalancer(properties.getProperty("UseDataBalancer"));
        configuration.setRunDatabalancer(properties.getProperty("RunDatabalancer"));
        configuration.setUseHierachicalLabeling(properties.getProperty("UseHierachicalLabeling"));
        configuration.setUseSparseInstance(properties.getProperty("UseSparseInstance"));
        configuration.setGenerateDatasetStatistics(properties.getProperty("GenerateDatasetStatistics"));
        configuration.setRunExperiment(properties.getProperty("RunExperiment"));
        configuration.setProjectAnnotationType(properties.getProperty("ProjectAnnotationType"));
        configuration.setRunCrossValidation(properties.getProperty("RunCrossValidation"));
        configuration.setOrderCommitsByCloneOrder(properties.getProperty("OrderCommitsByCloneOrder"));
        configuration.setRunPredictions(properties.getProperty("RunPredictions"));
        configuration.setRunRDataPlots(properties.getProperty("RunRDataPlots"));
        configuration.setrDataFolder(properties.getProperty("RDataFolder"));
        configuration.setCopyExpResultsToRFolder(properties.getProperty("CopyExpResultsToRFolder"));
        configuration.setEAGetFeatureRegex(properties.getProperty("EAGetFeatureRegex"));
        configuration.setIfDefGetFeatureRegex(properties.getProperty("IfDefGetFeatureRegex"));
        configuration.setIfDefBegin(properties.getProperty("IfDefBegin"));
        configuration.setIfDefEnd(properties.getProperty("IfDefEnd"));
        configuration.setTextReplacementsFromFeatures(properties.getProperty("TextReplacementsFromFeatures"));
        configuration.setIgnoreIntegerOptions(properties.getProperty("IgnoreIntegerOptions"));
        configuration.setSingleLineComment(properties.getProperty("SingleLineComment"));
        configuration.setUnFitFeatureStartString(properties.getProperty("UnFitFeatureStartString"));
        configuration.setPrintDetailedPredictionResults(properties.getProperty("PrintDetailedPredictionResults"));
        configuration.setFragmentClusteringMethod(properties.getProperty("FragmentClusterMethod"));
        configuration.setNormaliseData(properties.getProperty("NormaliseData"));
        configuration.setAttributesToNormalize(properties.getProperty("AttributesToNormalise"));
        configuration.setPerformFeatureSelection(properties.getProperty("PerformFeatureSelection"));
        configuration.setPerformFeatureSelectionForEveryKDataset(properties.getProperty("PerformFeatureSelectionForEveryKDataset"));
        configuration.setFeatureSelectionMethods(properties.getProperty("FeatureSelectionMethods"));
        configuration.setNormalisationStrategy(properties.getProperty("NormalisationStrategy"));
        configuration.setFeatureSelectionRank(properties.getProperty("FeatureSelectionRank"));
        configuration.setTopNFeaturesMethod(properties.getProperty("TopNFeaturesMethod"));
        configuration.setTopNFeatures(properties.getProperty("TopNFeatures"));
        configuration.setPersistProjectData(properties.getProperty("PersistProjectData"));
        configuration.setCopyRepositoriesToTargetAnalysisFolder(properties.getProperty("CopyRepositoriesToTargetAnalysisFolder"));
        configuration.setCommitsToExecute(properties.getProperty("CommitsToExecute"));
        configuration.setPrintAssetMappings(properties.getProperty("PrintAssetMappings"));
        configuration.setExperimentStartingCommit(properties.getProperty("ExperimentStartingCommit"));
        configuration.setPrintProjectStats(properties.getProperty("PrintProjectStats"));
        configuration.setProjectCopiedRepositoriesForStats(properties.getProperty("ProjectCopiedRepositoriesForStats"));
        configuration.setUseOnlyAssetsWithKnownLabelsForExperiment(properties.getProperty("UseOnlyAssetsWithKnownLabelsForExperiment"));
        configuration.setUseMaxForPredictionMetricAggregation(properties.getProperty("UseMaxForPredictionMetricAggregation"));
        configuration.setCalculateMetrics(properties.getProperty("CalculateMetrics"));
        configuration.setUseMetricCommitsFromCSV(properties.getProperty("UseMetricCommitsFromCSV"));
        configuration.setCommitCSVs(properties.getProperty("CommitCSVFiles"));
        configuration.setGenerateARRFFiles(properties.getProperty("GenerateARRFFiles"));
        configuration.setMultipleFeatureRegex(properties.getProperty("MultipleFeatureRegex"));
        configuration.setMetricCalculationBasis(properties.getProperty("MetricCalculationBasis"));
        configuration.setPrintMetricDetails(properties.getProperty("PrintMetricDetails"));
        configuration.setGetAnnotationsWithoutCommits(properties.getProperty("GetAnnotationsWithoutCommits"));
        configuration.setAnnotatedLineIsImmediatelyAfterLineAnnotation(properties.getProperty("AnnotatedLineIsImmediatelyAfterLineAnnotation"));
        configuration.setAllAnnotationPatterns(properties.getProperty("AllAnnotationPatterns"));
        configuration.setCamelCaseSplitRegex(properties.getProperty("CamelCaseSplitRegex"));
        configuration.setRunWithRepoDriller(properties.getProperty("RunWithRepoDriller"));
        configuration.setNumberOfThreads(properties.getProperty("NumberOfThreads"));
        configuration.setCommitsForFeatRacerExperiment(properties.getProperty("CommitsForFeatRacerExperiment"));
        configuration.setPerformFeatureCounts(properties.getProperty("PerformFeatureCounts"));
        configuration.setTrainDataFile(properties.getProperty("TrainDataFile"));
        configuration.setTestDataFile(properties.getProperty("TestDataFile"));
        configuration.setFeatureFileMapping(properties.getProperty("FeatureFileMapping"));
        configuration.setLabeledFilesPath(properties.getProperty("LabeledFilesPath"));
        configuration.setLabeledFeaturesPath(properties.getProperty("LabeledFeaturesPath"));
        configuration.setDefectRunMode(properties.getProperty("DefectRunMode"));
        configuration.setMetricCalculationCommitBased(properties.getProperty("MetricCalculationCommitBased"));
        configuration.setDataBaseConnectionString(properties.getProperty("DataBaseConnectionString"));
        configuration.setSaveDataInDataBase(properties.getProperty("SaveDataInDataBase"));
        configuration.setCommitDataSetParentFolder(properties.getProperty("CommitDataSetParentFolder"));
        configuration.setReleaseDataSetParentFolder(properties.getProperty("ReleaseDataSetParentFolder"));
        configuration.setCommitFilesParentFolder(properties.getProperty("CommitFilesParentFolder"));
        configuration.setProjectShortNames(properties.getProperty("ProjectShortNames"));
        configuration.setCrossProjectCommitFolder(properties.getProperty("CrossProjectCommitFolder"));
        configuration.setCrossProjectReleaseFolder(properties.getProperty("CrossProjectReleaseFolder"));
        configuration.setProjectCombinationsFile(properties.getProperty("ProjectCombinationsFile"));
        configuration.setGenerateLuceneData(properties.getProperty("GenerateLuceneData"));
        configuration.setLuceneThreshold(properties.getProperty("LuceneThreshold"));
        configuration.setStartingCommitIndex(properties.getProperty("StartingCommitIndex"));
        configuration.setTrainingDataIncludesUnMappedAssets(properties.getProperty("TrainingDataIncludesUnMappedAssets"));
        configuration.setProjectNamesList(properties.getProperty("ProjectNamesList"));
        configuration.setProjectShortNameList(properties.getProperty("ProjectShortNameList"));
        configuration.setAssetTypesToPredict(properties.getProperty("AssetTypesToPredict"));
        configuration.setClassifierForCombinedResults(properties.getProperty("ClassifierForCombinedResults"));
        configuration.setCommitCSVFilesCommitBasedPrediction(properties.getProperty("CommitCSVFilesCommitBasedPrediction"));
        configuration.setReadAllAssetsFromEachCommit(properties.getProperty("ReadAllAssetsFromEachCommit"));
        return configuration;
    }


}
