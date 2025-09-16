package se.gu.main;

import org.repodriller.RepositoryMining;
import org.repodriller.Study;
import org.repodriller.filter.range.Commits;
import org.repodriller.persistence.csv.CSVFile;
import org.repodriller.scm.GitRepository;
import se.gu.data.DataController;
import se.gu.git.Commit;
import se.gu.git.DiffExtractor;

import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectReaderWithDriller implements Study {
    public ProjectReaderWithDriller(ProjectData projectData) {
        this.projectData = projectData;
    }

    private ProjectData projectData;

    @Override
    public void execute() {
        Arrays.asList(projectData.getConfiguration().getAllowedFileExtensions().split(",")).parallelStream().map(e -> e.replace(".", "")).collect(Collectors.toList());
        String csvFile = String.format("%s/%s_projectDataCounts.csv", projectData.getConfiguration().getAnalysisDirectory(), projectData.getConfiguration().getProjectRepository().getName());

        List<String> commitHashes = new ArrayList<>();
        List<Commit> commits = new ArrayList<>();
        int commitCount=0;


        try {
            DiffExtractor diffExtractor = new DiffExtractor(projectData.getConfiguration());
            diffExtractor.setCommitHistory();
            commits = diffExtractor.getCommitHistory();
            commitHashes = commits.parallelStream().map(Commit::getCommitHash).collect(Collectors.toList());
            commitCount = commitHashes.size();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        DataController dataController;
        try {
            dataController = new DataController(projectData.getConfiguration());
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Repodriller tries to mine: " + projectData.getConfiguration().getProjectRepository().getAbsolutePath());
        System.out.println("Repodriller tries to create tmp dir in: " + projectData.getConfiguration().getAnalysisDirectory().toURI());
        try {
            new RepositoryMining()
                    .setRepoTmpDir(Paths.get(projectData.getConfiguration().getAnalysisDirectory().toURI()))
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
                    //.process(new CommitPolicy(projectData),new CSVFile(modificationsCSV))//==USED FOR COMMIT PRATICES
                    //.process(new MetricCommitVisitor(projectData,commitHashes.size(),commitHashes))
                    .process(new ProjectDBVisitor(projectData,commitCount,commitHashes, dataController),new CSVFile(csvFile))
                    .mine();


        }  catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                dataController.closeConnection();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
