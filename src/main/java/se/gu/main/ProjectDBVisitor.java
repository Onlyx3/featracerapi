package se.gu.main;

import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.repodriller.domain.*;
import org.repodriller.persistence.PersistenceMechanism;
import org.repodriller.scm.CommitVisitor;
import org.repodriller.scm.SCMRepository;
import se.gu.assets.AnnotationType;
import se.gu.assets.AssetType;
import se.gu.data.DataController;
import se.gu.reader.AnnotationPair;
import se.gu.reader.AnnotationReaderDB;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class ProjectDBVisitor implements CommitVisitor {
    private ProjectData projectData;
    private int count = 0;
    DataController dataController;
    String projectName;
    private int commitCount;
    private List<String> commitHashes;
    private int commitIndex;

    public ProjectDBVisitor(ProjectData projectData, int commitCount, List<String> commitHashes) throws SQLException, ClassNotFoundException {
        this.projectData = projectData;
        projectName = projectData.getConfiguration().getProjectRepository().getName();
        this.commitCount = commitCount;
        this.commitHashes = commitHashes;

        //dataController = new DataController(projectData.getConfiguration());
    }


    @Override
    public void process(SCMRepository repository, Commit commit, PersistenceMechanism persistenceMechanism) {

        try {
            dataController = new DataController(projectData.getConfiguration());
//            int index=0;
//            for(String c:commitHashes){
//                index++;
//                if(c.equalsIgnoreCase(commit.getHash())){
//                    break;
//                }
//            }
            count++;
            commitIndex = count;// commitHashes.indexOf(commit.getHash()) + 1;
            int startingCommitIndex = projectData.getConfiguration().getStartingCommitIndex();
            if(commitIndex <startingCommitIndex){
                return;
            }
            System.out.printf("Processing commit %d/%d: %s ", count, commitCount,commit.getHash());
            //delete existing assets for commit and project
            try {
                dataController.deleteAssetsForCommit(commit.getHash(), projectName);
            }catch (Exception ex){
                ex.printStackTrace();
            }
//            if (commit.getHash().equalsIgnoreCase("bbac41e93fe90a2a25420472dbc509944db0ab20")) {
//                System.out.println(commit.getHash());
//                return;
//            }


            List<String> allowedExtensions = Arrays.asList(projectData.getConfiguration().getAllowedFileExtensions().split(",")).parallelStream().map(e -> e.replace(".", "")).collect(Collectors.toList());

            //get modifications (modified files)
            List<Modification> modifications = commit.getModifications()
                    .parallelStream().filter(m -> allowedExtensions.contains(FilenameUtils.getExtension(m.getFileName())) ||
                            m.getFileName().contains(projectData.getConfiguration().getFileMappingFile()) ||
                            m.getFileName().contains(projectData.getConfiguration().getFolderMappingFile())).collect(Collectors.toList());

//create a map of modifications and their diffs
            Map<Modification, String> diffs = new HashMap<>();
            for (Modification modification : modifications) {
                diffs.put(modification, modification.getDiff());
            }
            //if one of the diffs is too big, check out the repo at this commit to read big added files
            for (Modification key : diffs.keySet()) {
                if (diffs.get(key).equalsIgnoreCase("-- TOO BIG --") ||
                        key.getFileName().contains(projectData.getConfiguration().getFileMappingFile()) ||
                        key.getFileName().contains(projectData.getConfiguration().getFolderMappingFile())) {
                    //check out repo at this state to be able to read contents of big files added
                    repository.getScm().checkout(commit.getHash());
                    break;
                }
            }
            try (ProgressBar pb = new ProgressBar("Modifications:", diffs.size())) {
            for (Modification modification : diffs.keySet()) {
                try {
                    String fileName = new File(String.format("%s/%s", repository.getPath(), modification.getFileName())).getAbsolutePath();
                    String oldPath = new File(String.format("%s/%s", repository.getPath(), modification.getOldPath())).getAbsolutePath();
                    pb.step();
                    pb.setExtraMessage(fileName);
                    if (modification.getType() == ModificationType.DELETE) {
                        File file = new File(fileName);
                        dataController.assertInsert(file.getAbsolutePath(), file.getName(), file.getParent(), commit.getHash(), commitIndex, commit.getAuthor().getName(),
                                AssetType.FILE.toString(), 0, 0, 0, projectName, modification.getType().name(), modification.getRemoved());

                    } else if (modification.getType() == ModificationType.RENAME) {

                        File file = new File(fileName);
                        dataController.assertInsert(file.getAbsolutePath(), file.getName(), file.getParent(), commit.getHash(), commitIndex, commit.getAuthor().getName(),
                                AssetType.FILE.toString(), 0, 0, 0, projectName, modification.getType().name(), modification.getAdded());
                        dataController.renameAssetAndChildren(oldPath, fileName);


                    } else {
                        //just add
                        addFileAsset(fileName, commit, commitIndex, modification, diffs);
                    }
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        }


        } catch (Exception ex) {
            ex.printStackTrace();
        }finally {
            try {
                dataController.closeConnection();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
    }

    public void addFileAsset(String fileName, Commit commit, int commitIndex, Modification modification, Map<Modification, String> diffs) throws IOException, SQLException {

        //read diff first
        String diff = diffs.get(modification);
        List<String> fileTextAsList = new ArrayList<>();

        List<DiffLine> newDiffLines = new ArrayList<>();

        List<DiffBlock> blocks;
        if (diff.equalsIgnoreCase("-- TOO BIG --")) {
            //read current state of file
            fileTextAsList = FileUtils.readLines(new File(fileName), projectData.getConfiguration().getTextEncoding());

        } else {
            //pass the diff
            DiffParser diffParser = null;
            boolean diffBig = false;
            try {
                diffParser = new DiffParser(diff);
            }catch (Exception ex){
                if(ex.getMessage().contains("Impossible to get line positions in this diff")){
                    diff="-- TOO BIG --";
                    diffBig=true;
                    File f = new File(fileName);
                    if(!f.exists()){
                        return;
                    }
                    fileTextAsList = FileUtils.readLines(f, projectData.getConfiguration().getTextEncoding());
                }
            }
            if(!diffBig) {
                blocks = diffParser.getBlocks();
                for (DiffBlock block : blocks) {
                    if (StringUtils.isBlank(block.getDiffBlock())) {
                        continue;
                    }
                    newDiffLines.addAll(block.getLinesInNewFile().parallelStream().filter(l -> l.getType() != DiffLineType.REMOVED).collect(Collectors.toList()));
                }
                //now make the diff lines into one
                fileTextAsList.addAll(newDiffLines.parallelStream().map(DiffLine::getLine).collect(Collectors.toList()));
            }

        }
        if (fileName.contains(projectData.getConfiguration().getFolderMappingFile())) {
            mapFolder(fileName, commit, commitIndex, modification.getType().name(), fileTextAsList);
        } else if (fileName.contains(projectData.getConfiguration().getFileMappingFile())) {
            mapFiles(fileName, commit, commitIndex, modification.getType().name(), fileTextAsList);
        } else {
            //add the file as asset
            File file = new File(fileName);
            dataController.assertInsert(file.getAbsolutePath(), file.getName(), file.getParent(), commit.getHash(), commitIndex, commit.getAuthor().getName(),
                    AssetType.FILE.toString(), 0, 0, 0, projectName, modification.getType().name(), fileTextAsList.size());
            //map fragments and lines of code
            AnnotationReaderDB annotationReader = new AnnotationReaderDB(projectData);
            List<AnnotationPair> pairs;
            List<Integer> annotatedLines;

            if (diff.equalsIgnoreCase("-- TOO BIG --")) {
                //add all lines in file
                //now read annotations from whole file
                pairs = annotationReader.readFragmentAnnotationsFromEntireFile(fileTextAsList);
                annotatedLines = new ArrayList<>();
                //add each pair as an asset
                createFragmentsFromWholeFile(commit, commitIndex, modification, fileTextAsList, file, pairs, annotatedLines,true,true);
                //add non-annotated lines in the file
                //create blocks based on consecutive non annotated lines
                List<AnnotationPair> nonPairs = createNonPairs(fileTextAsList,newDiffLines, annotatedLines,true);
                //now add non-annotated assets
                createFragmentsFromWholeFile(commit, commitIndex, modification, fileTextAsList, file, nonPairs, null,false,false);

            }else {
                //read diff blocks
//be mindful that some annotations might just be &begin [featureA].....lines....&end[featureA] so you might mis the actual code annotated. Identify lines annotated in this case
                annotatedLines = new ArrayList<>();
                pairs = annotationReader.readFragmentAnnotationsFromCombinedDiffBlocks(newDiffLines);
                createFragmentsFromDiffBlocks(commit, commitIndex, modification,  file, pairs, annotatedLines,true,true);
                List<AnnotationPair> nonPairs = createNonPairs(fileTextAsList,newDiffLines, annotatedLines,false);
                //now add non-annotated assets
                createFragmentsFromDiffBlocks(commit, commitIndex, modification,  file, nonPairs, null,false,false);


            }

        }
    }

    private List<AnnotationPair> createNonPairs(List<String> fileTextAsList,List<DiffLine> newDiffLines, List<Integer> annotatedLines, boolean isReadFromWholeFile) {
        List<AnnotationPair> nonPairs = new ArrayList<>();

        if (isReadFromWholeFile) {

            boolean pairCreated = false;
            int numLines = fileTextAsList.size();
            int startingLine = 0,endLine=0;
            if(annotatedLines.size()==0){
                AnnotationPair pair = new AnnotationPair(1,numLines,"NONE",AnnotationType.NONE);
                nonPairs.add(pair);
            }else{
                for(int lineNumber=1;lineNumber<=numLines;lineNumber++){
                    boolean lineAnnotated = annotatedLines.contains(lineNumber);
                    if(lineNumber==numLines&&pairCreated){
                        endLine=lineNumber;
                        nonPairs.add(new AnnotationPair(startingLine,endLine,"NONE",AnnotationType.NONE));
                        pairCreated=false;
                    }
                    else if(!lineAnnotated && !pairCreated){
                        startingLine = lineNumber;
                        pairCreated=true;
                    }else if(lineAnnotated && pairCreated){
                            endLine=lineNumber-1;
                            nonPairs.add(new AnnotationPair(startingLine,endLine,"NONE",AnnotationType.NONE));
                            pairCreated=false;

                        }
                    else{
                        continue;
                    }


                }
            }

        }else {

            boolean pairCreated = false;
            DiffLine lastLine = newDiffLines.get(newDiffLines.size()-1);

            int startingLine = 0,endLine=0;
            if(annotatedLines.size()==0){
                AnnotationPair pair = new AnnotationPair(newDiffLines.get(0).getLineNumber(), lastLine.getLineNumber(), "NONE",AnnotationType.NONE);
                nonPairs.add(pair);
            }else{
                for(DiffLine diffLine:newDiffLines){

                    boolean lineAnnotated = annotatedLines.contains(diffLine.getLineNumber());
                    if(diffLine.equals(lastLine)&&pairCreated){
                        endLine= diffLine.getLineNumber();
                        nonPairs.add(new AnnotationPair(startingLine,endLine,"NONE",AnnotationType.NONE));
                        pairCreated=false;
                    }
                    else if(!lineAnnotated && !pairCreated){
                        startingLine = diffLine.getLineNumber();
                        pairCreated=true;
                    }else if(lineAnnotated && pairCreated){
                            endLine= diffLine.getLineNumber()-1;
                            nonPairs.add(new AnnotationPair(startingLine,endLine,"NONE",AnnotationType.NONE));
                            pairCreated=false;

                        }
                    else {
                        continue;
                    }


                }
            }
        }
        return  nonPairs;
    }

    private void createFragmentsFromWholeFile(Commit commit, int commitIndex, Modification modification, List<String> fileTextAsList, File file, List<AnnotationPair> pairs, List<Integer> annotatedLines, boolean isMapFeatures, boolean isTrackAnnotated) throws SQLException {
        for (AnnotationPair pair : pairs) {
            String fragmentName = String.format("%s%s%d-%d", file.getAbsolutePath(), projectData.getConfiguration().getFeatureQualifiedNameSeparator(), pair.getStartLine(), pair.getEndLine());
            List<String> features = Arrays.asList(pair.getFeatureName().split(projectData.getConfiguration().getMultiFileMappingSeperator()));
            //add fragment asset
            dataController.assertInsert(fragmentName, fragmentName, file.getAbsolutePath(), commit.getHash(), commitIndex, commit.getAuthor().getName(),
                    AssetType.FRAGMENT.name(), pair.getStartLine(), pair.getEndLine(), 0, projectName, modification.getType().name(),Math.abs(pair.getEndLine()- pair.getStartLine()+1));
            //map fragment asset
            if(isMapFeatures) {
                mapAssetToFeatures(fragmentName,AssetType.FRAGMENT.name(),file.getAbsolutePath(), features, AnnotationType.FRAGMENT, commit.getHash(), commitIndex, commit.getAuthor().getName());
            }
            //add lines in the pair
            for (int l = pair.getStartLine(); l <= pair.getEndLine(); l++) {
                if(isTrackAnnotated) {
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
                dataController.assertInsert(lineName, lineName, file.getAbsolutePath(), commit.getHash(), commitIndex, commit.getAuthor().getName(),
                        AssetType.LOC.name(), l, l, l, projectName, modification.getType().name(),1);
                //map line asset
                if(isMapFeatures) {
                    mapAssetToFeatures(lineName,AssetType.LOC.name(),file.getAbsolutePath(), features, AnnotationType.FRAGMENT, commit.getHash(), commitIndex, commit.getAuthor().getName());
                }

            }
        }
    }

    private void createFragmentsFromDiffBlocks(Commit commit, int commitIndex, Modification modification,  File file, List<AnnotationPair> pairs, List<Integer> annotatedLines, boolean isMapFeatures, boolean isTrackAnnotated) throws SQLException {
        for (AnnotationPair pair : pairs) {
            String fragmentName = String.format("%s%s%d-%d", file.getAbsolutePath(), projectData.getConfiguration().getFeatureQualifiedNameSeparator(), pair.getStartLine(), pair.getEndLine());
            List<String> features = Arrays.asList(pair.getFeatureName().split(projectData.getConfiguration().getMultiFileMappingSeperator()));
            //add fragment asset
            dataController.assertInsert(fragmentName, fragmentName, file.getAbsolutePath(), commit.getHash(), commitIndex, commit.getAuthor().getName(),
                    AssetType.FRAGMENT.name(), pair.getStartLine(), pair.getEndLine(), 0, projectName, modification.getType().name(),Math.abs(pair.getEndLine()- pair.getStartLine()+1));
            //map fragment asset
            if(isMapFeatures) {
                mapAssetToFeatures(fragmentName,AssetType.FRAGMENT.name(),file.getAbsolutePath(), features, AnnotationType.FRAGMENT, commit.getHash(), commitIndex, commit.getAuthor().getName());
            }
            //add lines in the pair
            for (int l = pair.getStartLine(); l <= pair.getEndLine(); l++) {
                if(isTrackAnnotated) {
                    annotatedLines.add(l);
                }

                String lineName = String.format("%s%s%d", file.getAbsolutePath(), projectData.getConfiguration().getFeatureQualifiedNameSeparator(), l);
                //add line asset
                dataController.assertInsert(lineName, lineName, file.getAbsolutePath(), commit.getHash(), commitIndex, commit.getAuthor().getName(),
                        AssetType.LOC.name(), l, l, l, projectName, modification.getType().name(),1);
                //map line asset
                if(isMapFeatures) {
                    mapAssetToFeatures(lineName,AssetType.LOC.name(),file.getAbsolutePath(), features, AnnotationType.FRAGMENT, commit.getHash(), commitIndex, commit.getAuthor().getName());
                }

            }
        }
    }

    public void mapFolder(String vpFolderPath, Commit commit, int commitIndex, String changeType, List<String> fileTextAsList) throws IOException, SQLException {

        File vpFolderFile = new File(vpFolderPath);
        if (vpFolderFile.exists()) {
            File vpFolder = vpFolderFile.getParentFile();
            //if (vpFolder.exists()) {
                //add asset
                //asset full name is full path of folder
                dataController.assertInsert(vpFolder.getAbsolutePath(), vpFolder.getName(), vpFolder.getParent(), commit.getHash(), commitIndex, commit.getAuthor().getName(),
                        AssetType.FOLDER.toString(), 0, 0, 0, projectName, changeType,0);

                //map each feature to folder
                mapAssetToFeatures(vpFolder.getAbsolutePath(),AssetType.FOLDER.toString(),vpFolder.getParent(), fileTextAsList, AnnotationType.FOLDER, commit.getHash(), commitIndex, commit.getAuthor().getName());

            //}
        }
    }

    public void mapFiles(String vpFilePath, Commit commit, int commitIndex, String changeType, List<String> fileTextAsList) throws IOException, SQLException {
        File vpFile = new File(vpFilePath);
        if (vpFile.exists()) {
            int records = fileTextAsList.size();
            for (int j = 0; j < records; j += 2) {
                String[] fileNames = fileTextAsList.get(j).split(projectData.getConfiguration().getMultiFileMappingSeperator());
                String[] features = fileTextAsList.get(j + 1).split(projectData.getConfiguration().getMultiFileMappingSeperator());
                //map files to features
                for (String fileName : fileNames) {
                    File file = new File(vpFile.getParentFile().getAbsolutePath() + "/" + fileName);
                    //String fileRelativePath = file.getAbsolutePath().replace("\\","/").replace(repoPath.replace("\\","/")+"/","");
                    //if (file.exists()) {
                        dataController.assertInsert(file.getAbsolutePath(), file.getName(), file.getParent(), commit.getHash(), commitIndex, commit.getAuthor().getName(), AssetType.FILE.toString(),
                                0, 0, 0, projectName, changeType, fileTextAsList.size());
                        mapAssetToFeatures(file.getAbsolutePath(),AssetType.FILE.toString(),file.getParent(), Arrays.asList(features), AnnotationType.FILE, commit.getHash(), commitIndex, commit.getAuthor().getName());
                    //}
                }
            }
        }
    }

    public void mapAssetToFeatures(String asset,String assetType, String parent, List<String> features, AnnotationType annotationType, String commitHash, int commitIndex, String developer) throws SQLException {
        for (String feature : features) {
            dataController.assertMappingInsert(asset,assetType,parent, feature.trim(), projectName, annotationType.toString(), commitHash, commitIndex, developer);
        }
    }
}
