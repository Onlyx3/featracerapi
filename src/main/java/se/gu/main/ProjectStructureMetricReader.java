package se.gu.main;

import org.repodriller.RepositoryMining;
import org.repodriller.Study;
import org.repodriller.filter.range.Commits;
import org.repodriller.scm.GitRepository;
import se.gu.git.Commit;
import se.gu.git.DiffExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectStructureMetricReader  implements Study {
    private ProjectData projectData;

    public ProjectStructureMetricReader(ProjectData projectData) {
        this.projectData = projectData;

    }


    @Override
    public void execute() {
        //&begin[StructureMetrics]
        List<String> commitHashes = new ArrayList<>(),allCommitList=new ArrayList<>();
        List<Commit> commits = new ArrayList<>();
        int commitCount=0;
        try {
            DiffExtractor diffExtractor = new DiffExtractor(projectData.getConfiguration());
            diffExtractor.setCommitHistory();
            commits = diffExtractor.getCommitHistory();
            allCommitList = commits.parallelStream().map(Commit::getCommitHash).collect(Collectors.toList());
            //if starting from a certain index, then skip some commits
            int start = projectData.getConfiguration().getStartingCommitIndex();
            for(int i=start;i<allCommitList.size();i++){
                    commitHashes.add(allCommitList.get(i));
            }
            commitCount = commitHashes.size();
        } catch (Exception e) {
            e.printStackTrace();
        }
//&end[StructureMetrics]


        try {
            new RepositoryMining()
                    //.setRepoTmpDir(Paths.get(projectData.getConfiguration().getAnalysisDirectory().toURI()))
                    .in(GitRepository.singleProject(projectData.getConfiguration().getProjectRepository().getAbsolutePath()))
                    //.through(Commits.single("4407b3ef0aa1967ca981d46265f2581380ea0747"))
                    .through(Commits.list(commitHashes))//("b729c5def97e9e6b6adb25b631721d2255eb6792","ec55b65152f5819d45ff0ae320ea98b6fedfde21"))
                    //.through(Commits.all())
                    //.collect(new CollectConfiguration().diffs(new OnlyDiffsWithFileTypes(allowedExtensions)))
                    //.filters(new OnlyModificationsWithFileTypes(allowedExtensions))
//                    .visitorsAreThreadSafe(true) // Threads are possible.
//                    .visitorsChangeRepoState(true) // Each thread needs its own copy of the repo.
//                    .withThreads(5) // Now pick a good number of threads for my machine.

                    .process(new MetricCommitVisitor(projectData,commitCount,commitHashes))
                    //.process(new ProjectDBVisitor(projectData,commitCount,commitHashes),new CSVFile(csvFile))
                    .mine();


        }  catch (Exception e) {
            e.printStackTrace();
        }


    }
}