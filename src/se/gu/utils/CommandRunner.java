package se.gu.utils;

import se.gu.git.Git;
import se.gu.main.Configuration;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

/**
 * @Author: Mukelabai Mukelabai
 * @Date: 2019-06-04
 * @Description: This class contains different kinds of commands, e.g., git commands and commands for generating call graphs
 */
public class CommandRunner  implements Serializable {
    private static final long serialVersionUID = 5602195916652583116L;
    private String command;

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    private Configuration configuration;

    public String getCallGraphRepository() {
        return callGraphRepository;
    }

    public void setCallGraphRepository(String callGraphRepository) {
        this.callGraphRepository = callGraphRepository;
    }

    private String callGraphRepository;

    public CommandRunner(String command) {
        this.command = command;
    }

    public CommandRunner(Configuration configuration) {
        setConfiguration(configuration);
    }

    public BufferedReader getCommitHashes() throws IOException {
        ProcessBuilder builder = new ProcessBuilder("bash", "-c", "git log --pretty=format:\"%h\"");
        builder.directory(configuration.getProjectRepository());
        //start process
        final Process process = builder.start();
        return new BufferedReader(new InputStreamReader(process.getInputStream()));
    }

    public BufferedReader getClaferSimulationCommitHashes() throws IOException {
        ProcessBuilder builder = new ProcessBuilder("bash", "-c", "git --no-pager log --author='Wenbin Ji' --pretty=format:\"%h\"");
        builder.directory(configuration.getProjectRepository());
        //start process
        final Process process = builder.start();
        return new BufferedReader(new InputStreamReader(process.getInputStream()));
    }
    public BufferedReader getClaferSimulationCommitsFromLog() throws IOException {
        ProcessBuilder builder = new ProcessBuilder("bash", "-c",configuration.getSimulationCommitsGitLogCommand());

        builder.directory(configuration.getProjectRepository());
        //start process
        final Process process = builder.start();
        return new BufferedReader(new InputStreamReader(process.getInputStream()));
    }

    public BufferedReader getCurrentDiff() throws IOException {
        ProcessBuilder builder = new ProcessBuilder("bash", "-c", "git diff HEAD");
        builder.directory(configuration.getProjectRepository());
        //start process
        final Process process = builder.start();
        return new BufferedReader(new InputStreamReader(process.getInputStream()));
    }

    public BufferedReader getDiffFromTwoCommits(String commitA, String commitB) throws IOException {
        ProcessBuilder builder = new ProcessBuilder("bash", "-c", String.format("git diff %s %s", commitA, commitB));
        builder.directory(configuration.getProjectRepository());
        //start process
        final Process process = builder.start();
        return new BufferedReader(new InputStreamReader(process.getInputStream()));
    }
    public BufferedReader getFilesThatChangedBetween(String commitA, String commitB) throws IOException {
        ProcessBuilder builder = new ProcessBuilder("bash", "-c", String.format("git diff --name-only %s %s", commitA, commitB));
        builder.directory(configuration.getProjectRepository());
        //start process
        final Process process = builder.start();
        return new BufferedReader(new InputStreamReader(process.getInputStream()));
    }
    public BufferedReader getDiffFromOneCommit(String commit) throws IOException {

        ProcessBuilder builder = new ProcessBuilder("bash", "-c", String.format("git diff -U0 %s~ %s", commit,commit));
        builder.directory(configuration.getProjectRepository());
        //start process
        final Process process = builder.start();
        return new BufferedReader(new InputStreamReader(process.getInputStream()));
    }
    public BufferedReader getGitBlame(String command) throws IOException, ExecutionException, InterruptedException {

        Path repo = Paths.get(configuration.getProjectRepository().getAbsolutePath());
        BufferedReader reader = Git.runCommand(repo,"bash","-c",String.format("git blame -p %s",command.replaceAll(Pattern.quote("\\"),"/")));

        return reader;
//        ProcessBuilder builder = new ProcessBuilder("bash", "-c",command);
//        builder.directory(configuration.getProjectRepository());
//        //start process
//        final Process process = builder.start();
//        return new BufferedReader(new InputStreamReader(process.getInputStream()));
    }
    public void checkOutCommit(String commitHash) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder("bash", "-c", String.format("%s %s",configuration.getCommitCheckoutCommand(),commitHash));
        builder.directory(configuration.getProjectRepository());
        //start process
        final Process process = builder.start();
        process.waitFor();

    }

    public void checkOutFileAtCommit(String commitHash, String relativePath) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder("bash", "-c", String.format("git checkout %s %s",commitHash,relativePath));
        builder.directory(configuration.getProjectRepository());
        //start process
        final Process process = builder.start();
        process.waitFor();

    }
    public BufferedReader getJSCallGraphOutput() throws IOException {
        String processDirectory = getProcessDirectory();
        String command = String.format("js-callgraph --cg \"%s\"", configuration.getProjectRepository());
        //write command to a batch file since executing commands directly doesnt seem to work well

        //TODO:check what OS it is then use correct batch file e.g. .bat on windows

        File batchFile = getBatFile(processDirectory, command);
        //System.out.printf("batch file exists %b",batchFile.exists());


        ProcessBuilder builder = new ProcessBuilder(batchFile.getAbsolutePath());
        builder.directory(new File(processDirectory));
        //start process
        final Process process = builder.start();
        return new BufferedReader(new InputStreamReader(process.getInputStream()));//return output from the command
    }

    public BufferedReader getJSCallGraphJSONOutput() throws IOException, InterruptedException {
        String processDirectory = getProcessDirectory();
        File file = new File(processDirectory + "\\callgraph.json");

        return new BufferedReader(new FileReader(file));
    }

    private String getProcessDirectory() throws FileNotFoundException {
        File file = new File(callGraphRepository);
        if (!file.exists()) {
            throw new FileNotFoundException(String.format("%s does not exist", callGraphRepository));
        }

        return file.isDirectory() ? callGraphRepository : file.getParent();
    }


    private File getBatFile(String processDirectory, String command) throws IOException {
        FileWriter fileWriter = new FileWriter(processDirectory + "\\call.bat", false);
        fileWriter.write(command);
        fileWriter.close();
        return new File(processDirectory + "\\call.bat");
    }
}
