package se.gu.main;

import org.apache.commons.lang3.StringUtils;
import se.gu.assets.FragmentClusteringMethod;
import se.gu.assets.ProjectAnnotationType;
import se.gu.git.ProjectType;
import se.gu.metrics.UnlabledAssetComparison;
import se.gu.ml.experiment.EvaluationMethod;
import se.gu.ml.experiment.FeatureSelectionRank;
import se.gu.ml.experiment.MLMethod;
import se.gu.ml.experiment.TopNFeaturesMethod;
import se.gu.ml.preprocessing.AbstractionLevel;
import se.gu.ml.preprocessing.NormalisationStrategy;

import java.io.File;
import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Configuration implements Serializable {
    private static final long serialVersionUID = -913369537380759945L;
    private static Configuration ourInstance = new Configuration();

    public static Configuration getInstance() {
        return ourInstance;
    }

    private Configuration() {
        projectShortNameMap = new HashMap<>();
    }

    public File getProjectRepository() {
        return projectRepository;
    }

    public void setProjectRepository(File projectRepository) {
        this.projectRepository = projectRepository;
    }

    public File getAnalysisDirectory() {
        return analysisDirectory;
    }

    public void setAnalysisDirectory(File analysisDirectory) {
        this.analysisDirectory = analysisDirectory;
    }

    public String getFeatureQualifiedNameSeparator() {
        return featureQualifiedNameSeparator;
    }

    public void setFeatureQualifiedNameSeparator(String featureQualifiedNameSeparator) {
        this.featureQualifiedNameSeparator = featureQualifiedNameSeparator;
    }

    private String featureQualifiedNameSeparator;

    public String getMultiFileMappingSeperator() {
        return multiFileMappingSeperator;
    }

    public void setMultiFileMappingSeperator(String multiFileMappingSeperator) {
        this.multiFileMappingSeperator = multiFileMappingSeperator;
    }

    private String multiFileMappingSeperator;

    public String getTextEncoding() {
        return textEncoding;
    }

    public void setTextEncoding(String textEncoding) {
        this.textEncoding = textEncoding;
    }

    private String textEncoding;

    public String getFeatureModelFile() {
        return featureModelFile;
    }

    public void setFeatureModelFile(String featureModelFile) {
        this.featureModelFile = featureModelFile;
    }

    public String getFolderMappingFile() {
        return folderMappingFile;
    }

    public void setFolderMappingFile(String folderMappingFile) {
        this.folderMappingFile = folderMappingFile;
    }

    public String getFileMappingFile() {
        return fileMappingFile;
    }

    public void setFileMappingFile(String fileMappingFile) {
        this.fileMappingFile = fileMappingFile;
    }

    private String featureModelFile;
    private String folderMappingFile;
    private String fileMappingFile;
    private File projectRepository, analysisDirectory;

    public String getFragmentAnnotationBegin() {
        return fragmentAnnotationBegin;
    }

    public void setFragmentAnnotationBegin(String fragmentAnnotationBegin) {
        this.fragmentAnnotationBegin = fragmentAnnotationBegin;
    }

    public String getFragmentAnnotationEnd() {
        return fragmentAnnotationEnd;
    }

    public void setFragmentAnnotationEnd(String fragmentAnnotationEnd) {
        this.fragmentAnnotationEnd = fragmentAnnotationEnd;
    }

    public String getLineAnnotation() {
        return lineAnnotation;
    }

    public void setLineAnnotation(String lineAnnotation) {
        this.lineAnnotation = lineAnnotation;
    }

    private String fragmentAnnotationBegin;
    private String fragmentAnnotationEnd;
    private String lineAnnotation;

    public String getSimulationCommitAuthors() {
        return simulationCommitAuthors;
    }

    public void setSimulationCommitAuthors(String simulationCommitAuthors) {
        this.simulationCommitAuthors = simulationCommitAuthors;
    }

    private String simulationCommitAuthors;

    public String getAllowedFileExtensions() {
        return allowedFileExtensions;
    }

    public void setAllowedFileExtensions(String allowedFileExtensions) {
        this.allowedFileExtensions = allowedFileExtensions;
    }

    private String allowedFileExtensions;

    public String getSimulationCommitsGitLogCommand() {
        return simulationCommitsGitLogCommand;
    }

    public void setSimulationCommitsGitLogCommand(String simulationCommitsGitLogCommand) {
        this.simulationCommitsGitLogCommand = simulationCommitsGitLogCommand;
    }

    private String simulationCommitsGitLogCommand;

    public String getCommitCheckoutCommand() {
        return commitCheckoutCommand;
    }

    public void setCommitCheckoutCommand(String commitCheckoutCommand) {
        this.commitCheckoutCommand = commitCheckoutCommand;
    }

    private String commitCheckoutCommand;

    public String getMultiFeatureAnnotationSeparator() {
        return multiFeatureAnnotationSeparator;
    }

    public void setMultiFeatureAnnotationSeparator(String multiFeatureAnnotationSeparator) {
        this.multiFeatureAnnotationSeparator = multiFeatureAnnotationSeparator;
    }

    private String multiFeatureAnnotationSeparator;

    public List<String> getAnnotationFileNames() {
        List<String> files = new ArrayList<>();
        files.addAll(Arrays.asList(featureModelFile.split(",")));
        files.addAll(Arrays.asList(folderMappingFile.split(",")));
        files.addAll(Arrays.asList(fileMappingFile.split(",")));
        return files;
    }

    public AbstractionLevel getCodeAbstractionLevel() {
        return codeAbstractionLevel;
    }

    public void setCodeAbstractionLevel(AbstractionLevel codeAbstractionLevel) {
        this.codeAbstractionLevel = codeAbstractionLevel;
    }

    public void setCodeAbstractionLevel(String codeAbstractionLevel) {
        if (codeAbstractionLevel.equalsIgnoreCase("LOC")) {

            this.codeAbstractionLevel = AbstractionLevel.LOC;
        } else if (codeAbstractionLevel.equalsIgnoreCase("FRAGMENT")) {
            this.codeAbstractionLevel = AbstractionLevel.FRAGMENT;
        } else if (codeAbstractionLevel.equalsIgnoreCase("FILE")) {
            this.codeAbstractionLevel = AbstractionLevel.FILE;
        } else if (codeAbstractionLevel.equalsIgnoreCase("FOLDER")) {
            this.codeAbstractionLevel = AbstractionLevel.FOLDER;
        }

    }

    private AbstractionLevel codeAbstractionLevel;
    private String callGraphNonLabeledNodeName;

    public String getCallGraphNonLabeledNodeName() {
        return callGraphNonLabeledNodeName;
    }

    public List<String> getCallGraphNonLabeledNodeNames() {
        return Arrays.asList(callGraphNonLabeledNodeName.split(","));
    }

    public void setCallGraphNonLabeledNodeName(String callGraphNonLabeledNodeName) {
        this.callGraphNonLabeledNodeName = callGraphNonLabeledNodeName;
    }

    private String callGraphNonLabeledLineName;

    public String getCallGraphNonLabeledLineName() {
        return callGraphNonLabeledLineName;
    }

    public List<String> getCallGraphNonLabeledLineNames() {
        return Arrays.asList(callGraphNonLabeledLineName.split(","));
    }

    public void setCallGraphNonLabeledLineName(String callGraphNonLabeledLineName) {
        this.callGraphNonLabeledLineName = callGraphNonLabeledLineName;
    }

    private ProjectLanguage projectLanguage;

    public ProjectLanguage getProjectLanguage() {
        return projectLanguage;
    }

    public void setProjectLanguage(String projectLanguage) {
        if (projectLanguage.equalsIgnoreCase(".js")) {
            this.projectLanguage = ProjectLanguage.JavaScript;
        } else if (projectLanguage.equalsIgnoreCase(".java")) {
            this.projectLanguage = ProjectLanguage.Java;
        } else if (projectLanguage.equalsIgnoreCase(".c") || projectLanguage.equalsIgnoreCase(".cpp")) {
            this.projectLanguage = ProjectLanguage.CCPP;
        }
    }

    public boolean isUseGraphEditDistanceMetric() {
        return useGraphEditDistanceMetric;
    }

    public void setUseGraphEditDistanceMetric(boolean useGraphEditDistanceMetric) {
        this.useGraphEditDistanceMetric = useGraphEditDistanceMetric;
    }

    public void setUseGraphEditDistanceMetric(String useGraphEditDistanceMetric) {
        this.useGraphEditDistanceMetric = Boolean.parseBoolean(useGraphEditDistanceMetric.trim());
    }

    private boolean useGraphEditDistanceMetric;

    private boolean useFullFeatureNamesInMLDataFile;

    public boolean isUseFullFeatureNamesInMLDataFile() {
        return useFullFeatureNamesInMLDataFile;
    }

    public void setUseFullFeatureNamesInMLDataFile(String useFullFeatureNamesInMLDataFile) {
        this.useFullFeatureNamesInMLDataFile = Boolean.parseBoolean(useFullFeatureNamesInMLDataFile.trim());
    }

    public void setUseFullFeatureNamesInMLDataFile(boolean useFullFeatureNamesInMLDataFile) {
        this.useFullFeatureNamesInMLDataFile = useFullFeatureNamesInMLDataFile;
    }

    private String experimentDataFolder, executionMethod;

    public String getExperimentDataFolder() {
        return experimentDataFolder;
    }

    public void setExperimentDataFolder(String experimentDataFolder) {
        this.experimentDataFolder = experimentDataFolder;
    }

    public String getExecutionMethod() {
        return executionMethod;
    }

    public void setExecutionMethod(String executionMethod) {
        this.executionMethod = executionMethod;
    }

    private MLMethod mlMethod;

    public MLMethod getMlMethod() {
        return mlMethod;
    }

    public void setMlMethod(MLMethod mlMethod) {
        this.mlMethod = mlMethod;
    }

    public void setMlMethod(String mlMethod) {
        this.mlMethod = getMLMethodFromString(mlMethod);
    }

    private MLMethod getMLMethodFromString(String mlMethod) {
        Optional<MLMethod> method = Arrays.stream(MLMethod.values()).filter(m -> m.toString().equalsIgnoreCase(mlMethod)).findFirst();
        return method.isPresent() ? method.get() : MLMethod.MULAN;
    }

    private EvaluationMethod evaluationMethod;

    public EvaluationMethod getEvaluationMethod() {
        return evaluationMethod;
    }

    public void setEvaluationMethod(EvaluationMethod evaluationMethod) {
        this.evaluationMethod = evaluationMethod;
    }

    public void setEvaluationMethod(String evaluationMethod) {
        this.evaluationMethod = getEvaluationMethodFromString(evaluationMethod);
    }

    private EvaluationMethod getEvaluationMethodFromString(String evaluationMethod) {
        Optional<EvaluationMethod> method = Arrays.stream(EvaluationMethod.values()).filter(m -> m.toString().equalsIgnoreCase(evaluationMethod)).findFirst();
        return method.isPresent() ? method.get() : EvaluationMethod.CV;
    }

    private int kFolds;

    public int getChainIterations() {
        return chainIterations;
    }

    public void setChainIterations(int chainIterations) {
        this.chainIterations = chainIterations;
    }

    public void setChainIterations(String chainIterations) {
        this.chainIterations = Integer.parseInt(chainIterations);
    }

    private int chainIterations;

    public int getkFolds() {
        return kFolds;
    }

    public void setkFolds(int kFolds) {
        this.kFolds = kFolds;
    }

    public void setkFolds(String kFolds) {
        this.kFolds = Integer.parseInt(kFolds);
    }

    private File[] clonedRepositories;

    public File[] getClonedRepositories() {
        return clonedRepositories;
    }

    public void setClonedRepositories(File[] clonedRepositories) {
        this.clonedRepositories = clonedRepositories;
    }

    public void setClonedRepositories(String clonedRepositories) {
        String[] fileNames = clonedRepositories.split(",");
        List<File> clones = new ArrayList<>();
        Arrays.stream(fileNames).filter(f -> !StringUtils.isBlank(f)).forEach(fileName -> clones.add(new File(fileName)));
        this.clonedRepositories = clones.toArray(new File[clones.size()]);
    }

    private List<File> copiedGitRepositories;

    public List<File> getCopiedGitRepositories() {
        return copiedGitRepositories;
    }

    public void setCopiedGitRepositories(List<File> copiedGitRepositories) {
        this.copiedGitRepositories = copiedGitRepositories;
    }

    private ProjectType projectType;

    public ProjectType getProjectType() {
        return projectType;
    }

    public void setProjectType(ProjectType projectType) {
        this.projectType = projectType;
    }

    public void setProjectType(String projectType) {
        this.projectType = projectType.equalsIgnoreCase("SIMULATION") ? ProjectType.SIMULATION : ProjectType.REGULAR;
    }

    private UnlabledAssetComparison unlabledAssetComparision;

    public UnlabledAssetComparison getUnlabledAssetComparision() {
        return unlabledAssetComparision;
    }

    public void setUnlabledAssetComparision(UnlabledAssetComparison unlabledAssetComparision) {
        this.unlabledAssetComparision = unlabledAssetComparision;
    }
    public void setUnlabledAssetComparision(String unlabledAssetComparision) {
        if(unlabledAssetComparision.equalsIgnoreCase("self")){
            this.unlabledAssetComparision = UnlabledAssetComparison.SELF;
        }else if(unlabledAssetComparision.equalsIgnoreCase("knn")){
            this.unlabledAssetComparision = UnlabledAssetComparison.KNN;
        }else {
            this.unlabledAssetComparision = UnlabledAssetComparison.ALLNEIGHBORS;
        }

    }

    private int unlabledAssetComparisionKNN;

    public int getUnlabledAssetComparisionKNN() {
        return unlabledAssetComparisionKNN;
    }

    public void setUnlabledAssetComparisionKNN(int unlabledAssetComparisionKNN) {
        this.unlabledAssetComparisionKNN = unlabledAssetComparisionKNN;
    }
    public void setUnlabledAssetComparisionKNN(String unlabledAssetComparisionKNN) {
        this.unlabledAssetComparisionKNN = Integer.parseInt(unlabledAssetComparisionKNN);
    }

    private List<String> evaluationModels;

    public List<String> getEvaluationModels() {
        return evaluationModels;
    }

    public void setEvaluationModels(List<String> evaluationModels) {
        this.evaluationModels = evaluationModels;
    }
    public void setEvaluationModels(String evaluationModels) {
        this.evaluationModels = new ArrayList<>(Arrays.asList(evaluationModels.split(",")));
    }
    private List<String> predictionModels;

    public List<String> getPredictionModels() {
        return predictionModels;
    }

    public void setPredictionModels(List<String> predictionModels) {
        this.predictionModels = predictionModels;
    }
    public void setPredictionModels(String predictionModels) {
        this.predictionModels = new ArrayList<>(Arrays.asList(predictionModels.split(",")));
    }

    private List<String> finalPredictionModels;

    public List<String> getFinalPredictionModels() {
        return finalPredictionModels;
    }

    public void setFinalPredictionModels(List<String> finalPredictionModels) {
        this.finalPredictionModels = finalPredictionModels;
    }
    public void setFinalPredictionModels(String finalPredictionModels) {
        this.finalPredictionModels =  new ArrayList<>(Arrays.asList(finalPredictionModels.split(",")));
    }

    private boolean rankRelevantLabelsOnly;

    public boolean isRankRelevantLabelsOnly() {
        return rankRelevantLabelsOnly;
    }

    public void setRankRelevantLabelsOnly(boolean rankRelevantLabelsOnly) {
        this.rankRelevantLabelsOnly = rankRelevantLabelsOnly;
    }
    public void setRankRelevantLabelsOnly(String rankRelevantLabelsOnly) {
        this.rankRelevantLabelsOnly = Boolean.parseBoolean(rankRelevantLabelsOnly.trim());
    }

    private int fragmentClusterThreshold;

    public int getFragmentClusterThreshold() {
        return fragmentClusterThreshold;
    }

    public void setFragmentClusterThreshold(int fragmentClusterThreshold) {
        this.fragmentClusterThreshold = fragmentClusterThreshold;
    }
    public void setFragmentClusterThreshold(String fragmentClusterThreshold) {
        this.fragmentClusterThreshold = Integer.parseInt(fragmentClusterThreshold);
    }

    private boolean includeScenarioCalculation;

    public boolean isIncludeScenarioCalculation() {
        return includeScenarioCalculation;
    }

    public void setIncludeScenarioCalculation(boolean includeScenarioCalculation) {
        this.includeScenarioCalculation = includeScenarioCalculation;
    }
    public void setIncludeScenarioCalculation(String includeScenarioCalculation) {
        this.includeScenarioCalculation = Boolean.parseBoolean(includeScenarioCalculation.trim());
    }

    private int mLSMOTELabelCombination;

    public int getmLSMOTELabelCombination() {
        return mLSMOTELabelCombination;
    }

    public void setmLSMOTELabelCombination(int mLSMOTELabelCombination) {
        this.mLSMOTELabelCombination = mLSMOTELabelCombination;
    }
    public void setmLSMOTELabelCombination(String mLSMOTELabelCombination) {
        this.mLSMOTELabelCombination = Integer.parseInt(mLSMOTELabelCombination);
    }

    private boolean useDataBalancer;

    public boolean isUseDataBalancer() {
        return useDataBalancer;
    }

    public void setUseDataBalancer(boolean useDataBalancer) {
        this.useDataBalancer = useDataBalancer;
    }

    public void setUseDataBalancer(String useDataBalancer) {
        this.useDataBalancer = Boolean.parseBoolean(useDataBalancer.trim());
    }

    public boolean isUseHierachicalLabeling() {
        return useHierachicalLabeling;
    }

    public void setUseHierachicalLabeling(boolean useHierachicalLabeling) {
        this.useHierachicalLabeling = useHierachicalLabeling;
    }
    public void setUseHierachicalLabeling(String useHierachicalLabeling) {
        this.useHierachicalLabeling = Boolean.parseBoolean(useHierachicalLabeling.trim());
    }
    private boolean useHierachicalLabeling;

    private boolean useSparseInstance;

    public boolean isUseSparseInstance() {
        return useSparseInstance;
    }

    public void setUseSparseInstance(boolean useSparseInstance) {
        this.useSparseInstance = useSparseInstance;
    }
    public void setUseSparseInstance(String useSparseInstance) {
        this.useSparseInstance = Boolean.parseBoolean(useSparseInstance.trim());
    }

    private boolean generateDatasetStatistics,runExperiment;

    public boolean isGenerateDatasetStatistics() {
        return generateDatasetStatistics;
    }

    public void setGenerateDatasetStatistics(boolean generateDatasetStatistics) {
        this.generateDatasetStatistics = generateDatasetStatistics;
    }
    public void setGenerateDatasetStatistics(String generateDatasetStatistics) {
        this.generateDatasetStatistics = Boolean.parseBoolean(generateDatasetStatistics.trim());
    }
    public boolean isRunExperiment() {
        return runExperiment;
    }

    public void setRunExperiment(boolean runExperiment) {
        this.runExperiment = runExperiment;
    }
    public void setRunExperiment(String runExperiment) {
        this.runExperiment = Boolean.parseBoolean(runExperiment.trim());
    }

    private ProjectAnnotationType projectAnnotationType;

    public ProjectAnnotationType getProjectAnnotationType() {
        return projectAnnotationType;
    }

    public void setProjectAnnotationType(ProjectAnnotationType projectAnnotationType) {
        this.projectAnnotationType = projectAnnotationType;
    }
    public void setProjectAnnotationType(String projectAnnotationType) {
        this.projectAnnotationType = projectAnnotationType.equalsIgnoreCase("EA")?ProjectAnnotationType.EMBEDDED_ANNOTATION:ProjectAnnotationType.PREPROCESSOR;
    }
    private  boolean runCrossValidation;

    public boolean isRunCrossValidation() {
        return runCrossValidation;
    }

    public void setRunCrossValidation(boolean runCrossValidation) {
        this.runCrossValidation = runCrossValidation;
    }
    public void setRunCrossValidation(String runCrossValidation) {
        this.runCrossValidation = this.runCrossValidation = Boolean.parseBoolean(runCrossValidation.trim());
    }

    private boolean orderCommitsByCloneOrder;

    public boolean isOrderCommitsByCloneOrder() {
        return orderCommitsByCloneOrder;
    }

    public void setOrderCommitsByCloneOrder(boolean orderCommitsByCloneOrder) {
        this.orderCommitsByCloneOrder = orderCommitsByCloneOrder;
    }
    public void setOrderCommitsByCloneOrder(String orderCommitsByCloneOrder) {
        this.orderCommitsByCloneOrder = Boolean.parseBoolean(orderCommitsByCloneOrder.trim());
    }

    public Map<String, String> getProjectShortNameMap() {
        return projectShortNameMap;
    }

    public void setProjectShortNameMap(String projectShorNameList) {
        String[] shortNames = projectShorNameList.split(",");
        List<File> projects = getCopiedGitRepositories();
        if(projectShortNameMap==null){
            projectShortNameMap=new HashMap<>();
        }
        for(int i=0;i<shortNames.length;i++){
            projectShortNameMap.put(projects.get(i).getAbsolutePath(),shortNames[i]);
        }

    }

    private Map<String,String> projectShortNameMap;
private boolean runPredictions;

    public boolean isRunPredictions() {
        return runPredictions;
    }

    public void setRunPredictions(boolean runPredictions) {
        this.runPredictions = runPredictions;
    }
    public void setRunPredictions(String runPredictions) {
        this.runPredictions = Boolean.parseBoolean(runPredictions.trim());
    }

    public boolean isRunRDataPlots() {
        return runRDataPlots;
    }

    public void setRunRDataPlots(boolean runRDataPlots) {
        this.runRDataPlots = runRDataPlots;
    }
    public void setRunRDataPlots(String runRDataPlots) {
        this.runRDataPlots = Boolean.parseBoolean(runRDataPlots.trim());
    }
    private boolean runRDataPlots;

    public File getrDataFolder() {
        return rDataFolder;
    }

    public void setrDataFolder(File rDataFolder) {
        this.rDataFolder = rDataFolder;
    }
    public void setrDataFolder(String rDataFolder) {
        this.rDataFolder = new File(rDataFolder);
    }
    private File rDataFolder;

    private  String EAGetFeatureRegex;

    public String getEAGetFeatureRegex() {
        return EAGetFeatureRegex;
    }

    public void setEAGetFeatureRegex(String EAGetFeatureRegex) {
        this.EAGetFeatureRegex = EAGetFeatureRegex;
    }

    public String getIfDefGetFeatureRegex() {
        return IfDefGetFeatureRegex;
    }

    public void setIfDefGetFeatureRegex(String ifDefGetFeatureRegex) {
        IfDefGetFeatureRegex = ifDefGetFeatureRegex;
    }

    private String IfDefGetFeatureRegex;
    private String ifDefBegin,ifDefEnd;;

    public String getIfDefBegin() {
        return ifDefBegin;
    }

    public void setIfDefBegin(String ifDefBegin) {
        this.ifDefBegin = ifDefBegin;
    }

    public String getIfDefEnd() {
        return ifDefEnd;
    }

    public void setIfDefEnd(String ifDefEnd) {
        this.ifDefEnd = ifDefEnd;
    }

    private String textReplacementsFromFeatures;

    public String[] getTextReplacementsFromFeatures() {
        return textReplacementsFromFeatures.split(Pattern.quote(","));
    }

    public void setTextReplacementsFromFeatures(String textReplacementsFromFeatures) {
        this.textReplacementsFromFeatures = textReplacementsFromFeatures;
    }

    private  String singleLineComment;

    public String getSingleLineComment() {
        return singleLineComment;
    }

    public void setSingleLineComment(String singleLineComment) {
        this.singleLineComment = singleLineComment;
    }

    public String getUnFitFeatureStartString() {
        return unFitFeatureStartString;
    }

    public void setUnFitFeatureStartString(String unFitFeatureStartString) {
        this.unFitFeatureStartString = unFitFeatureStartString;
    }

    private String unFitFeatureStartString;
    private boolean printDetailedPredictionResults;

    public boolean isPrintDetailedPredictionResults() {
        return printDetailedPredictionResults;
    }

    public void setPrintDetailedPredictionResults(boolean printDetailedPredictionResults) {
        this.printDetailedPredictionResults = printDetailedPredictionResults;
    }
    public void setPrintDetailedPredictionResults(String printDetailedPredictionResults) {
        this.printDetailedPredictionResults = Boolean.parseBoolean(printDetailedPredictionResults.trim());
    }
    private FragmentClusteringMethod fragmentClusteringMethod;

    public FragmentClusteringMethod getFragmentClusteringMethod() {
        return fragmentClusteringMethod;
    }

    public void setFragmentClusteringMethod(String fragmentClusteringMethod) {

        this.fragmentClusteringMethod = fragmentClusteringMethod.equalsIgnoreCase("DIFF")?FragmentClusteringMethod.DIFF:FragmentClusteringMethod.THRESHOLD;
    }

    private boolean normaliseData;

    public boolean isNormaliseData() {
        return normaliseData;
    }

    public void setNormaliseData(String normaliseData) {
        this.normaliseData =  Boolean.parseBoolean(normaliseData.trim());
    }

    public boolean isPerformFeatureSelection() {
        return performFeatureSelection;
    }

    public void setPerformFeatureSelection(String performFeatureSelection) {
        this.performFeatureSelection = Boolean.parseBoolean(performFeatureSelection.trim());
    }

    private boolean performFeatureSelection;

    private List<String> attributesToNormalize;

    public List<String> getAttributesToNormalize() {
        return attributesToNormalize;
    }

    public void setAttributesToNormalize(String attributesToNormalize) {
        this.attributesToNormalize = Arrays.asList(attributesToNormalize.split(","));
    }
    private NormalisationStrategy normalisationStrategy;

    public NormalisationStrategy getNormalisationStrategy() {
        return normalisationStrategy;
    }

    public void setNormalisationStrategy(String normalisationStrategy) {
        this.normalisationStrategy = normalisationStrategy.equalsIgnoreCase("FTR")?NormalisationStrategy.FTR:NormalisationStrategy.WEKA;
    }
    private FeatureSelectionRank featureSelectionRank;

    public FeatureSelectionRank getFeatureSelectionRank() {
        return featureSelectionRank;
    }

    public void setFeatureSelectionRank(String featureSelectionRank) {
        featureSelectionRank = featureSelectionRank.trim();
        if(featureSelectionRank.equalsIgnoreCase("IG")){
            this.featureSelectionRank = FeatureSelectionRank.IG_rank;
        }else if(featureSelectionRank.equalsIgnoreCase("IG_BR")||featureSelectionRank.equalsIgnoreCase("IG-BR")){
            this.featureSelectionRank = FeatureSelectionRank.IG_BR_rank;
        }else if(featureSelectionRank.equalsIgnoreCase("IG_LP")||featureSelectionRank.equalsIgnoreCase("IG-LP")){
            this.featureSelectionRank = FeatureSelectionRank.IG_LP_rank;
        }else if(featureSelectionRank.equalsIgnoreCase("RF")){
            this.featureSelectionRank = FeatureSelectionRank.RF_rank;
        }else if(featureSelectionRank.equalsIgnoreCase("RF_BR")||featureSelectionRank.equalsIgnoreCase("RF-BR")){
            this.featureSelectionRank = FeatureSelectionRank.RF_BR_rank;
        }else if(featureSelectionRank.equalsIgnoreCase("RF_LP")||featureSelectionRank.equalsIgnoreCase("RF-LP")){
            this.featureSelectionRank = FeatureSelectionRank.RF_LP_rank;
        }else {
            this.featureSelectionRank= FeatureSelectionRank.overall_rank;
        }

    }
    private TopNFeaturesMethod topNFeaturesMethod;

    public TopNFeaturesMethod getTopNFeaturesMethod() {
        return topNFeaturesMethod;
    }

    public void setTopNFeaturesMethod(String topNFeaturesMethod) {
        topNFeaturesMethod = topNFeaturesMethod.trim();
        if(topNFeaturesMethod.equalsIgnoreCase("T")){
            this.topNFeaturesMethod = TopNFeaturesMethod.Threshold;
        }else if (topNFeaturesMethod.equalsIgnoreCase("P")){
            this.topNFeaturesMethod = TopNFeaturesMethod.Percent;
        }
        else {
            this.topNFeaturesMethod = TopNFeaturesMethod.KBest;
        }

    }

    public double getTopNFeatures() {
        return topNFeatures;
    }

    public void setTopNFeatures(String topNFeatures) {
        this.topNFeatures = Double.parseDouble(topNFeatures.trim());
    }

    private double topNFeatures;

    public boolean isCopyExpResultsToRFolder() {
        return copyExpResultsToRFolder;
    }

    public void setCopyExpResultsToRFolder(String copyExpResultsToRFolder) {
        this.copyExpResultsToRFolder = Boolean.parseBoolean(copyExpResultsToRFolder.trim());
    }

    private boolean copyExpResultsToRFolder;

    public String[] getFeatureSelectionMethods() {
        return featureSelectionMethods;
    }

    public void setFeatureSelectionMethods(String featureSelectionMethods) {
        this.featureSelectionMethods = featureSelectionMethods.split(",");
    }

    private String[] featureSelectionMethods;

    public boolean isPersistProjectData() {
        return persistProjectData;
    }

    public void setPersistProjectData(String persistProjectData) {
        this.persistProjectData = Boolean.parseBoolean(persistProjectData);
    }

    private boolean persistProjectData;
    public String getProjectShortName() {
        return getProjectShortNameMap().get(getProjectRepository().getAbsolutePath());
    }
    public String getBalancedDataSetName() {
        return isUseDataBalancer() ? "b" : "im";
    }
    public String getNormalizedDataSetName() {
        return isNormaliseData() ? "norm" : "reg";
    }
    public String getFragmentThresholdDataSetName() {
        return getFragmentClusteringMethod() == FragmentClusteringMethod.DIFF ? "diff" : String.format("%d",getFragmentClusterThreshold());
    }
    public String getProjectDataName(){
        return String.format("%s_%s_%s_%s_%s",getProjectShortName(),getCodeAbstractionLevel().toString().toLowerCase(),getBalancedDataSetName(),getNormalizedDataSetName(),getFragmentThresholdDataSetName());
    }
    public String getProjectDataFileName(){
        return String.format("%s/projectData/%s.data",getAnalysisDirectory(),getProjectDataName());
    }
    public String getProjectDataBackupFileName(){
        return String.format("%s/projectData/%s2.data",getAnalysisDirectory(),getProjectDataName());
    }

    public String getDataFilesDirectory() {
        return String.format("%s/%s/%s/dataFiles", getAnalysisDirectory(), getCodeAbstractionLevel(), getProjectRepository().getName());
    }



    public String getDataFilesSubDirectory() {
        return String.format("%s/%s/%s/dataFiles/%s_%s_%s", getAnalysisDirectory(), getCodeAbstractionLevel(), getProjectRepository().getName(),getBalancedDataSetName(),getNormalizedDataSetName(),getFragmentThresholdDataSetName());
    }


    public String getDataFilesImbalancedDirectory() {
        return String.format("%s/im_%s_%s", getDataFilesDirectory(), getNormalizedDataSetName(),getFragmentThresholdDataSetName());
    }

    public boolean isCopyRepositoriesToTargetAnalysisFolder() {
        return copyRepositoriesToTargetAnalysisFolder;
    }

    public void setCopyRepositoriesToTargetAnalysisFolder(String copyRepositoriesToTargetAnalysisFolder) {
        this.copyRepositoriesToTargetAnalysisFolder = Boolean.parseBoolean(copyRepositoriesToTargetAnalysisFolder.trim());
    }

    private boolean copyRepositoriesToTargetAnalysisFolder;

    public int getCommitsToExecute() {
        return commitsToExecute;
    }

    public void setCommitsToExecute(String commitsToExecute) {
        this.commitsToExecute = Integer.parseInt(commitsToExecute);
    }

    private int commitsToExecute;

    public boolean isPrintAssetMappings() {
        return printAssetMappings;
    }

    public void setPrintAssetMappings(String printAssetMappings) {
        this.printAssetMappings = Boolean.parseBoolean(printAssetMappings.trim());
    }

    private boolean printAssetMappings;

    public int getExperimentStartingCommit() {
        return experimentStartingCommit;
    }

    public void setExperimentStartingCommit(String experimentStartingCommit) {
        this.experimentStartingCommit = Integer.parseInt(experimentStartingCommit);
    }

    private int experimentStartingCommit;

    public boolean isRunDatabalancer() {
        return runDatabalancer;
    }

    public void setRunDatabalancer(String runDatabalancer) {
        this.runDatabalancer = Boolean.parseBoolean(runDatabalancer.trim());
    }

    private boolean runDatabalancer;

    public List<String> getSimulationCommitAuthorsList(){
        return Arrays.asList(getSimulationCommitAuthors().split(","));
    }

    public boolean isPrintProjectStats() {
        return printProjectStats;
    }

    public void setPrintProjectStats(String printProjectStats) {
        this.printProjectStats = Boolean.parseBoolean(printProjectStats.trim());
    }

    private boolean printProjectStats;
    private List<File> projectCopiedRepositoriesForStats;

    public List<File> getProjectCopiedRepositoriesForStats() {
        return projectCopiedRepositoriesForStats;
    }

    public void setProjectCopiedRepositoriesForStats(String projectCopiedRepositoriesForStats) {
        this.projectCopiedRepositoriesForStats = Arrays.asList(projectCopiedRepositoriesForStats.split(",")).stream().map(f->new File(f)).collect(Collectors.toList());
    }

    public boolean isIgnoreIntegerOptions() {
        return ignoreIntegerOptions;
    }

    public void setIgnoreIntegerOptions(String ignoreIntegerOptions) {
        this.ignoreIntegerOptions = Boolean.parseBoolean(ignoreIntegerOptions.trim());
    }

    private boolean ignoreIntegerOptions;

    public boolean isUseOnlyAssetsWithKnownLabelsForExperiment() {
        return useOnlyAssetsWithKnownLabelsForExperiment;
    }

    public void setUseOnlyAssetsWithKnownLabelsForExperiment(String useOnlyAssetsWithKnownLabelsForExperiment) {
        this.useOnlyAssetsWithKnownLabelsForExperiment = Boolean.parseBoolean(useOnlyAssetsWithKnownLabelsForExperiment.trim());
    }

    private boolean useOnlyAssetsWithKnownLabelsForExperiment;

    public boolean isUseMaxForPredictionMetricAggregation() {
        return useMaxForPredictionMetricAggregation;
    }

    public void setUseMaxForPredictionMetricAggregation(String useMaxForPredictionMetricAggregation) {
        this.useMaxForPredictionMetricAggregation = Boolean.parseBoolean(useMaxForPredictionMetricAggregation.trim());
    }

    private boolean useMaxForPredictionMetricAggregation;

    public int getPerformFeatureSelectionForEveryKDataset() {
        return performFeatureSelectionForEveryKDataset;
    }

    public void setPerformFeatureSelectionForEveryKDataset(String performFeatureSelectionForEveryKDataset) {
        this.performFeatureSelectionForEveryKDataset = Integer.parseInt(performFeatureSelectionForEveryKDataset.trim());
    }

    private int performFeatureSelectionForEveryKDataset;
    private boolean calculateMetrics,useMetricCommitsFromCSV;

    public boolean isCalculateMetrics() {
        return calculateMetrics;
    }

    public void setCalculateMetrics(String calculateMetrics) {
        this.calculateMetrics = Boolean.parseBoolean(calculateMetrics);
    }

    public boolean isUseMetricCommitsFromCSV() {
        return useMetricCommitsFromCSV;
    }

    public void setUseMetricCommitsFromCSV(String useMetricCommitsFromCSV) {
        this.useMetricCommitsFromCSV = Boolean.parseBoolean(useMetricCommitsFromCSV);
    }

    public List<File> getCommitCSVs() {
        return commitCSVs;
    }

    public void setCommitCSVs(String commitCSVs) {

        this.commitCSVs = Arrays.asList(commitCSVs.split(",")).stream().map(f->new File(f)).collect(Collectors.toList());
    }

    public List<File> getCommitCSVFilesCommitBasedPrediction() {
        return commitCSVFilesCommitBasedPrediction;
    }

    public void setCommitCSVFilesCommitBasedPrediction(String files) {
        this.commitCSVFilesCommitBasedPrediction = Arrays.asList(files.split(",")).stream().map(f->new File(f)).collect(Collectors.toList());;
    }

    private List<File> commitCSVFilesCommitBasedPrediction;
    public List<File> getCommitsForFeatRacerExperiment() {
        return commitsForFeatRacerExperiment;
    }

    public void setCommitsForFeatRacerExperiment(String commitsForFeatRacerExperiment) {

        this.commitsForFeatRacerExperiment = Arrays.asList(commitsForFeatRacerExperiment.split(",")).stream().map(f->new File(f)).collect(Collectors.toList());
    }
    private List<File> commitCSVs,commitsForFeatRacerExperiment;

    public boolean isGenerateARRFFiles() {
        return generateARRFFiles;
    }

    public void setGenerateARRFFiles(String generateARRFFiles) {
        this.generateARRFFiles = Boolean.parseBoolean(generateARRFFiles);
    }

    private boolean generateARRFFiles;

    public String getMultipleFeatureRegex() {
        return multipleFeatureRegex;
    }

    public void setMultipleFeatureRegex(String multipleFeatureRegex) {
        this.multipleFeatureRegex = multipleFeatureRegex;
    }

    private String multipleFeatureRegex;

    public String getMetricCalculationBasis() {
        return metricCalculationBasis;
    }

    public void setMetricCalculationBasis(String metricCalculationBasis) {
        this.metricCalculationBasis = metricCalculationBasis;
    }

    private String metricCalculationBasis;

    public boolean isPrintMetricDetails() {
        return printMetricDetails;
    }

    public void setPrintMetricDetails(String printMetricDetails) {
        this.printMetricDetails = Boolean.parseBoolean(printMetricDetails);
    }

    private boolean printMetricDetails;

    public boolean isGetAnnotationsWithoutCommits() {
        return getAnnotationsWithoutCommits;
    }

    public void setGetAnnotationsWithoutCommits(String getAnnotationsWithoutCommits) {
        this.getAnnotationsWithoutCommits = Boolean.parseBoolean(getAnnotationsWithoutCommits);
    }

    private boolean getAnnotationsWithoutCommits;

    public boolean isAnnotatedLineIsImmediatelyAfterLineAnnotation() {
        return annotatedLineIsImmediatelyAfterLineAnnotation;
    }

    public void setAnnotatedLineIsImmediatelyAfterLineAnnotation(String annotatedLineIsImmediatelyAfterLineAnnotation) {
        this.annotatedLineIsImmediatelyAfterLineAnnotation = Boolean.parseBoolean(annotatedLineIsImmediatelyAfterLineAnnotation);
    }

    private boolean annotatedLineIsImmediatelyAfterLineAnnotation;

    public String getAllAnnotationPatterns() {
        return allAnnotationPatterns;
    }

    public void setAllAnnotationPatterns(String allAnnotationPatterns) {
        this.allAnnotationPatterns = allAnnotationPatterns;
    }

    private String allAnnotationPatterns;

    public String getCamelCaseSplitRegex() {
        return camelCaseSplitRegex;
    }

    public void setCamelCaseSplitRegex(String camelCaseSplitRegex) {
        this.camelCaseSplitRegex = camelCaseSplitRegex;
    }

    private String camelCaseSplitRegex;

    public boolean isRunWithRepoDriller() {
        return runWithRepoDriller;
    }

    public void setRunWithRepoDriller(String runWithRepoDriller) {
        this.runWithRepoDriller = Boolean.parseBoolean(runWithRepoDriller.trim());
    }

    private boolean runWithRepoDriller;

    public int getNumberOfThreads() {
        return numberOfThreads;
    }

    public void setNumberOfThreads(String numberOfThreads) {
        this.numberOfThreads = Integer.parseInt(numberOfThreads.trim());
    }

    private  int numberOfThreads;

    public boolean isPerformFeatureCounts() {
        return performFeatureCounts;
    }

    public void setPerformFeatureCounts(String performFeatureCounts) {
        this.performFeatureCounts = Boolean.parseBoolean(performFeatureCounts.trim());
    }

    private boolean performFeatureCounts;

    public String getTrainDataFile() {
        return trainDataFile;
    }

    public void setTrainDataFile(String trainDataFile) {
        this.trainDataFile = trainDataFile;
    }

    public String getTestDataFile() {
        return testDataFile;
    }

    public void setTestDataFile(String testDataFile) {
        this.testDataFile = testDataFile;
    }

    private String trainDataFile;
    private String testDataFile;

    public String getFeatureFileMapping() {
        return featureFileMapping;
    }

    public void setFeatureFileMapping(String featureFileMapping) {
        this.featureFileMapping = featureFileMapping;
    }

    private String featureFileMapping;
    private String labeledFeaturesPath;

    public String getLabeledFeaturesPath() {
        return labeledFeaturesPath;
    }

    public void setLabeledFeaturesPath(String labeledFeaturesPath) {
        this.labeledFeaturesPath = labeledFeaturesPath;
    }

    public String getLabeledFilesPath() {
        return labeledFilesPath;
    }

    public void setLabeledFilesPath(String labeledFilesPath) {
        this.labeledFilesPath = labeledFilesPath;
    }

    private String labeledFilesPath;

    public String getDefectRunMode() {
        return defectRunMode;
    }

    public void setDefectRunMode(String defectRunMode) {
        this.defectRunMode = defectRunMode;
    }

    private String defectRunMode;

    public boolean isMetricCalculationCommitBased() {
        return metricCalculationCommitBased;
    }

    public void setMetricCalculationCommitBased(String metricCalculationCommitBased) {
        this.metricCalculationCommitBased = Boolean.parseBoolean(metricCalculationCommitBased);
    }

    private boolean metricCalculationCommitBased;

    public String getDataBaseConnectionString() {
        return dataBaseConnectionString;
    }

    public void setDataBaseConnectionString(String dataBaseConnectionString) {
        this.dataBaseConnectionString = dataBaseConnectionString;
    }

    private String dataBaseConnectionString;

    public boolean isSaveDataInDataBase() {
        return saveDataInDataBase;
    }

    public void setSaveDataInDataBase(String saveDataInDataBase) {
        this.saveDataInDataBase = Boolean.parseBoolean(saveDataInDataBase);
    }

    private boolean saveDataInDataBase;

    public String getCommitDataSetParentFolder() {
        return commitDataSetParentFolder;
    }

    public void setCommitDataSetParentFolder(String commitDataSetParentFolder) {
        this.commitDataSetParentFolder = commitDataSetParentFolder;
    }

    public String getReleaseDataSetParentFolder() {
        return releaseDataSetParentFolder;
    }

    public void setReleaseDataSetParentFolder(String releaseDataSetParentFolder) {
        this.releaseDataSetParentFolder = releaseDataSetParentFolder;
    }

    private String commitDataSetParentFolder,releaseDataSetParentFolder;

    public String getCommitFilesParentFolder() {
        return commitFilesParentFolder;
    }

    public void setCommitFilesParentFolder(String commitFilesParentFolder) {
        this.commitFilesParentFolder = commitFilesParentFolder;
    }

    private String commitFilesParentFolder,projectShortNames;


    public String getProjectShortNames() {
        return projectShortNames;
    }

    public void setProjectShortNames(String projectShortNames) {
        this.projectShortNames = projectShortNames;
    }

    public String getCrossProjectCommitFolder() {
        return crossProjectCommitFolder;
    }

    public void setCrossProjectCommitFolder(String crossProjectCommitFolder) {
        this.crossProjectCommitFolder = crossProjectCommitFolder;
    }

    public String getCrossProjectReleaseFolder() {
        return crossProjectReleaseFolder;
    }

    public void setCrossProjectReleaseFolder(String crossProjectReleaseFolder) {
        this.crossProjectReleaseFolder = crossProjectReleaseFolder;
    }

    public String getProjectCombinationsFile() {
        return projectCombinationsFile;
    }

    public void setProjectCombinationsFile(String projectCombinationsFile) {
        this.projectCombinationsFile = projectCombinationsFile;
    }

    private String crossProjectCommitFolder,crossProjectReleaseFolder,projectCombinationsFile;

    private boolean generateLuceneData;

    public boolean isGenerateLuceneData() {
        return generateLuceneData;
    }

    public void setGenerateLuceneData(String generateLuceneData) {
        this.generateLuceneData = Boolean.parseBoolean(generateLuceneData);
    }

    private double luceneThreshold;

    public double getLuceneThreshold() {
        return luceneThreshold;
    }

    public void setLuceneThreshold(String luceneThreshold) {
        this.luceneThreshold = Double.parseDouble(luceneThreshold.trim());
    }

    public int getStartingCommitIndex() {
        return startingCommitIndex;
    }

    public void setStartingCommitIndex(String startingCommitIndex) {
        this.startingCommitIndex = Integer.parseInt(startingCommitIndex.trim());
    }

    private int startingCommitIndex;

    public boolean isTrainingDataIncludesUnMappedAssets() {
        return trainingDataIncludesUnMappedAssets;
    }

    public void setTrainingDataIncludesUnMappedAssets(String trainingDataIncludesUnMappedAssets) {
        this.trainingDataIncludesUnMappedAssets = Boolean.parseBoolean(trainingDataIncludesUnMappedAssets.trim());
    }

    private boolean trainingDataIncludesUnMappedAssets;
    public String[] getAssetTypes(){
        return new String[]{"FOLDER", "FILE", "FRAGMENT"};
    }
    public String[] getAssetLevels(){
        return new String[]{"folder", "file", "fragment"};
    }
    public String[] getProjectNamesList() {
        return projectNamesList;
    }

    public void setProjectNamesList(String projectNamesList) {
        this.projectNamesList = projectNamesList.split(",");
    }

    public String[] getProjectShortNameList() {
        return projectShortNameList;
    }

    public void setProjectShortNameList(String projectShortNameList) {
        this.projectShortNameList = projectShortNameList.split(",");
    }

    private String[] projectNamesList,projectShortNameList;

    public Map<String,String> getMappedProjectNames(){
        Map<String,String> projectNames = new HashMap<>();
        String[]namelist = getProjectShortNameList();
        String[]shortNames= getProjectShortNameList();
        for(int i=0;i<namelist.length;i++){
            projectNames.put(namelist[i],shortNames[i]);
        }

        return projectNames;
    }

    public List<String> getAssetTypesToPredict() {
        return assetTypesToPredict;
    }

    public void setAssetTypesToPredict(String assetTypesToPredict) {
        this.assetTypesToPredict = Arrays.stream(assetTypesToPredict.split(",")).collect(Collectors.toList());
    }

    private List<String> assetTypesToPredict;

    public String getClassifierForCombinedResults() {
        return classifierForCombinedResults;
    }

    public void setClassifierForCombinedResults(String classifierForCombinedResults) {
        this.classifierForCombinedResults = classifierForCombinedResults;
    }

    private String classifierForCombinedResults;

    public boolean isReadAllAssetsFromEachCommit() {
        return readAllAssetsFromEachCommit;
    }

    public void setReadAllAssetsFromEachCommit(String readAllAssetsFromEachCommit) {
        this.readAllAssetsFromEachCommit = Boolean.parseBoolean(readAllAssetsFromEachCommit.trim());
    }

    private boolean readAllAssetsFromEachCommit;
}
