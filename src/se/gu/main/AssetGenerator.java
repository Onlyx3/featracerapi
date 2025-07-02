package se.gu.main;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import se.gu.assets.AnnotationType;
import se.gu.assets.Asset;
import se.gu.assets.AssetType;
import se.gu.assets.FeatureAssetMap;
import se.gu.parser.fmparser.FeatureTreeNode;
import se.gu.reader.AnnotationPair;
import se.gu.reader.AnnotationReader;
import se.gu.utils.CommandRunner;
import se.gu.utils.Utilities;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AssetGenerator implements Runnable{
    public AssetGenerator(File file, ProjectData projectData, FeatureAssetMapper assetMapper,String currentCommit) {
        this.file = file;
        this.projectData = projectData;
        this.assetMapper = assetMapper;
        this.currentCommit = currentCommit;
    }

    private File file;
    private ProjectData projectData;
    private FeatureAssetMapper assetMapper;
    private String currentCommit;
    @Override
    public void run() {
        try {
            if (file.getName().contains(projectData.getConfiguration().getFolderMappingFile())) {
                assetMapper.mapFolder(file.getAbsolutePath());
            } else if (file.getName().contains(projectData.getConfiguration().getFileMappingFile())) {
                assetMapper.mapFiles(file.getAbsolutePath(),projectData.getConfiguration().getProjectRepository().getAbsolutePath());

            } else {
                //check out file at current commit
                String relativeFileName = file.getAbsolutePath().replace("\\", "/").replace(projectData.getConfiguration().getProjectRepository().getAbsolutePath().replace("\\", "/") + "/", "");
//chekc out this file
                if(currentCommit!=null) {//if commit is supplied then checkout file at this commit
                    CommandRunner runner = new CommandRunner(projectData.getConfiguration());
                    runner.checkOutFileAtCommit(currentCommit, relativeFileName);
                }
                //Asset fileAsset = createFileAndSubAssets(file, false);
                List<String> lines = FileUtils.readLines(file, projectData.getConfiguration().getTextEncoding());
                String fileText = projectData.getConfiguration().isCalculateMetrics()?null: FileUtils.readFileToString(file,projectData.getConfiguration().getTextEncoding());
                String relativePath = file.getPath();
                assetMapper.mapFragments(file.getAbsolutePath(),relativePath,fileText,lines);

            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }








}
