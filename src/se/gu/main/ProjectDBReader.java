package se.gu.main;

import com.google.common.base.Stopwatch;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.repodriller.domain.DiffLine;
import org.repodriller.domain.Modification;
import se.gu.assets.AnnotationType;
import se.gu.assets.AssetType;
import se.gu.data.DataController;
import se.gu.git.CodeChange;
import se.gu.git.Commit;
import se.gu.git.DiffEntry;
import se.gu.git.DiffExtractor;
import se.gu.git.scenarios.ScenarioHandler;
import se.gu.reader.AnnotationPair;
import se.gu.reader.AnnotationReaderDB;
import se.gu.utils.CommandRunner;
import se.gu.utils.Utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ProjectDBReader {
    public DiffExtractor getDiffExtractor() {
        return diffExtractor;
    }

    public void setDiffExtractor(DiffExtractor diffExtractor) {
        this.diffExtractor = diffExtractor;
    }

    private DiffExtractor diffExtractor;
    private FeatureAssetMapper assetMapper;
    DataController dataController;
    String projectName;
    private ProjectData projectData;

    public ProjectData getProjectData() {
        return projectData;
    }

    public void setProjectData(ProjectData projectData) {
        this.projectData = projectData;
    }

    public ProjectDBReader(ProjectData projectData) {

        this.projectData = projectData;
        assetMapper = new FeatureAssetMapper(projectData);

    }

    private String currentCommit = null;

    public void readCommits() throws Exception {
        DiffExtractor diffExtractor = new DiffExtractor(projectData.getConfiguration());
        projectName = projectData.getConfiguration().getProjectRepository().getName();
        this.diffExtractor = diffExtractor;
        if (diffExtractor != null) {
            diffExtractor.setCommitHistory();
            CommandRunner commandRunner = new CommandRunner(projectData.getConfiguration());
            List<Commit> commitHistory = diffExtractor.getCommitHistory();
            int lastRunCommitIndex = projectData.getConfiguration().isCalculateMetrics() ? projectData.getConfiguration().getExperimentStartingCommit() : projectData.getLastRunCommitIndex();
            int commitsToRun = projectData.getConfiguration().getCommitsToExecute() == 0 ? commitHistory.size() : projectData.getConfiguration().getCommitsToExecute();
            List<String> featureModelFiles = Arrays.asList(projectData.getConfiguration().getFeatureModelFile().split(","));
            try (ProgressBar pb = new ProgressBar("ML DATA:", commitsToRun)) {
                pb.stepTo(lastRunCommitIndex);
                for (int i = lastRunCommitIndex > 0 ? lastRunCommitIndex + 1 : lastRunCommitIndex; i < commitsToRun; i++) {
                    Stopwatch stopwatch = Stopwatch.createStarted();
                    pb.step();


                    Commit commit = commitHistory.get(i);
                    currentCommit = commit.getCommitHash();
                    pb.setExtraMessage(currentCommit);

                    if (!projectData.getConfiguration().isGetAnnotationsWithoutCommits()) {
                        commandRunner.checkOutCommit(currentCommit);
                    }

                    BufferedReader bufferedReader = commandRunner.getDiffFromOneCommit(currentCommit);
                    diffExtractor.setDiffEntries(bufferedReader);
                    List<DiffEntry> diffEntries = diffExtractor.getDiffEntries();
                    for (DiffEntry diffEntry : diffEntries) {
                        //skip those not added..not handling deletions yet
                        if (diffEntry.getAddedFullyQualifiedName() == null) {
                            continue;
                        }
                        File diffEntryFile = new File(diffEntry.getAddedFullyQualifiedName());
                        if (featureModelFiles.contains(diffEntry.getAddedFileName())) {
                            //skip Feature Model since already done
                            continue;
                        } else if (diffEntry.getAddedFileName().equalsIgnoreCase(projectData.getConfiguration().getFolderMappingFile())) {
                            //mapVpFolder(diffEntry.getAddedFullyQualifiedName());
                            //assetMapper.mapFolder(diffEntry.getAddedFullyQualifiedName());
                            List<String> fileTextAsList = FileUtils.readLines(diffEntryFile, projectData.getConfiguration().getTextEncoding());
                            mapFolder(diffEntryFile, commit, i + 1, "ADD", fileTextAsList);
                        } else if (diffEntry.getAddedFileName().equalsIgnoreCase(projectData.getConfiguration().getFileMappingFile())) {
                            //mapVPFile(diffEntry.getAddedFullyQualifiedName());
                            //assetMapper.mapFiles(diffEntry.getAddedFullyQualifiedName(), projectData.getConfiguration().getProjectRepository().getAbsolutePath());
                            List<String> fileTextAsList = FileUtils.readLines(diffEntryFile, projectData.getConfiguration().getTextEncoding());
                            mapFiles(diffEntryFile, commit, i + 1, "ADD", fileTextAsList);
                        } else {
                            //fragment annotations
                            if (!Utilities.extensionAllowed(diffEntryFile, projectData.getConfiguration())) {
                                continue;
                            }
                            List<Integer> linesAffectedInCommit = getFileLinesAffectedInCommit(diffEntry);
                            List<String> fileTextAsList = FileUtils.readLines(diffEntryFile, projectData.getConfiguration().getTextEncoding());
                            //add the file as an asset
                            dataController.assertInsert(diffEntryFile.getAbsolutePath(), diffEntryFile.getName(), diffEntryFile.getParent(), currentCommit, i + 1, commit.getAuthor(),
                                    AssetType.FILE.toString(), 1, fileTextAsList.size(), 0, projectName, "ADD", fileTextAsList.size());
                            //map fragments and lines of code
                            AnnotationReaderDB annotationReader = new AnnotationReaderDB(projectData);
                            List<AnnotationPair> pairs;
                            List<Integer> annotatedLines;
                            //add all lines in file
                            //now read annotations from whole file
                            pairs = annotationReader.readFragmentAnnotationsFromEntireFile(fileTextAsList);
                            annotatedLines = new ArrayList<>();
                            //add each pair as an asset
                            createFragmentsFromWholeFile(commit, i + 1, "ADD", fileTextAsList, diffEntryFile, pairs, annotatedLines, true, true, linesAffectedInCommit);
                            //add non-annotated lines in the file
                            //create blocks based on consecutive non annotated lines
                            List<AnnotationPair> nonPairs = createNonPairs(fileTextAsList, annotatedLines);
                            //now add non-annotated assets
                            createFragmentsFromWholeFile(commit, i + 1, "ADD", fileTextAsList, diffEntryFile, nonPairs, null, false, false,linesAffectedInCommit);


                        }


                    }

                    stopwatch.stop();
                    System.out.printf("\ntime for processing commit %s is %s seconds\n", currentCommit, stopwatch.elapsed(TimeUnit.SECONDS));
                }
            }


        }
    }

    private List<Integer> getFileLinesAffectedInCommit(DiffEntry diffEntry) {
        List<Integer> linesAffected = new ArrayList<>();
        for (CodeChange change : diffEntry.getCodeChanges()) {
            int startLine = change.getAddedLinesStart();
            int addedLines = change.getAddedLines();
            for (int i = startLine; i <= addedLines; i++) {
                linesAffected.add(i);
            }
        }
        return linesAffected;
    }

    private List<AnnotationPair> createNonPairs(List<String> fileTextAsList, List<Integer> annotatedLines) {
        List<AnnotationPair> nonPairs = new ArrayList<>();

        boolean pairCreated = false;
        int numLines = fileTextAsList.size();
        int startingLine = 0, endLine = 0;
        if (annotatedLines.size() == 0) {
            AnnotationPair pair = new AnnotationPair(1, numLines, "NONE", AnnotationType.NONE);
            nonPairs.add(pair);
        } else {
            for (int lineNumber = 1; lineNumber <= numLines; lineNumber++) {
                boolean lineAnnotated = annotatedLines.contains(lineNumber);
                if (lineNumber == numLines && pairCreated) {
                    endLine = lineNumber;
                    nonPairs.add(new AnnotationPair(startingLine, endLine, "NONE", AnnotationType.NONE));
                    pairCreated = false;
                } else if (!lineAnnotated && !pairCreated) {
                    startingLine = lineNumber;
                    pairCreated = true;
                } else if (lineAnnotated && pairCreated) {
                    endLine = lineNumber - 1;
                    nonPairs.add(new AnnotationPair(startingLine, endLine, "NONE", AnnotationType.NONE));
                    pairCreated = false;

                } else {
                    continue;
                }


            }
        }


        return nonPairs;
    }

    private void createFragmentsFromWholeFile(Commit commit, int commitIndex, String changeType, List<String> fileTextAsList, File file, List<AnnotationPair> pairs, List<Integer> annotatedLines, boolean isMapFeatures, boolean isTrackAnnotated,List<Integer> linesAffectedInCommit) throws SQLException {
        for (AnnotationPair pair : pairs) {
            String fragmentName = String.format("%s%s%d-%d", file.getAbsolutePath(), projectData.getConfiguration().getFeatureQualifiedNameSeparator(), pair.getStartLine(), pair.getEndLine());
            List<String> features = Arrays.asList(pair.getFeatureName().split(projectData.getConfiguration().getMultiFileMappingSeperator()));
            //add fragment asset
            dataController.assertInsert(fragmentName, fragmentName, file.getAbsolutePath(), commit.getCommitHash(), commitIndex, commit.getAuthor(),
                    AssetType.FRAGMENT.name(), pair.getStartLine(), pair.getEndLine(), 0, projectName, changeType,pair.getEndLine()- pair.getStartLine()+1);
            //map fragment asset
            if (isMapFeatures) {
                mapAssetToFeatures(fragmentName,AssetType.FRAGMENT.name(), file.getAbsolutePath(),features, AnnotationType.FRAGMENT, commit.getCommitHash(), commitIndex, commit.getAuthor());
            }
            //add lines in the pair
            for (int l = pair.getStartLine(); l <= pair.getEndLine(); l++) {
                if (isTrackAnnotated) {
                    annotatedLines.add(l);
                }
                //get rid of blank lines
                String lineContent = fileTextAsList.get(l - 1).replaceAll("[^a-zA-Z0-9\\s]", "");//remove non word characters except white spaces
                //skip all blank lines
                if (StringUtils.isBlank(lineContent)) {
                    continue;
                }
                String lineName = String.format("%s%s%d", file.getAbsolutePath(), projectData.getConfiguration().getFeatureQualifiedNameSeparator(), l);
                //add line asset
                dataController.assertInsert(lineName, lineName, file.getAbsolutePath(), commit.getCommitHash(), commitIndex, commit.getAuthor(),
                        AssetType.LOC.name(), l, l, l, projectName, changeType,1);
                //map line asset
                if (isMapFeatures) {
                    mapAssetToFeatures(lineName,AssetType.LOC.name(), file.getAbsolutePath(),features, AnnotationType.FRAGMENT, commit.getCommitHash(), commitIndex, commit.getAuthor());
                }

            }
        }
    }

    public void mapFolder(File vpFolderFile, Commit commit, int commitIndex, String changeType, List<String> fileTextAsList) throws IOException, SQLException {


        if (vpFolderFile.exists()) {
            File vpFolder = vpFolderFile.getParentFile();
            if (vpFolder.exists()) {
                //add asset
                //asset full name is full path of folder
                dataController.assertInsert(vpFolder.getAbsolutePath(), vpFolder.getName(), vpFolder.getParent(), commit.getCommitHash(), commitIndex, commit.getAuthor(),
                        AssetType.FOLDER.toString(), 0, 0, 0, projectName, changeType,0);

                //map each feature to folder
                mapAssetToFeatures(vpFolder.getAbsolutePath(),AssetType.FOLDER.toString(),vpFolder.getParent(), fileTextAsList, AnnotationType.FOLDER, commit.getCommitHash(), commitIndex, commit.getAuthor());

            }
        }
    }

    public void mapFiles(File vpFile, Commit commit, int commitIndex, String changeType, List<String> fileTextAsList) throws IOException, SQLException {

        if (vpFile.exists()) {
            int records = fileTextAsList.size();
            for (int j = 0; j < records; j += 2) {
                String[] fileNames = fileTextAsList.get(j).split(projectData.getConfiguration().getMultiFileMappingSeperator());
                String[] features = fileTextAsList.get(j + 1).split(projectData.getConfiguration().getMultiFileMappingSeperator());
                //map files to features
                for (String fileName : fileNames) {
                    File file = new File(vpFile.getParentFile().getAbsolutePath() + "/" + fileName);
                    //String fileRelativePath = file.getAbsolutePath().replace("\\","/").replace(repoPath.replace("\\","/")+"/","");
                    if (file.exists()) {
                        dataController.assertInsert(file.getAbsolutePath(), file.getName(), file.getParent(), commit.getCommitHash(), commitIndex, commit.getAuthor(), AssetType.FILE.toString(),
                                0, 0, 0, projectName, changeType, fileTextAsList.size());
                        mapAssetToFeatures(file.getAbsolutePath(),AssetType.FILE.name(),file.getParent(), Arrays.asList(features), AnnotationType.FILE, commit.getCommitHash(), commitIndex, commit.getAuthor());
                    }
                }
            }
        }
    }

    public void mapAssetToFeatures(String asset,String assetType, String parent, List<String> features, AnnotationType annotationType, String commitHash, int commitIndex, String developer) throws SQLException {
        for (String feature : features) {
            dataController.assertMappingInsert(asset,assetType,parent, feature, projectName, annotationType.toString(), commitHash, commitIndex, developer);
        }
    }

}
