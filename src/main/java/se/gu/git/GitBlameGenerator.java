package se.gu.git;

import se.gu.assets.Asset;
import se.gu.assets.AssetType;
import se.gu.main.ProjectData;
import se.gu.utils.CommandRunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class GitBlameGenerator implements Serializable {


    private static final long serialVersionUID = 5273711436137269141L;

    public GitBlameGenerator(ProjectData projectData) {

        this.projectData = projectData;
    }

    private ProjectData projectData;



    public void createGitBlameEntries(){
        CommandRunner commandRunner = new CommandRunner(projectData.getConfiguration());
        List<Asset> allChangedFiles = projectData.getChangedAssetsByType(AssetType.FILE);
        for(Asset changedFile:allChangedFiles){
            //get blame
            try {

                BufferedReader blameOutput = commandRunner.getGitBlame(changedFile.getFullyQualifiedName());
                System.out.println("GIT BLAME FOR FILE "+changedFile.getFullyQualifiedName());
                String line;
                while((line=blameOutput.readLine())!=null){
                    System.out.println(line);
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }
}
