package se.gu.main;

import com.google.common.base.Stopwatch;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.io.FileUtils;
import se.gu.assets.AssetDB;
import se.gu.assets.AssetMappingDB;
import se.gu.data.DataController;
import se.gu.git.Commit;
import se.gu.lucene.LuceneIndexerDB;
import se.gu.utils.CommandRunner;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class LuceneRunner {
    public LuceneRunner(ProjectData projectData) {
        this.projectData = projectData;
        this.configuration = projectData.getConfiguration();
    }

    private ProjectData projectData;
    Configuration configuration;
    private String currentCommit;
    private DataController dataController;

    /**
     * We have to run Lucene for the same commits that we have metrics for in the FeatRacer database
     * @throws Exception
     */
    public void runLucene() throws Exception {

        dataController = new DataController(projectData.getConfiguration());
        String projectName = projectData.getConfiguration().getProjectRepository().getName();
        int startingCommitIndex = projectData.getConfiguration().getStartingCommitIndex();
        int commitsToRun = projectData.getConfiguration().getCommitsToExecute();
        //get all commits
        List<Commit> commits = dataController.getAllCommits(projectName);
        commitsToRun = commitsToRun == 0 ? commits.size() : commitsToRun;

        List<AssetDB> allAssetsForProject = dataController.getAssetsForProject(projectName);
        List<AssetMappingDB> assetMappingsForProject = dataController.getAssetMappingsForProject(projectName);

        CommandRunner commandRunner = new CommandRunner(configuration);
        int commitsRun = 0;

        int lastRunCommitIndex = configuration.getStartingCommitIndex();

        try (ProgressBar pb = new ProgressBar("ML DATA:", commitsToRun)) {

            for (Commit commit : commits) {
                if (commit.getCommitIndex() < startingCommitIndex) {
                    pb.step();
                    continue;
                }
                if (commitsRun > commitsToRun) {
                    break;
                }
                Stopwatch stopwatch = Stopwatch.createStarted();
                pb.step();
                currentCommit = commit.getCommitHash();
                pb.setExtraMessage(currentCommit);
                //check out project at this commit to index assets
                commandRunner.checkOutCommit(currentCommit);
                List<AssetDB> allassetsAtCommit = allAssetsForProject.parallelStream().filter(a->a.getCommitIndex()<=commit.getCommitIndex()).collect(Collectors.toList());
                List<AssetMappingDB> assetmappingsAtCommit = assetMappingsForProject.parallelStream().filter(a->a.getCommitIndex()<=commit.getCommitIndex()).collect(Collectors.toList());
                List<String> featureNames = assetmappingsAtCommit.parallelStream().map(AssetMappingDB::getFeaturename).distinct().collect(Collectors.toList());
                List<String> mappedAssetNames = assetmappingsAtCommit.parallelStream().map(AssetMappingDB::getAssetfullname).distinct().collect(Collectors.toList());
                List<AssetDB> mappedAssets  = allassetsAtCommit.parallelStream().filter(a->mappedAssetNames.contains(a.getAssetFullName())).collect(Collectors.toList());

                LuceneIndexerDB luceneIndexer = new LuceneIndexerDB(projectData, commit.getCommitIndex(), currentCommit,featureNames,mappedAssets);
                luceneIndexer.indexAssetsForLucene();
                luceneIndexer.searchAssets();
                stopwatch.stop();
                System.out.printf("\ntime for processing commit %s is %s seconds\n", currentCommit, stopwatch.elapsed(TimeUnit.SECONDS));
            }
        }
    }


    public  void combineLuceneData() throws IOException {
        File analysisDirectory = projectData.getConfiguration().getAnalysisDirectory();
        String luceneFolder = String.format("%s/lucene",analysisDirectory);

        String[] projects = projectData.getConfiguration().getProjectNamesList();
        String[] shortNames = projectData.getConfiguration().getProjectShortNameList();
        String targetFile = String.format("%s/luceneCombinedSummary.csv",projectData.getConfiguration().getrDataFolder());
        File tFile = new File(targetFile);
        if (tFile.exists()) {
            FileUtils.forceDelete(tFile);
        }
        PrintWriter writer = new PrintWriter(new FileWriter(tFile, true));
        System.out.println("COMBINED LUCENE DATA");
        try {
            //header
            writer.println("project;feature;mappedAssets;foundAssets;macthedAssets;precision;recall;fscore;commitIndex;commit");
            for (int p = 0; p < projects.length; p++) {
                File projectSumaryFile = new File(String.format("%s/%s/resultSummary.csv",luceneFolder,projects[p]));
                if(!projectSumaryFile.exists()){
                    continue;
                }
                System.out.println(projectSumaryFile);
                List<String> fileLines = FileUtils.readLines(projectSumaryFile, "UTF-8");
                for (int i = 1; i < fileLines.size(); i++) {
                    String[] parts = fileLines.get(i).split(";");
                    String project = shortNames[p], feature = parts[0], mappedAssets = parts[1], foundAssets = parts[2], macthedAssets = parts[3], precision = parts[4], recall = parts[5], fscore = parts[6], commitIndex = parts[7], commit = parts[8];
                    precision = precision.trim().equalsIgnoreCase("NaN") ? "0" : precision;
                    recall = recall.trim().equalsIgnoreCase("NaN") ? "0" : recall;
                    fscore = fscore.trim().equalsIgnoreCase("NaN") ? "0" : fscore;
                    writer.printf("%s;%s;%s;%s;%s;%s;%s;%s;%s;%s\n", project, feature, mappedAssets, foundAssets, macthedAssets, precision, recall, fscore, commitIndex, commit);

                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            writer.close();
        }
    }

    public  void combineMeasuresLuceneData() throws IOException {
        File analysisDirectory = projectData.getConfiguration().getAnalysisDirectory();
        String luceneFolder = String.format("%s/lucene",analysisDirectory);

        String[] projects = projectData.getConfiguration().getProjectNamesList();
        String[] shortNames = projectData.getConfiguration().getProjectShortNameList();
        String targetFile = String.format("%s/luceneCombinedMeasuresSummary.csv",projectData.getConfiguration().getrDataFolder());
        File tFile = new File(targetFile);
        if (tFile.exists()) {
            FileUtils.forceDelete(tFile);
        }
        PrintWriter writer = new PrintWriter(new FileWriter(tFile, true));
        System.out.println("COMBINED MEASURES LUCENE DATA");
        try {
            //header
            writer.println("project;feature;measure;measureValue;commitIndex;commit");
            for (int p = 0; p < projects.length; p++) {
                File projectSumaryFile = new File(String.format("%s/%s/resultSummary.csv",luceneFolder,projects[p]));
                List<String> fileLines = FileUtils.readLines(projectSumaryFile, "UTF-8");
                if(!projectSumaryFile.exists()){
                    continue;
                }
                System.out.println(projectSumaryFile);
                for (int i = 1; i < fileLines.size(); i++) {
                    String[] parts = fileLines.get(i).split(";");
                    String project = shortNames[p], feature = parts[0], precision = parts[4], recall = parts[5], fscore = parts[6], commitIndex = parts[7], commit = parts[8];
                    precision = precision.trim().equalsIgnoreCase("NaN") ? "0" : precision;
                    recall = recall.trim().equalsIgnoreCase("NaN") ? "0" : recall;
                    fscore = fscore.trim().equalsIgnoreCase("NaN") ? "0" : fscore;
                    writer.printf("%s;%s;precision;%s;%s;%s\n", project, feature, precision, commitIndex, commit);
                    writer.printf("%s;%s;recall;%s;%s;%s\n", project, feature, recall, commitIndex, commit);
                    writer.printf("%s;%s;fscore;%s;%s;%s\n", project, feature, fscore, commitIndex, commit);

                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            writer.close();
        }
    }
}
