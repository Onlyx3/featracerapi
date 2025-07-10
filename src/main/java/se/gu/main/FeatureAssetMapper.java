package se.gu.main;

import se.gu.assets.*;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.repodriller.domain.*;
import org.repodriller.scm.SCMRepository;
import se.gu.assets.*;
import se.gu.reader.AnnotationPair;
import se.gu.reader.AnnotationReader;
import se.gu.utils.LocalExecutionRunner;
import se.gu.utils.Utilities;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FeatureAssetMapper implements Serializable {
    private static final long serialVersionUID = 8195717304886038271L;
    private ProjectData projectData;

    public FeatureAssetMapper(ProjectData projectData) {
        this.projectData = projectData;
    }

    public void mapFolder(String vpFolderPath) throws IOException, SQLException {

        File vpFolderFile = new File(vpFolderPath);
        if (vpFolderFile.exists()) {
            File vpFolder = vpFolderFile.getParentFile();
            if (vpFolder.exists()) {
                Asset vpAsset = projectData.getConfiguration().isSaveDataInDataBase()?projectData.addFileAssetToDB(vpFolder): projectData.addFileAsset(vpFolder);//get folder referenced by vp-folderfile
                List<String> fileTextAsList = FileUtils.readLines(vpFolderFile, projectData.getConfiguration().getTextEncoding());
                mapAssetToFeatures(vpAsset, fileTextAsList, AnnotationType.FOLDER);

            }
        }
    }

    public void mapFiles(String vpFilePath,String repoPath) throws IOException, SQLException {
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
                    String fileRelativePath = file.getAbsolutePath().replace("\\","/").replace(repoPath.replace("\\","/")+"/","");
                    if (file.exists()) {
                        String fileText = FileUtils.readFileToString(file, projectData.getConfiguration().getTextEncoding());
                        List<String> fileLines = FileUtils.readLines(file, projectData.getConfiguration().getTextEncoding());
                        Asset fileAsset = createFileAsset(file, fileText,fileLines.size());
                        fileAsset.setFileRelativePath(fileRelativePath);
                        mapAssetToFeatures(fileAsset, Arrays.asList(features), AnnotationType.FILE);
                    }
                }
            }
        }
    }

    public void mapFragments(String fileName,String fileRelativePath, String fileText, List<String> fileLines) throws IOException, SQLException {
        Asset fileAsset = createFileAsset(new File(fileName), fileText,fileLines.size());
        fileAsset.setFileRelativePath(fileRelativePath);
        if(projectData.getConfiguration().isCalculateMetrics()){
            //log asset changed
            projectData.getChangedAssetsInCurrentCommit().add(fileAsset);
        }
        //now read annotations from file
        AnnotationReader annotationReader = new AnnotationReader(projectData, fileAsset, fileLines);
        annotationReader.readFragmentAnnotations();


    }


    public void mapFragment(Asset fileAsset, List<String> fileLines, AnnotationPair pair, String features) throws SQLException {
        if(features.contains("_AND_")||features.contains("_OR_")){
            //System.out.printf("Tangled feature: %s\n",features);
        }
        List<String> splitFeatures = new ArrayList<>();
        splitFeatures.add(features);
        splitFeatures.addAll(Arrays.asList(features.split(projectData.getConfiguration().getMultipleFeatureRegex())));
        splitFeatures = splitFeatures.parallelStream().distinct().collect(Collectors.toList());



        //For example featureA AND featureB OR featureC
        //will be split into three features but we will also include the main combination as a feature
        Asset fragmentAsset = addAnnotationAsAssetToFile(fileAsset, pair, splitFeatures);
        if(!projectData.getConfiguration().isCalculateMetrics()) {//only add lines if not calculating metrics

            //mapAssetToFeatures(fragmentAsset, Arrays.asList(features.split(projectData.getConfiguration().getMultiFileMappingSeperator())), pair.getAnnotationType());
            for (int l = pair.getStartLine(); l <= pair.getEndLine(); l++) {
                String lineContent = fileLines.get(l - 1).replaceAll("[^a-zA-Z0-9\\s]", "");//remove non word characters except white spaces
                //skip all blank lines
                if (StringUtils.isBlank(lineContent)) {
                    continue;
                }
                Asset lineAsset = new Asset(l, String.format("%d", l), String.format("%s%s%d", fileAsset.getFullyQualifiedName(), projectData.getConfiguration().getFeatureQualifiedNameSeparator(), l), AssetType.LOC, fragmentAsset);
                lineAsset.setNloc(1);
                lineAsset.setFileRelativePath(fileAsset.getFileRelativePath());
                lineAsset.setAssetContent(lineContent);
                if (projectData.getConfiguration().isSaveDataInDataBase()) {
                    projectData.getDataController().addAsset(lineAsset, fragmentAsset.getFullyQualifiedName(), projectData.getConfiguration().getProjectRepository().getName());
                } else {
                    fragmentAsset.getChildren().add(lineAsset);
                    projectData.getAssetList().add(lineAsset);
                }
                mapAssetToFeatures(lineAsset, splitFeatures, pair.getAnnotationType());

            }
        }
    }

    private Asset addAnnotationAsAssetToFile(Asset fileAsset, AnnotationPair annotationPair, List<String> mappedFeatures) throws SQLException {
        Asset fragmentAsset = new Asset(annotationPair.getStartLine(), annotationPair.getEndLine(), annotationPair.getAnnotationType(), String.format("%d-%d", annotationPair.getStartLine(), annotationPair.getEndLine()), String.format("%s%s%d-%d", fileAsset.getFullyQualifiedName(), projectData.getConfiguration().getFeatureQualifiedNameSeparator(), annotationPair.getStartLine(), annotationPair.getEndLine()), AssetType.FRAGMENT, fileAsset);
        fragmentAsset.setNloc(annotationPair.getEndLine()- annotationPair.getStartLine()+1);
        fragmentAsset.setFileRelativePath(fileAsset.getFileRelativePath());
        if(projectData.getConfiguration().isSaveDataInDataBase()){
            projectData.getDataController().addAsset(fragmentAsset,fileAsset.getFullyQualifiedName(),projectData.getConfiguration().getProjectRepository().getName());
        }else {
            fileAsset.getChildren().add(fragmentAsset);
            projectData.getAssetList().add(fragmentAsset);
        }
        mapAssetToFeatures(fragmentAsset, mappedFeatures, annotationPair.getAnnotationType());
        return fragmentAsset;
    }

    private Asset createFileAsset(File file, String fileText,int nloc) throws IOException, SQLException {

        Asset fileAsset = projectData.getConfiguration().isSaveDataInDataBase()?projectData.addFileAssetToDB(file): projectData.addFileAsset(file);
        fileAsset.setNloc(nloc);
        //now add the content also as an asset
        if(fileText!=null) {
            fileAsset.setAssetContent(fileText.replaceAll("[^a-zA-Z0-9\\s]", ""));//remove non word characters except white spaces
        }

        return fileAsset;
    }

    private List<String>  getSplitListOfFeatures(List<String> featureNames){
        List<String> features = new ArrayList<>();
        for(String feature: featureNames){
            String[] splitFeatures = feature.split(projectData.getConfiguration().getMultipleFeatureRegex());
            for(String splitFeature:splitFeatures){
                features.add(splitFeature.trim());
            }
        }
        return features;
    }
    public void mapAssetToFeatures(Asset asset, List<String> features, AnnotationType annotationType) throws SQLException {
       // List<String> features = featureNames;//getSplitListOfFeatures(featureNames);
        int tangled = features.size()-1;
        for (String featureName : features) {
            if (StringUtils.isBlank(featureName.trim())) {
                continue;
            }
            if (!projectData.getFeatureNames().contains(featureName.trim())) {
                projectData.getFeatureNames().add(featureName.trim());
            }
            //FeatureTreeNode feature = projectData.addFeatureToList(featureName);
            //create mapping
            if(projectData.getConfiguration().isSaveDataInDataBase()){
                projectData.getDataController().mapAssetToFeature(featureName.trim(), asset.getFullyQualifiedName(), annotationType.toString(),projectData.getConfiguration().getProjectRepository().getName(),asset.getAssetType().name(),tangled);
            }else {
                FeatureAssetMap featureAssetMap = new FeatureAssetMap(featureName.trim(), asset, annotationType,tangled);
                if (!projectData.getAssetFeatureMap().contains(featureAssetMap)) {
                    projectData.getAssetFeatureMap().add(featureAssetMap);
                }
            }
        }

    }

    private void updateChangedAsset(String fileName, String oldFileName, String newRelativePath, ModificationType modificationType) {
        if (modificationType == ModificationType.RENAME) {
            projectData.getAssetChangedList().stream().filter(a -> a.getFileName().equalsIgnoreCase(oldFileName)).forEach(a -> {
                a.setFileName(fileName);
                a.setFileRelativePath(newRelativePath);
            });
        } else if (modificationType == ModificationType.DELETE) {
            projectData.getAssetChangedList().removeIf(a -> a.getFileName().equalsIgnoreCase(fileName));
        }
    }

    public void logChangedAssets(List<Modification> modifications, SCMRepository repository, String commitHash, String author) throws IOException {
        DiffParser diffParser;
        if (commitHash.equalsIgnoreCase("4407b3ef0aa1967ca981d46265f2581380ea0747")) {
           //System.out.println("check modifications here");
        }
        for (Modification modification : modifications) {
            String fileName = new File(String.format("%s/%s", repository.getPath(), modification.getFileName())).getAbsolutePath();
            String oldFileName = new File(String.format("%s/%s", repository.getPath(), modification.getOldPath())).getAbsolutePath();
            if(fileName.contains(projectData.getConfiguration().getFileMappingFile())||fileName.contains(projectData.getConfiguration().getFolderMappingFile())){
                continue;
            }
            if (modification.getType() == ModificationType.RENAME || modification.getType() == ModificationType.DELETE) {
                updateChangedAsset(fileName, oldFileName, modification.getFileName(), modification.getType());
                continue;
            }

            //projectData.getAssetChangedList().add(new AssetChanged(fileName, modification.getFileName(), -1, commitHash,author));//-1 is for main file
            String diff = modification.getDiff();
            if (diff.equalsIgnoreCase("-- TOO BIG --")) {
                continue;//skip diffs too big to process

            }
            diffParser = new DiffParser(diff);
            List<DiffLine> newDiffLines = new ArrayList<>();
            List<Integer> changedLines = new ArrayList<>();
            List<DiffBlock> blocks = diffParser.getBlocks();
            for (DiffBlock block : blocks) {
                if (StringUtils.isBlank(block.getDiffBlock())) {
                    continue;
                }
                newDiffLines.addAll(block.getLinesInNewFile());
            }

            //add the changed lines
            for (DiffLine diffLine : newDiffLines) {
                if (diffLine == null) {
                    continue;
                }
                if(diffLine.getType() != DiffLineType.REMOVED){
                    changedLines.add(diffLine.getLineNumber());
                }
                //projectData.getAssetChangedList().add(new AssetChanged(fileName, modification.getFileName(), diffLine.getLineNumber(), commitHash,author));
            }
            projectData.getAssetChangedList().add(new AssetChanged(fileName, modification.getFileName(), changedLines, commitHash,author));
        }
    }



    private void addUnlbaledAsset(Asset asset,Modification  modification){
        asset.setFileRelativePath(modification.getFileName());
        projectData.getUnlabeledAssetList().add(asset);
    }
    public void createUnlabeledAssets(List<Modification> modifications, SCMRepository repository) throws IOException {
        projectData.setUnlabeledAssetList(new ArrayList<>());
        DiffParser diffParser;

        for (Modification modification : modifications) {
            String fileName = new File(String.format("%s/%s", repository.getPath(), modification.getFileName())).getAbsolutePath();
            if(fileName.contains(projectData.getConfiguration().getFileMappingFile())||fileName.contains(projectData.getConfiguration().getFolderMappingFile())){
                continue;
            }
            if (modification.getType() == ModificationType.RENAME || modification.getType() == ModificationType.DELETE) {
                                continue;
            }

            //projectData.getAssetChangedList().add(new AssetChanged(fileName, modification.getFileName(), -1, commitHash,author));//-1 is for main file
            String diff = modification.getDiff();
            if (diff.equalsIgnoreCase("-- TOO BIG --")) {
                continue;//skip diffs too big to process

            }
            //create file asset
            Asset fileAsset = new Asset(fileName,fileName,AssetType.FILE);
            List<String> fileLines = FileUtils.readLines(new File(fileName),projectData.getConfiguration().getTextEncoding());
            fileAsset.setNloc(fileLines.size());
            addUnlbaledAsset(fileAsset,modification);

            diffParser = new DiffParser(diff);
            List<DiffBlock> blocks = diffParser.getBlocks();
            for (DiffBlock block : blocks) {
                if (StringUtils.isBlank(block.getDiffBlock())) {
                    continue;
                }
                //add fragment asset
                List<DiffLine> blockLines = block.getLinesInNewFile().stream().filter(l->l!=null&&l.getType()!= DiffLineType.REMOVED).collect(Collectors.toList());
                int startLine = blockLines.get(0).getLineNumber();
                int endLine = blockLines.get(blockLines.size()-1).getLineNumber();

                Asset fragmentAsset = new Asset(startLine,endLine , AnnotationType.FRAGMENT, String.format("%d-%d", startLine, endLine), String.format("%s%s%d-%d", fileAsset.getFullyQualifiedName(), projectData.getConfiguration().getFeatureQualifiedNameSeparator(), startLine, endLine), AssetType.FRAGMENT, fileAsset);
                fragmentAsset.setNloc(endLine-startLine+1);
                fileAsset.getChildren().add(fragmentAsset);
                addUnlbaledAsset(fragmentAsset,modification);
                for(DiffLine line:blockLines){

                    Asset lineAsset = new Asset(line.getLineNumber(), String.format("%d", line.getLineNumber()), String.format("%s%s%d", fileAsset.getFullyQualifiedName(), projectData.getConfiguration().getFeatureQualifiedNameSeparator(), line.getLineNumber()), AssetType.LOC, fragmentAsset);
                    lineAsset.setNloc(1);
                    fragmentAsset.getChildren().add(lineAsset);
                    addUnlbaledAsset(lineAsset,modification);

                }
            }


        }
    }
public void deleteAssetFromDatabase(String fileName) throws SQLException {
        projectData.getDataController().deleteAssetAndChildren(fileName);
}
    public void resetChildren(String fileName, ModificationType type) {
//        Set<String> keysForFile = projectData.getMlDataSet().keySet().stream().filter(k->k.contains(fileName)).collect(Collectors.toSet());
//        for(String key:keysForFile){
//            projectData.getMlDataSet().remove(key);
//        }
        //remove all existing mappings
        if(type == ModificationType.DELETE) {
            projectData.getAssetFeatureMap().removeIf(a -> a.getMappedAsset().getFullyQualifiedName().contains(fileName));
        }else {
            projectData.getAssetFeatureMap().removeIf(a -> (a.getMappedAsset().getAssetType()== AssetType.FRAGMENT|| a.getMappedAsset().getAssetType()== AssetType.LOC) && a.getMappedAsset().getFullyQualifiedName().contains(fileName));
        }
        //projectData.getMlData().removeIf(a -> a.getAssetName().contains(fileName));
        Optional<Asset> fileAsset = projectData.getAssetList().stream().filter(a -> a.getFullyQualifiedName().equalsIgnoreCase(fileName)).findAny();
        projectData.getAssetList().removeIf(a -> a.getFullyQualifiedName().contains(fileName));
        //projectData.getAssetChangedList().removeIf(a -> a.getAsset().getFullyQualifiedName().contains(fileName));
        if (fileAsset.isPresent()) {
            if(fileAsset.get().getParent()!=null) {
                fileAsset.get().getParent().getChildren().removeIf(a -> a.getFullyQualifiedName().equalsIgnoreCase(fileName));
            }
        }
        fileAsset = null;
        //remove any features that have no mappings
        List<String> mappedFeatures = projectData.getAssetFeatureMap().parallelStream().map(FeatureAssetMap::getFeatureName).distinct().collect(Collectors.toList());
        projectData.getFeatureNames().removeIf(f -> !mappedFeatures.contains(f));

    }
    public void createFileAssetsFromRepository(String currentCommit) {
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
                AssetGenerator gen = new AssetGenerator(new File(filePath), projectData, this,currentCommit);
                Utilities.createFuture(gen, executionRunner);

                //createFileAssetsForMetrics(new File(filePath));==OSBOLTE NOW USING THEADED ONE

            }
            executionRunner.waitForTaskToFinish();
            executionRunner.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
