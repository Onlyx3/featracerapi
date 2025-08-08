package se.gu.main;

import org.repodriller.RepositoryMining;
import org.repodriller.Study;
import org.repodriller.filter.range.Commits;
import org.repodriller.persistence.csv.CSVFile;
import org.repodriller.scm.GitRepository;
import se.gu.data.DataController;
import se.gu.git.Commit;
import se.gu.git.DiffExtractor;

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
        List<String> allowedExtensions = Arrays.asList(projectData.getConfiguration().getAllowedFileExtensions().split(",")).parallelStream().map(e -> e.replace(".", "")).collect(Collectors.toList());
        String csvFile = String.format("%s/%s_projectDataCounts.csv", projectData.getConfiguration().getAnalysisDirectory(), projectData.getConfiguration().getProjectRepository().getName());
        //String commitSummaryCSV = String.format("%s/%s_commitSummary.csv", projectData.getConfiguration().getAnalysisDirectory(), projectData.getConfiguration().getProjectRepository().getName());
        //&begin[CommitPracticesFeatRacer]
//       String modificationsCSV = String.format("%s/%s_modifications.csv", projectData.getConfiguration().getAnalysisDirectory(), projectData.getConfiguration().getProjectShortName());
//
//
//        try {
//            File modFIle = new File(modificationsCSV);
//            if(modFIle.exists()){
//                FileUtils.forceDelete(modFIle);
//            }
//            PrintWriter writer  = new PrintWriter(new FileWriter(modificationsCSV,true));
//            writer.println("project;commit;tLOCA;tLOCR;aLOCA;aLOCR;mLOCA;mLOCR;tFC;tFA;tFM;tFD;tFRe;tChurn;aChurn;tHunk;aHunk;mHunkSize;aHunkSize;naa;nua;raa;rua;nafo;nafi;nafra;nal");
//            writer.close();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//&end[CommitPracticesFeatRacer]

        List<String> commitHashes = new ArrayList<>();
        List<Commit> commits = new ArrayList<>();
        String projectName = projectData.getConfiguration().getProjectRepository().getName();
        int commitCount=0;
//        try {
//            DiffExtractor diffExtractor = new DiffExtractor(projectData.getConfiguration());
//            commitHashes = diffExtractor.getCSVCommitLines();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        //List<File> projectRepositories = projectData.getConfiguration().getCopiedGitRepositories();
        //int indexOfCurrentProject = projectRepositories.indexOf(projectData.getConfiguration().getProjectRepository());
        //List<File> commitCSVs = projectData.getConfiguration().getCommitsForFeatRacerExperiment();
        //File projectCommitCSV = commitCSVs.get(indexOfCurrentProject);
//        if(projectCommitCSV.exists()){
//            try {
//                commitHashes = FileUtils.readLines(projectCommitCSV,projectData.getConfiguration().getTextEncoding());
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
        try {
            DiffExtractor diffExtractor = new DiffExtractor(projectData.getConfiguration());
            diffExtractor.setCommitHistory();
            commits = diffExtractor.getCommitHistory();
            commitHashes = commits.parallelStream().map(Commit::getCommitHash).collect(Collectors.toList());
            commitCount = commitHashes.size();
        } catch (Exception e) {
            e.printStackTrace();
        }
//        System.out.println(commitHashes);
        DataController dataController;
        try {
            dataController = new DataController(projectData.getConfiguration());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
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
                    //.process(new CommitPolicy(projectData),new CSVFile(modificationsCSV))//==USED FOR COMMIT PRATICES
                    //.process(new MetricCommitVisitor(projectData,commitHashes.size(),commitHashes))
                    .process(new ProjectDBVisitor(projectData,commitCount,commitHashes, dataController),new CSVFile(csvFile))
                    .mine();

            //update commit indexs
//            DataController dc = new DataController(projectData.getConfiguration());
//            for(int i=0;i<commits.size();i++){
//                dc.updateCommitIndex(commits.get(i).getCommitHash(),i+1,projectName );
//            }

        }  catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                dataController.closeConnection();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
    }
}
