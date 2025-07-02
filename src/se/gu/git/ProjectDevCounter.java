package se.gu.git;

import org.apache.commons.io.FileUtils;
import se.gu.main.Configuration;
import se.gu.utils.CommandRunner;
import sun.util.calendar.Gregorian;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectDevCounter {
    private Configuration configuration;

    public ProjectDevCounter(Configuration configuration) {
        this.configuration = configuration;
    }

    public void printProjectDevCounts() throws Exception {

        File output = new File(String.format("%s/projectstats.csv", configuration.getAnalysisDirectory()));
        if(output.exists()){
            FileUtils.forceDelete(output);
        }
        PrintWriter printWriter = new PrintWriter(new FileWriter(output, true));
        printWriter.printf("project;devCount;commitCount;yearMin;yearmax\n");
        DiffExtractor diffExtractor = new DiffExtractor(configuration);
        CommandRunner commandRunner = new CommandRunner(configuration);
        try {
        List<ProjectStat> projectStats = new ArrayList<>();
        List<String> simulationCommitAuthors = configuration.getSimulationCommitAuthorsList();
        Calendar cal = Calendar.getInstance();
        for (File repo : configuration.getProjectCopiedRepositoriesForStats()) {

            int commitsToExecute = configuration.getCommitsToExecute();
            configuration.setProjectRepository(repo);
            configuration.setProjectType(ProjectType.REGULAR);
            diffExtractor.setCommitHistory();
            List<Commit> ch = diffExtractor.getCommitHistory();
            List<Commit> commitHistory = ch.stream().filter(c->ch.indexOf(c)<commitsToExecute).collect(Collectors.toList());
            ProjectStat projectStat = new ProjectStat();
            projectStat.setProjectName(repo.getName());
            long devCount = commitHistory.stream().map(Commit::getAuthor).filter(a -> !simulationCommitAuthors.contains(a)).distinct().count();
            projectStat.setDevCount(devCount);
            long commitCount = commitHistory.stream().map(Commit::getAuthor).filter(a -> !simulationCommitAuthors.contains(a)).count();
            projectStat.setCommitCount(commitCount);
            List<Integer> years = new ArrayList<>();
            commitHistory.stream().map(Commit::getCommitDate).forEach(d -> {
                cal.setTime(d);
                years.add(cal.get(Calendar.YEAR));
            });
            long yearMin = years.stream().mapToInt(Integer::intValue).min().getAsInt();
            long yearMax = years.stream().mapToInt(Integer::intValue).max().getAsInt();
            projectStat.setYearMax(yearMax);
            projectStat.setYearMin(yearMin);

                printWriter.printf("%s;%d;%d;%d;%d\n", projectStat.getProjectName(), projectStat.getDevCount(), projectStat.getCommitCount(), projectStat.getYearMin(), projectStat.getYearMax());



        }

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (printWriter != null) {
                printWriter.close();
            }
        }
    }
}

class ProjectStat {
    private long devCount;
    private long commitCount;

    public long getDevCount() {
        return devCount;
    }

    public void setDevCount(long devCount) {
        this.devCount = devCount;
    }

    public long getCommitCount() {
        return commitCount;
    }

    public void setCommitCount(long commitCount) {
        this.commitCount = commitCount;
    }

    public long getYearMin() {
        return yearMin;
    }

    public void setYearMin(long yearMin) {
        this.yearMin = yearMin;
    }

    public long getYearMax() {
        return yearMax;
    }

    public void setYearMax(long yearMax) {
        this.yearMax = yearMax;
    }

    private long yearMin;
    private long yearMax;
    private String projectName;

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }


}
