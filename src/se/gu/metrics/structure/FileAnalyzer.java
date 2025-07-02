package se.gu.metrics.structure;

import org.apache.commons.io.FileUtils;
import se.gu.main.FeatureAssetMapper;
import se.gu.main.ProjectData;
import se.gu.utils.Utilities;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

public class FileAnalyzer implements Runnable, Serializable {
    private ProjectData projectData;
    private FeatureAssetMapper assetMapper;
    private String fileName,fileRelativePath,repoPath;

    public FileAnalyzer(ProjectData projectData, FeatureAssetMapper assetMapper, String fileName, String fileRelativePath, String repoPath) {
        this.projectData = projectData;
        this.assetMapper = assetMapper;
        this.fileName = fileName;
        this.fileRelativePath = fileRelativePath;
        this.repoPath = repoPath;
    }

    @Override
    public void run() {
        try {
            addFileAsset();
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }
    public void addFileAsset() throws IOException, SQLException {

        if (fileName.contains(projectData.getConfiguration().getFolderMappingFile())) {
            assetMapper.mapFolder(fileName);
        } else if (fileName.contains(projectData.getConfiguration().getFileMappingFile())) {
            assetMapper.mapFiles(fileName,repoPath);
        }else {
            //map fragments and lines of code
            String fileText = FileUtils.readFileToString(new File(fileName), projectData.getConfiguration().getTextEncoding());
            //boolean fileContainsAnnotations = Utilities.isFileContainsAnnotations(fileText,projectData.getConfiguration().getAllAnnotationPatterns());
           // if (fileContainsAnnotations) {
                List<String> fileLines = FileUtils.readLines(new File(fileName), projectData.getConfiguration().getTextEncoding());
                assetMapper.mapFragments(fileName,fileRelativePath, fileText, fileLines);
                System.out.printf("Mapped file: %s\n",fileName);
           // }
        }
    }
}
