package se.gu.main;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.repodriller.domain.Commit;
import org.repodriller.domain.Modification;
import org.repodriller.domain.ModificationType;
import org.repodriller.persistence.PersistenceMechanism;
import org.repodriller.scm.CommitVisitor;
import org.repodriller.scm.SCMRepository;
import se.gu.ml.preprocessing.FileMetricCalculator;
import se.gu.utils.Utilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MetricCommitVisitor implements CommitVisitor {
    private ProjectData projectData;
    FeatureAssetMapper assetMapper;
    private int count = -1;
    File diffFilesDirectory;
    private int previousCommitIndex = -1;
    private String previousCommit;
    private int commitCount;
    List<String> commitList;

    public MetricCommitVisitor(ProjectData projectData, int commits, List<String> commitList) {
        this.projectData = projectData;
        commitCount = commits;
        this.commitList = commitList;

        assetMapper = new FeatureAssetMapper(projectData);
        try {
            diffFilesDirectory = Utilities.createOutputDirectory(String.format("%s/%s/%s/%s", projectData.getConfiguration().getAnalysisDirectory(), projectData.getConfiguration().getCodeAbstractionLevel(), projectData.getConfiguration().getProjectRepository().getName(), "diffFiles"), true);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void process(SCMRepository repository, Commit commit, PersistenceMechanism persistenceMechanism) {
        try {
            doWork(repository, commit, persistenceMechanism);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void doWork(SCMRepository repository, Commit commit, PersistenceMechanism persistenceMechanism) throws SQLException, IOException, InterruptedException {
        boolean shouldCheckoutRepo = false;
        try {
            if (projectData.getConfiguration().getProjectRepository().getName().equalsIgnoreCase("marlin") && commit.getCommitterDate().getWeekYear() < 2017) {
                return;
            }
            if (!commitList.contains(commit.getHash())) {
                return;
            }


            projectData.setNestingDepthPairs(null);
            projectData.setNestingDepthPairs(new CopyOnWriteArrayList<>());//&line [Metrics]
            projectData.setChangedAssetsInCurrentCommit(new CopyOnWriteArrayList<>());
            previousCommitIndex = count;
            count++;

            if (count == 0) {
                //reset repo before checkout anything
                shouldCheckoutRepo=true;
                System.out.println("===CHCKING OUT REPO HERE===");
                repository.getScm().checkout(commit.getHash());
                assetMapper.createFileAssetsFromRepository(null);//no need to check out individual files

            } else {
                //reset repo before checkout anything
                if (commit.getHash().equalsIgnoreCase("8167c084d933cdd26895547ae721188a6ad99625")) {
                    System.out.println("STOP HERE");
                }
                List<String> allowedExtensions = Arrays.asList(projectData.getConfiguration().getAllowedFileExtensions().split(",")).parallelStream().map(e -> e.replace(".", "")).collect(Collectors.toList());

                //get modifications (modified files)
                List<Modification> modifications = commit.getModifications()
                        .parallelStream().filter(m -> allowedExtensions.contains(FilenameUtils.getExtension(m.getFileName()))).collect(Collectors.toList());
                //now checkout repo at this revision
                //repository.getScm().checkout(commit.getHash());
                //check if atleast one of the modified files is issing from repo then check it out for this commit

                for (Modification modification : modifications) {

                    File file = new File(String.format("%s/%s", repository.getPath(), modification.getFileName()));
                    if (!file.exists()) {
                        System.out.println("===WILL CHECKOUT OUT REPO BECAUSE FILE DOES NOT EXIST"+file.getAbsolutePath());
                        shouldCheckoutRepo = true;
                        break;
                    }

                }
                List<String> filePaths = new ArrayList<>();
                //add all paths
                for (Modification modification : modifications) {

                    String fileName = new File(String.format("%s/%s", repository.getPath(), modification.getFileName())).getAbsolutePath();
                    filePaths.add(fileName);
                    System.out.println(fileName);

                }

                //now add these assets
                if(shouldCheckoutRepo){
                    System.out.println("===CHCKING OUT REPO HERE===");
                    repository.getScm().checkout(commit.getHash());
                    assetMapper.generateAssetsFromPaths(filePaths, null);//don't check out per commit that is why commit is null
                }else {
                    assetMapper.generateAssetsFromPaths(filePaths, commit.getHash());//check out individual files only
                }
            }
            //now do metrics
            if (projectData.getConfiguration().isCalculateMetrics()) {
                //LocalExecutionRunner metricsExecutorService = new LocalExecutionRunner();
                if (!projectData.getConfiguration().isSaveDataInDataBase()) {
                    if (projectData.getAssetFeatureMap().size() > 0) {
                        FileMetricCalculator fileMetricCalculator = new FileMetricCalculator(projectData, new se.gu.git.Commit(commit.getHash(), commit.getHash(), null, projectData.getConfiguration().getProjectRepository()), commitCount + 1, null, null);
                        fileMetricCalculator.calculateMetrics();
                    }
                } else {
                    FileMetricCalculator fileMetricCalculator = new FileMetricCalculator(projectData, new se.gu.git.Commit(commit.getHash(), commit.getHash(), null, projectData.getConfiguration().getProjectRepository()), commitCount + 1, null, null);
                    fileMetricCalculator.calculateMetrics();
                }


            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if(shouldCheckoutRepo) {
                System.out.println("===RESETTING REPO HERE===");
                //repository.getScm().reset();//reset if repo was checkedout
            }
            System.out.printf("Commit %d/%d\n", count + 1, commitCount);
        }

    }


}
