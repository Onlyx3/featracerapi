package se.gu.main;

import org.apache.commons.io.FileUtils;
import org.repodriller.RepositoryMining;
import org.repodriller.Study;
import org.repodriller.filter.range.Commits;
import org.repodriller.persistence.csv.CSVFile;
import org.repodriller.scm.GitRepository;
import se.gu.git.Commit;
import se.gu.git.CommitPolicy;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectCommitMetricReader implements Study {
    private ProjectData projectData;

    public ProjectCommitMetricReader(ProjectData projectData) {
        this.projectData = projectData;
    }


    @Override
    public void execute() {
               //&begin[CommitPracticesFeatRacer]
        //THIS IS OLD WHEN WE SAVED ReSULTS IN CSV, we NOW USE the database
        String modificationsCSV = String.format("%s/%s_modifications.csv", projectData.getConfiguration().getAnalysisDirectory(), projectData.getConfiguration().getProjectShortName());


        try {
            File modFIle = new File(modificationsCSV);
            if(modFIle.exists()){
                FileUtils.forceDelete(modFIle);
            }
            PrintWriter writer  = new PrintWriter(new FileWriter(modificationsCSV,true));
            writer.println("project;commit;tLOCA;tLOCR;aLOCA;aLOCR;mLOCA;mLOCR;tFC;tFA;tFM;tFD;tFRe;tChurn;aChurn;tHunk;aHunk;mHunkSize;aHunkSize;naa;nua;raa;rua;nafo;nafi;nafra;nal");
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
//&end[CommitPracticesFeatRacer]


        try {
            new RepositoryMining()
                    //.setRepoTmpDir(Paths.get(projectData.getConfiguration().getAnalysisDirectory().toURI()))
                    .in(GitRepository.singleProject(projectData.getConfiguration().getProjectRepository().getAbsolutePath()))
                    //.through(Commits.single("4407b3ef0aa1967ca981d46265f2581380ea0747"))
                    //.through(Commits.list(commitHashes))//("b729c5def97e9e6b6adb25b631721d2255eb6792","ec55b65152f5819d45ff0ae320ea98b6fedfde21"))
                    .through(Commits.all())
                    //.collect(new CollectConfiguration().diffs(new OnlyDiffsWithFileTypes(allowedExtensions)))
                    //.filters(new OnlyModificationsWithFileTypes(allowedExtensions))
                    //.visitorsAreThreadSafe(true) // Threads are possible.
                    //.visitorsChangeRepoState(true) // Each thread needs its own copy of the repo.
                    //.withThreads(1) // Now pick a good number of threads for my machine.
                    //.process(new ProjectDataVisitor(projectData,commitHashes.size()), new CSVFile(csvFile))
                    //.process(new CommitSummaryVisitor(projectData),new CSVFile(commitSummaryCSV))
                    .process(new CommitPolicy(projectData),new CSVFile(modificationsCSV))//==USED FOR COMMIT PRATICES
                    //.process(new MetricCommitVisitor(projectData,commitHashes.size(),commitHashes))
                    //.process(new ProjectDBVisitor(projectData,commitCount,commitHashes),new CSVFile(csvFile))
                    .mine();


        }  catch (Exception e) {
            e.printStackTrace();
        }


    }
}
