package se.gu.git;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Git implements Serializable {

    private static final long serialVersionUID = -6118184827935938566L;

    // example of usage
    private static void initAndAddFile() throws IOException, InterruptedException, ExecutionException {
        Path directory = Paths.get("c:\\temp\\example");
        Files.createDirectories(directory);
        gitInit(directory);
        Files.write(directory.resolve("example.txt"), new byte[0]);
        gitStage(directory);
        gitCommit(directory, "Add example.txt");
    }

    // example of usage
    private static void cloneAndAddFile() throws IOException, InterruptedException, ExecutionException {
        String originUrl = "https://github.com/Crydust/TokenReplacer.git";
        Path directory = Paths.get("c:\\temp\\TokenReplacer");
        gitClone(directory, originUrl);
        Files.write(directory.resolve("example.txt"), new byte[0]);
        gitStage(directory);
        gitCommit(directory, "Add example.txt");
        gitPush(directory);
    }

    public static void gitInit(Path directory) throws IOException, InterruptedException, ExecutionException {
        runCommand(directory, "git", "init");
    }

    public static void gitStage(Path directory) throws IOException, InterruptedException, ExecutionException {
        runCommand(directory, "git", "add", "-A");
    }

    public static BufferedReader getCommitHashes(Path directory) throws IOException, InterruptedException, ExecutionException {
        return runCommand(directory, "git", "--no-pager", "log", "--graph", "--pretty=format:'_%H _%ad _%an _%s'", "--date=iso");
    }

    public static BufferedReader gitCheckout(Path directory, String commit) throws IOException, InterruptedException, ExecutionException {
        return runCommand(directory, "git", "reset", "--hard", commit);
    }

    public static BufferedReader gitDiff(Path directory, String commit) throws IOException, InterruptedException, ExecutionException {
        return runCommand(directory, "git", "diff", "~3195dfd2df22c7ea9e51061b7da82582425c6989", "3195dfd2df22c7ea9e51061b7da82582425c6989");
    }

    public static void gitCommit(Path directory, String message) throws IOException, InterruptedException, ExecutionException {
        runCommand(directory, "git", "commit", "-m", message);
    }

    public static void gitPush(Path directory) throws IOException, InterruptedException, ExecutionException {
        runCommand(directory, "git", "push");
    }

    public static void gitClone(Path directory, String originUrl) throws IOException, InterruptedException, ExecutionException {
        runCommand(directory.getParent(), "git", "clone", originUrl, directory.getFileName().toString());
    }

    public static BufferedReader runCommand(Path directory, String... command) throws IOException, InterruptedException, ExecutionException {
        Objects.requireNonNull(directory, "directory");
        if (!Files.exists(directory)) {
            throw new RuntimeException("can't run command in non-existing directory '" + directory + "'");
        }
        ProcessBuilder pb = new ProcessBuilder()
                .command(command)
                .directory(directory.toFile());
        Process p = pb.start();
        BufferedReader bufferedReader = getBufferedReader(p);
        return bufferedReader;


    }

    public static BufferedReader getBufferedReader(Process p) throws InterruptedException, ExecutionException {
        StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), "ERROR");
        StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), "OUTPUT");
        GitExecutorService executorService = new GitExecutorService();
        executorService.addFuture(executorService.submit(outputGobbler));
        //errorGobbler.run();
        executorService.waitForTaskToFinish();
        executorService.shutdown();

        Future<BufferedReader> bufferedReaderFuture = executorService.getFutures().get(0);

        return bufferedReaderFuture.get();
    }


}
