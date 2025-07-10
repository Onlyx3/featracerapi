package se.gu.defectpred;

import org.apache.commons.io.FileUtils;
import se.gu.main.Configuration;
import se.gu.utils.Utilities;

import java.io.File;
import java.io.IOException;

/**
 * THis calss pairs projects together for clone detection.
 */
public class ProjectGrouper {
    public ProjectGrouper(Configuration configuration) {
        this.configuration = configuration;
    }

    private Configuration configuration;

    public void groupProjects() throws IOException {
        String clonesFolder = "C:/studies/defectprediction/clones";
        Utilities.createOutputDirectory(clonesFolder, false);
        File[] projectPaths = configuration.getClonedRepositories();
        String[] projectNameList = configuration.getProjectNamesList();
        for (int i = 0; i < projectNameList.length; i++) {
            for (int j = i + 1; j < projectNameList.length; j++) {
                String pair = String.format("%s_%s", projectNameList[i], projectNameList[j]);
                File targetFolder = null;
                try {
                    //copy project at i and at j in the target folder
                    targetFolder = new File(String.format("%s/%s/%s", clonesFolder, pair,projectNameList[i]));
                    System.out.println(String.format("Copying %s into %s", projectPaths[i], targetFolder));
                    FileUtils.copyDirectory(projectPaths[i], targetFolder);
                    System.out.println(String.format("DONE Copying %s into %s", projectPaths[i], targetFolder));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                try {
                    System.out.println(String.format("Copying %s into %s", projectPaths[j], targetFolder));
                    targetFolder = new File(String.format("%s/%s/%s", clonesFolder, pair,projectNameList[j]));
                    FileUtils.copyDirectory(projectPaths[j], targetFolder);
                    System.out.println(String.format("DONE Copying %s into %s", projectPaths[j], targetFolder));

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
