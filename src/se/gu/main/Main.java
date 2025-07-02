package se.gu.main;

import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.io.FileUtils;
import org.repodriller.RepoDriller;
import se.gu.assets.FeatureCleaner;
import se.gu.defectpred.*;
import se.gu.git.ProjectDevCounter;
import se.gu.metrics.MetricCalculatorDB;
import se.gu.ml.experiment.*;
import se.gu.ml.preprocessing.DataGeneratorDB;
import se.gu.utils.Utilities;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

//import uk.ac.shef.wit.simmetrics.similaritymetrics.CosineSimilarity;

public class Main {
    public static void main(String[] args) {
        try {
            //Read properties file
            Properties properties = new Properties();
            InputStream inputStream = new FileInputStream("config.properties");
            properties.load(inputStream);

            //set directories
            final File analyisDirectory = Utilities.createOutputDirectory(properties.getProperty("AnalysisDirectory"), false);
            //set configuration
            Configuration configuration = Utilities.getConfiguration(properties, analyisDirectory);
            File[] clones = configuration.getClonedRepositories();

            if(configuration.getExecutionMethod().equalsIgnoreCase("DP")){
                //defect prediction
                InstanceClassifier instanceClassifier = new InstanceClassifier(configuration);
                if(configuration.getDefectRunMode().equalsIgnoreCase("CM")){
                    instanceClassifier.classifyInstances();
                    instanceClassifier.mapLabelsToFeaturesAndFiles();
                }else  if (configuration.getDefectRunMode().equalsIgnoreCase("C")) {
                    instanceClassifier.classifyInstances();
                }
                else  if (configuration.getDefectRunMode().equalsIgnoreCase("AE")) {
                    //instanceClassifier.evaluateClassifiers();
                    instanceClassifier.evaluateAttributes();
                }
                else  if (configuration.getDefectRunMode().equalsIgnoreCase("CC")) {
                    ChangeBasedPrediction changeBasedPrediction = new ChangeBasedPrediction(configuration);
                    //commit-based
                    changeBasedPrediction.createCommitLevelDataSets();
                    //releases
                    //changeBasedPrediction.createReleaseLevelDataSets();
                }
                else  if (configuration.getDefectRunMode().equalsIgnoreCase("XP")) {
                    CrossProjectPrediction crossProjectPrediction = new CrossProjectPrediction(configuration);
                    //crossProjectPrediction.createCommitLevelDataSets(true);
                    crossProjectPrediction.createCommitLevelDataSets(false);//releases

                }
                else if(configuration.getDefectRunMode().equalsIgnoreCase("LIBSVM")){
                    LIBSVMFileGenerator gen = new LIBSVMFileGenerator(configuration);
                    gen.createLIBSVMFromDefectPredictionCSVs();
                }
                else {
                    instanceClassifier.mapLabelsToFeaturesAndFiles();
                }
            }else {


                //create copies of the repos
                if (!configuration.isRunWithRepoDriller()) {
                    try (ProgressBar pb = new ProgressBar("Copying repositories:", clones.length)) {
                        for (File clonedRepository : clones) {
                            pb.step();
                            pb.setExtraMessage(clonedRepository.getName());
                            File gitRepository = new File(analyisDirectory.getAbsolutePath() + "/" + clonedRepository.getName());
                            if (configuration.isCopyRepositoriesToTargetAnalysisFolder() && !(configuration.isPrintProjectStats() || configuration.isRunRDataPlots())) {
                                FileUtils.copyDirectory(clonedRepository, gitRepository);
                            }
                            configuration.getCopiedGitRepositories().add(gitRepository);
                        }
                    }
                }
                if (configuration.isRunWithRepoDriller()) {
                    configuration.setCopiedGitRepositories(Arrays.asList(clones));
                }

                configuration.setProjectShortNameMap(properties.getProperty("ProjectShortNames"));//assign short names to project names

                //for each project, run:
                //run R or experiments
                RDataPlotRunner runner = new RDataPlotRunner(configuration);

                if (configuration.isRunRDataPlots()) {
                    runner.combineDatasetStats();
                    //runner.assignRowIDs();
                } else if (configuration.isPrintProjectStats()) {
                    ProjectDevCounter projectDevCounter = new ProjectDevCounter(configuration);
                    projectDevCounter.printProjectDevCounts();
                } else {
                    for (File repo : configuration.getCopiedGitRepositories()) {

                        configuration.setProjectRepository(repo);
                        ProjectData projectData = null;

                        String fileName = configuration.getProjectDataFileName();
                        File projectDataFile = new File(fileName);

                        if (projectDataFile.exists() && configuration.isPersistProjectData()) {

                            // Reading the object from a file
                            FileInputStream file = new FileInputStream(fileName);
                            ObjectInputStream in = new ObjectInputStream(file);
                            try {
                                // Method for deserialization of object
                                projectData = (ProjectData) in.readObject();
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                in.close();
                                file.close();
                            }


                        } else {
                            projectData = new ProjectData(configuration);
                        }

                        if (configuration.getExecutionMethod().equalsIgnoreCase("D")) {//Generate asset data and mappings
                            if(configuration.isRunWithRepoDriller()){
                                RepoDriller repoDriller = new RepoDriller();
                                repoDriller.start(new ProjectReaderWithDriller(projectData));
                            }else {
                                ProjectDBReader projectDBReader = new ProjectDBReader(projectData);
                                projectDBReader.readCommits();
                            }
                            //createData(configuration, projectData);
                        }else if (configuration.getExecutionMethod().equalsIgnoreCase("GM")) {//Generate metrics for assets e.g., commits, devs, nff, etc
                            MetricCalculatorDB metricCalculatorDB = new MetricCalculatorDB(projectData);
                            //metricCalculatorDB.calculateMetrics();
                            metricCalculatorDB.calculateMetricsALLASSETSLOADED();
                        }
                        else if (configuration.getExecutionMethod().equalsIgnoreCase("GDT")) {//Generate ARFF datasets
                            DataGeneratorDB dataGeneratorDB = new DataGeneratorDB(projectData);
                            dataGeneratorDB.createDataSets();
                        }else if (configuration.getExecutionMethod().equalsIgnoreCase("EDB")) {//Execute classifiers on ARFF datasets
                            ExperimentRunnerDB experimentRunnerDB = new ExperimentRunnerDB(projectData);
                            experimentRunnerDB.runExperiment();
                        }
                        else if (configuration.getExecutionMethod().equalsIgnoreCase("EDBTopN")) {//Execute classifiers on ARFF datasets with TopN metrics
                            ExperimentRunnerDB experimentRunnerDB = new ExperimentRunnerDB(projectData);
                            experimentRunnerDB.runExperimentForTopNFeatures();
                        }
                        else if (configuration.getExecutionMethod().equalsIgnoreCase("CM")){//Generate COmmit Metrics data e.g., lines added, deleted, hunk sizes, etc
                            //first run through all commits of each project
                            RepoDriller repoDriller = new RepoDriller();
                            repoDriller.start(new ProjectCommitMetricReader(projectData));
                        }
                        else if (configuration.getExecutionMethod().equalsIgnoreCase("CMU")){//Updated COmmit Metrics to include asset metrics such as ration of annotatedfolders, files, frgments and lines
                            MetricCalculatorDB metricCalculatorDB = new MetricCalculatorDB(projectData);
                            metricCalculatorDB.calculateAssetBasedCommitMetrics();
                        }
                        else if (configuration.getExecutionMethod().equalsIgnoreCase("RU")){//Updated prediction results from CSVs into DB
                            ResultsUpdater resultsUpdater= new ResultsUpdater(projectData.getConfiguration());
                            //resultsUpdater.updateResulsFromCSVs();
                            resultsUpdater.updatePSResulsFromCSVs();//update results into DB from CSVs
                            break;//do only once not for each projcect

                            //resultsUpdater.generateLatexResultsTable();
                        }
                        else if (configuration.getExecutionMethod().equalsIgnoreCase("FC")){//Clean features by removing whitespaces and special charaters
                            FeatureCleaner featureCleaner= new FeatureCleaner(projectData.getConfiguration());
                            featureCleaner.cleanFeatures();
                            break;
                        }
                        else if (configuration.getExecutionMethod().equalsIgnoreCase("GMCM")){//Generate CSVs with mapings for commit metrics and prediction results
                            ResultsUpdater resultsUpdater= new ResultsUpdater(projectData.getConfiguration());
                            resultsUpdater.generatePracticePredictionResultsMapping();
                            break;
                        }
                        else if (configuration.getExecutionMethod().equalsIgnoreCase("CROF")){//Combine results from multiple projects into one file for pivot analysis
                            ResultsUpdater resultsUpdater= new ResultsUpdater(projectData.getConfiguration());
                            resultsUpdater.createCombinedProjectResults();
                            resultsUpdater.combineProjectResultsIntoOneFile();
                            resultsUpdater.createRelativeCommits();
                            break;
                        }
                        else if (configuration.getExecutionMethod().equalsIgnoreCase("LTG")){//Report Generator
                            ResultsUpdater resultsUpdater= new ResultsUpdater(projectData.getConfiguration());
                            resultsUpdater.generateLatexResultsTable();
                            break;
                        }
                        else if (configuration.getExecutionMethod().equalsIgnoreCase("RG")){//Report Generator--do min, max, avg, number of assets per commits
                            ReportGenerator reportGenerator= new ReportGenerator(projectData.getConfiguration());
                            reportGenerator.generateAssetCountPerCommitPerProject();
                        }
                        else if (configuration.getExecutionMethod().equalsIgnoreCase("AWC")){//Read Annotations from the given git project without running through the revision history (all commits)
                            ProjectReader projectReader = new ProjectReader(projectData);
                            projectReader.createAnnotationsWithoutCommits();
                        }
                        else if (configuration.getExecutionMethod().equalsIgnoreCase("LDG")){//Lucene Data Generator--this is used to retrieve features using Lucene (LSI)
                            ProjectReader projectReader = new ProjectReader(projectData);
                            projectReader.createProjectData();
                        }
                        else if (configuration.getExecutionMethod().equalsIgnoreCase("CLD")){//Combine Lucene Data Generator
                           LuceneRunner luceneRunner = new LuceneRunner(projectData);
                           luceneRunner.combineMeasuresLuceneData();
                           luceneRunner.combineLuceneData();
                           break;
                        }
                        else if (configuration.getExecutionMethod().equalsIgnoreCase("DSSG")){//DataSet STats Geerator--get cardinality, labels per ARFF dataset
                            DatasetStatsGenerator datasetStatsGenerator  =new DatasetStatsGenerator(projectData);
                            datasetStatsGenerator.generateDatasetStats();
                            break;//run only once for all projects
                        }
                        else if (configuration.getExecutionMethod().equalsIgnoreCase("FS")){//Feature Selection
                            FeatureSelectionRunner featureSelectionRunner  =new FeatureSelectionRunner(projectData);
                            featureSelectionRunner.selectFeatures();
                            break;//run only once for all projects
                        }else if (configuration.getExecutionMethod().equalsIgnoreCase("CFS")){//Combine Feature selection summary data for all projects
                            FeatureSelectionRunner featureSelectionRunner  =new FeatureSelectionRunner(projectData);
                            featureSelectionRunner.combineSUmmaryFiles();
                            break;//run only once for all projects
                        }else if (configuration.getExecutionMethod().equalsIgnoreCase("CNTop")){//Combine nTop feature results files
                            ResultsUpdater resultsUpdater  =new ResultsUpdater(projectData.getConfiguration());
                            resultsUpdater.combineNTopMetricsFiles();
                            break;//run only once for all projects
                        }
                        else if (configuration.getExecutionMethod().equalsIgnoreCase("CMDP")){//Combine nTop feature results files
                            if(configuration.isRunWithRepoDriller()){
                                RepoDriller repoDriller = new RepoDriller();
                                repoDriller.start(new ProjectStructureMetricReader(projectData));
                            }else {
                                ProjectReader projectReader = new ProjectReader(projectData);
                                projectReader.createProjectData();
                            }

                        }
                        else if (configuration.getExecutionMethod().equalsIgnoreCase("GPCD")){//Group projects for clone detection
                            ProjectGrouper projectGrouper= new ProjectGrouper(configuration);
                            projectGrouper.groupProjects();
                            break;//do it only once for all projects

                        }
                        else if (configuration.getExecutionMethod().equalsIgnoreCase("E")) {//experiment only
                            runTheExperiment(projectData);
                        }
                    }
                }
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createData(Configuration configuration, ProjectData projectData) throws Exception {

        if(configuration.isRunWithRepoDriller()){
            RepoDriller repoDriller = new RepoDriller();
            repoDriller.start(new ProjectReaderWithDriller(projectData));
        }else {
            ProjectReader projectReader = new ProjectReader(projectData);
            ProjectDBReader projectDBReader = new ProjectDBReader(projectData);
            if(configuration.isGetAnnotationsWithoutCommits()){
                projectReader.createAnnotationsWithoutCommits();
            }else {
                //projectReader.createProjectData();
                projectDBReader.readCommits();
            }
        }


   }

    private static void runTheExperiment(ProjectData projectData) throws Exception {
        ExperimentRunner experimentRunner = new ExperimentRunner(projectData);
        experimentRunner.runExperiment();
    }


}
