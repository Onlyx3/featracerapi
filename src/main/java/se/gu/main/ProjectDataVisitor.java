package se.gu.main;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.repodriller.domain.Commit;
import org.repodriller.domain.Modification;
import org.repodriller.domain.ModificationType;
import org.repodriller.persistence.PersistenceMechanism;
import org.repodriller.scm.*;
import se.gu.assets.FeatureAssetMap;
import se.gu.ml.preprocessing.DataGenerator;
import se.gu.utils.LocalExecutionRunner;
import se.gu.utils.Utilities;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class ProjectDataVisitor implements CommitVisitor {
    private ProjectData projectData;
    FeatureAssetMapper assetMapper;
    private int count = 0;
    File diffFilesDirectory;
    private int previousCommitIndex = -1;
    private String previousCommit;
    private int commitCount;


    public ProjectDataVisitor(ProjectData projectData, int commits) {
        this.projectData = projectData;
        commitCount = commits;

        assetMapper = new FeatureAssetMapper(projectData);
        try {
            diffFilesDirectory = Utilities.createOutputDirectory(String.format("%s/%s/%s/%s", projectData.getConfiguration().getAnalysisDirectory(), projectData.getConfiguration().getCodeAbstractionLevel(), projectData.getConfiguration().getProjectRepository().getName(), "diffFiles"), true);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void process(SCMRepository repository, Commit commit, PersistenceMechanism writer) {
        try {
//            projectData.setAssetList(null);
//            projectData.setAssetList(new ArrayList<>());
//            projectData.setMlDataSet(null);
//            projectData.setMlDataSet(new HashMap<>());
//            projectData.setUnlabledMLDataSet(null);
//            projectData.setUnlabledMLDataSet(new HashMap<>());
//            projectData.setAssetFeatureMap(null);
//            projectData.setAssetFeatureMap(new ArrayList<>());

            previousCommitIndex = count;
            count++;
            List<String> allowedExtensions = Arrays.asList(projectData.getConfiguration().getAllowedFileExtensions().split(",")).parallelStream().map(e -> e.replace(".", "")).collect(Collectors.toList());

            //get modifications (modified files)
            List<Modification> modifications = commit.getModifications()
                    .parallelStream().filter(m -> allowedExtensions.contains(FilenameUtils.getExtension(m.getFileName()))).collect(Collectors.toList());
            //now checkout repo at this revision
            repository.getScm().checkout(commit.getHash());
            if (commit.getHash().equalsIgnoreCase("c4f1be007225b7a92af919024abc8a0459463463")) {
                System.out.println("HERE");
            }
            //get only modified files
            for (Modification modification : modifications) {
                String fileName = new File(String.format("%s/%s", repository.getPath(), modification.getFileName())).getAbsolutePath();
                String oldPath = new File(String.format("%s/%s", repository.getPath(), modification.getOldPath())).getAbsolutePath();
                if (modification.getType() == ModificationType.ADD || modification.getType() == ModificationType.COPY) {
                    //just add
                    addFileAsset(fileName, modification.getFileName(), repository.getPath());
                } else if (modification.getType() == ModificationType.MODIFY) {
                    //remove asset's children first
                    assetMapper.resetChildren(fileName,modification.getType());
                    //now add the file
                    addFileAsset(fileName, modification.getFileName(), repository.getPath());


                } else if (modification.getType() == ModificationType.RENAME) {
                    Set<String> keysForFile = projectData.getMlDataSet().keySet().stream().filter(k -> k.contains(oldPath)).collect(Collectors.toSet());
                    for (String key : keysForFile) {
                        Map<String, Double> value = projectData.getMlDataSet().get(key);
                        projectData.getMlDataSet().put(key.replace(oldPath, fileName), value);
                        projectData.getMlDataSet().remove(key);
                    }

                    projectData.getAssetList().parallelStream().filter(a -> a.getFullyQualifiedName().contains(oldPath)).forEach(a -> a.setFullyQualifiedName(a.getFullyQualifiedName().replace(oldPath, fileName)));
                    projectData.getAssetFeatureMap().parallelStream().filter(a -> a.getMappedAsset().getFullyQualifiedName().contains(oldPath)).forEach(a -> a.getMappedAsset().setFullyQualifiedName(a.getMappedAsset().getFullyQualifiedName().replace(oldPath, fileName)));

                    //assetMapper.resetChildren(fileName);
                    //assetMapper.resetChildren(new File(String.format("%s/%s", repository.getPath(),modification.getOldPath())).getAbsolutePath());
                    //addFileAsset(fileName,modification.getFileName(), repository.getPath());


                } else if (modification.getType() == ModificationType.DELETE) {
                    assetMapper.resetChildren(fileName, modification.getType());

                }


            }

            //read all files in repo
//            List<RepositoryFile> repositoryFiles = repository.getScm().files().parallelStream().filter(m->allowedExtensions.contains(FilenameUtils.getExtension(m.getFullName()))).collect(Collectors.toList());;
////            LocalExecutionRunner fileAnalyzerService = new LocalExecutionRunner();
////            int maxFutures = 2;//projectData.getConfiguration().getNumberOfThreads();
////            int futureCount = 0;
//            for(RepositoryFile file:repositoryFiles){
//                String fileRelativePath = file.getFullName().replace("\\","/").replace(repository.getPath().replace("\\","/")+"/","");
//                //if (futureCount < maxFutures) {
//                    FileAnalyzer fileAnalyzer = new FileAnalyzer(projectData,assetMapper,file.getFullName(),fileRelativePath,repository.getPath());
//                    fileAnalyzer.addFileAsset();
////                    fileAnalyzerService.addFuture((Future<?>) fileAnalyzerService.submit(fileAnalyzer));
////                    futureCount++;
////                }
////                if (futureCount == maxFutures) {
////                    fileAnalyzerService.waitForTaskToFinish();
////                    futureCount=0;
////                }
//
//            }

            // fileAnalyzerService.shutdown();

            if (projectData.getAssetFeatureMap().size() > 0) {

                //projectData.getAssetFeatureMap().parallelStream().forEach(m -> System.out.println(m));
                Utilities.writeStringFile(String.format("%s/%d_%s-MAP_%s.txt", diffFilesDirectory, count, commit.getHash(), projectData.getConfiguration().getProjectRepository().getName()), projectData.getAssetFeatureMappingsAsStringOutput());
            }
            //log changed assets
            assetMapper.logChangedAssets(modifications, repository, commit.getHash(), commit.getAuthor().getName());

            //create unlabeled assets
            //assetMapper.createUnlabeledAssets(modifications,repository);
            //create ML Data for all existing assets
            if (projectData.getConfiguration().isGenerateARRFFiles()) {
                //now genrate ML data
//                System.out.print("DATA GENERATION START: ");
//                CurrentTimeDateCalendar.printCurrentTimeUsingCalendar();
                //labeled data
                if (projectData.getAssetFeatureMap().size() > 0) {//generate data if mapped assets exist
                    LocalExecutionRunner metricsExecutorService = new LocalExecutionRunner();
                    DataGenerator dataGenerator = new DataGenerator(count, previousCommitIndex, commit.getHash(), previousCommit, projectData, metricsExecutorService, repository.getScm());
                    dataGenerator.createMLData();
                    //create arff
                    if (projectData.getMlDataSet().size() > 0) {
                        dataGenerator.createARFF();
                    }

                    //UNLABLED DATA
//                metricsExecutorService = new LocalExecutionRunner();
//                dataGenerator = new DataGenerator(count, previousCommitIndex, commit.getHash(), previousCommit, projectData, metricsExecutorService, repository.getScm());
//                dataGenerator.createUnLabeledData();
                    if (projectData.getUnlabledMLDataSet() != null && projectData.getUnlabledMLDataSet().size() > 0) {
                        dataGenerator.createUnlabledARFF();
                    }
//                System.out.print("DATA GENERATION END: ");
//                CurrentTimeDateCalendar.printCurrentTimeUsingCalendar();
                }
            }
            long mappedAssets = projectData.getAssetFeatureMap().parallelStream().map(FeatureAssetMap::getMappedAsset).distinct().count();
            writer.write(commit.getHash(), mappedAssets, projectData.getMlDataSet().size());

            previousCommit = commit.getHash();
//            List<String> mappedFeatureNames = projectData.getAssetFeatureMap().parallelStream().map(FeatureAssetMap::getFeatureName).distinct().collect(Collectors.toList());
//            projectData.setPreviousFeatureNames(mappedFeatureNames);
            System.out.printf("Commit %d/%d\n", count + 1, commitCount);


            //geneate ARFF
            //projectData.setPreviousFeatureNames(projectData.getFeatureNames());

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            repository.getScm().reset();
        }
    }

    public void addFileAsset(String fileName, String fileRelativePath, String repoPath) throws IOException, SQLException {

        if (fileName.contains(projectData.getConfiguration().getFolderMappingFile())) {
            assetMapper.mapFolder(fileName);
        } else if (fileName.contains(projectData.getConfiguration().getFileMappingFile())) {
            assetMapper.mapFiles(fileName, repoPath);
        } else {
            //map fragments and lines of code
            String fileText = FileUtils.readFileToString(new File(fileName), projectData.getConfiguration().getTextEncoding());
            //boolean fileContainsAnnotations = Utilities.isFileContainsAnnotations(fileText,projectData.getConfiguration().getAllAnnotationPatterns());
            // if (fileContainsAnnotations) {
            List<String> fileLines = FileUtils.readLines(new File(fileName), projectData.getConfiguration().getTextEncoding());
            assetMapper.mapFragments(fileName, fileRelativePath, fileText, fileLines);
            //System.out.printf("Mapped file: %s\n",fileName);
            // }
        }
    }

    private List<BlamedLine> getBlamedLines(String fileName, String commit, SCM scm) {
        return scm.blame(fileName, commit, false);
    }


}
