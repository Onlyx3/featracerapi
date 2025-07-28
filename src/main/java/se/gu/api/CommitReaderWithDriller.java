package se.gu.api;

import org.repodriller.RepositoryMining;
import org.repodriller.Study;
import org.repodriller.filter.range.Commits;
import org.repodriller.persistence.csv.CSVFile;
import org.repodriller.scm.GitRepository;
import se.gu.main.ProjectDBVisitor;
import se.gu.main.ProjectData;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CommitReaderWithDriller implements Study {

    private ProjectData projectData;
    private String commitHash;

    public CommitReaderWithDriller(ProjectData projectData,  String commitHash) {
        this.projectData = projectData;
        this.commitHash = commitHash;
    }

    @Override
    public void execute() {
        String csvFile = String.format("%s/%s_projectDataCounts.csv", projectData.getConfiguration().getAnalysisDirectory(), projectData.getConfiguration().getProjectRepository().getName());

        List<String> commitHashes = new ArrayList<>();
        commitHashes.add(commitHash);

        try {
            new RepositoryMining()
                    .in(GitRepository.singleProject(projectData.getConfiguration().getProjectRepository().getAbsolutePath()))
                    .through(Commits.single(commitHash))
                    .process(new ProjectDBVisitor(projectData,1,commitHashes),new CSVFile(csvFile)) // Assumption: ProjectDBVisitor works as it is
                    .mine();
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
