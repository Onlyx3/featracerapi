package se.gu.api;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.repodriller.RepositoryMining;
import org.repodriller.Study;
import org.repodriller.filter.range.Commits;
import org.repodriller.persistence.csv.CSVFile;
import org.repodriller.scm.GitRepository;
import se.gu.data.DataController;
import se.gu.main.ProjectDBVisitor;
import se.gu.main.ProjectData;

import java.io.File;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommitReaderWithDriller implements Study {

    private ProjectData projectData;
    private String commitHash;

    public CommitReaderWithDriller(ProjectData projectData,  String commitHash) {
        this.projectData = projectData;
        this.commitHash = commitHash;
    }

    @Override
    public void execute() {
        Arrays.asList(projectData.getConfiguration().getAllowedFileExtensions().split(",")).parallelStream().map(e -> e.replace(".", "")).collect(Collectors.toList());
        String csvFile = String.format("%s/%s_projectDataCounts.csv", projectData.getConfiguration().getAnalysisDirectory(), projectData.getConfiguration().getProjectRepository().getName());

        List<String> commitHashes = new ArrayList<>();
        commitHashes.add(commitHash);

        DataController dataController;
        try {
            dataController = new DataController(projectData.getConfiguration());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        String repoPath = projectData.getConfiguration().getProjectRepository().getAbsolutePath();
        int commitIndex = getCommitIndex(repoPath, commitHash);

        System.out.println("DEBUG: Attempting to find commit hash: " + commitHash);
        System.out.println("DEBUG: Inside repository path: " + repoPath);
        try {
            new RepositoryMining()
                    .setRepoTmpDir(Paths.get(projectData.getConfiguration().getAnalysisDirectory().toURI()))
                    .in(GitRepository.singleProject(projectData.getConfiguration().getProjectRepository().getAbsolutePath()))
                    .through(Commits.single(commitHash))
                    .process(new ProjectDBVisitorSingle(projectData,commitIndex,commitHashes, dataController),new CSVFile(csvFile))
                    .mine();
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }finally {
            try {
                dataController.closeConnection();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
    }

    private int getCommitIndex(String repoPath, String commitHash) {
        int c = 0;
        try(Git git = Git.open(new File(repoPath))) {
            ObjectId commitId = git.getRepository().resolve(commitHash);
            if(commitId == null) {
                throw new IllegalArgumentException("Invalid commit hash: " + commitHash);
            }
            RevWalk revWalk = new RevWalk(git.getRepository());
            RevCommit revCommit = revWalk.parseCommit(commitId);
            revWalk.markStart(revCommit);
            for(RevCommit rev : revWalk) c++;

        } catch (Exception e) {
            throw new RuntimeException("Failed to get commit index from Git", e);
        }
        System.out.println("DEBUG: CommitIndex: " + c);
        return c;
    }
}
