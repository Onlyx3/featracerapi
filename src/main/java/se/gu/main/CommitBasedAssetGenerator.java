package se.gu.main;

import org.apache.commons.io.FileUtils;
import se.gu.assets.AssetChanged;
import se.gu.git.CodeChange;
import se.gu.git.Commit;
import se.gu.git.DiffEntry;
import se.gu.utils.CommandRunner;
import se.gu.utils.Utilities;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommitBasedAssetGenerator implements Runnable {

    public CommitBasedAssetGenerator(ProjectData projectData, DiffEntry diffEntry, FeatureAssetMapper assetMapper, Commit currentCommit, List<String> featureModelFiles) {
        this.projectData = projectData;
        this.diffEntry = diffEntry;
        this.assetMapper = assetMapper;
        this.currentCommit = currentCommit;
        this.featureModelFiles = featureModelFiles;
    }

    private ProjectData projectData;
private DiffEntry diffEntry;
private FeatureAssetMapper assetMapper;
private Commit currentCommit;
private List<String> featureModelFiles;
    @Override
    public void run() {
        try {

            if (diffEntry.getAddedFullyQualifiedName() == null) {
                return;
            }
            File diffEntryFile = new File(diffEntry.getAddedFullyQualifiedName());
            if (featureModelFiles.contains(diffEntry.getAddedFileName())) {
                //skip Feature Model since already done
                return;
            } else if (diffEntry.getAddedFileName().equalsIgnoreCase(projectData.getConfiguration().getFolderMappingFile())) {
                //mapVpFolder(diffEntry.getAddedFullyQualifiedName());
                assetMapper.mapFolder(diffEntry.getAddedFullyQualifiedName());
            } else if (diffEntry.getAddedFileName().equalsIgnoreCase(projectData.getConfiguration().getFileMappingFile())) {
                //mapVPFile(diffEntry.getAddedFullyQualifiedName());
                assetMapper.mapFiles(diffEntry.getAddedFullyQualifiedName(), projectData.getConfiguration().getProjectRepository().getAbsolutePath());
            } else {
                if(currentCommit.getCommitHash().equalsIgnoreCase("8167c084d933cdd26895547ae721188a6ad99625")){
                    System.out.println("STOP HERE");
                }
                //fragment annotations
                if (!Utilities.extensionAllowed(diffEntryFile, projectData.getConfiguration())) {
                    return;
                }
                //get text from file refered to in the diff
                String relativeFileName = diffEntryFile.getAbsolutePath().replace("\\", "/").replace(projectData.getConfiguration().getProjectRepository().getAbsolutePath().replace("\\", "/") + "/", "");
//chekc out this file
                CommandRunner runner = new CommandRunner(projectData.getConfiguration());
                runner.checkOutFileAtCommit(currentCommit.getCommitHash(),relativeFileName);
                //now read file text and extract mappings
                String fileText = projectData.getConfiguration().isCalculateMetrics()?null: FileUtils.readFileToString(diffEntryFile, projectData.getConfiguration().getTextEncoding());
                List<String> fileLines = FileUtils.readLines(diffEntryFile, projectData.getConfiguration().getTextEncoding());
                assetMapper.mapFragments(diffEntry.getAddedFullyQualifiedName(), relativeFileName, fileText, fileLines);

                if (!projectData.getConfiguration().isCalculateMetrics()) {
                    addChangedAssetFromDiffEntry(diffEntry, relativeFileName, diffEntryFile, currentCommit);
                }
                //createFileAssets(diffFilesDirectory, currentCommit, diffEntry, diffEntryFile, false);
            }

        }catch (Exception ex){
            ex.printStackTrace();
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
}
