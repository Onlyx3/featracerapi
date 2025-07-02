package se.gu.metrics.ged.javascript;

import me.tongfei.progressbar.ProgressBar;
import org.json.JSONArray;
import org.json.JSONObject;
import se.gu.git.DiffEntry;
import se.gu.main.Configuration;
import se.gu.metrics.ged.CallNode;
import se.gu.metrics.ged.FunctionCall;
import se.gu.parser.fmparser.FeatureTreeNode;
import se.gu.parser.fmparser.IndentedTextToTreeParser;
import se.gu.utils.CommandRunner;
import se.gu.utils.LocalExecutionRunner;
import se.gu.utils.Utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

import static se.gu.utils.Utilities.writeOutputFile;

public class JSCallGraph {
    public LocalExecutionRunner getExecutionRunner() {
        return executionRunner;
    }

    public void setExecutionRunner(LocalExecutionRunner executionRunner) {
        this.executionRunner = executionRunner;
    }

    private LocalExecutionRunner executionRunner;

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    private Configuration configuration;
    private List<CallNode> callNodes;

    public List<CallNode> getCallNodes() {
        return callNodes;
    }

    public void setCallNodes(List<CallNode> callNodes) {
        this.callNodes = callNodes;
    }

    public List<FunctionCall> getFunctionCalls() {
        return functionCalls;
    }

    public void setFunctionCalls(List<FunctionCall> functionCalls) {
        this.functionCalls = functionCalls;
    }

    private List<FunctionCall> functionCalls;
    private CallNode rootNode;
    private String commitHash;

    public String getCommitHash() {
        return commitHash;
    }

    public void setCommitHash(String commitHash) {
        this.commitHash = commitHash;
    }

    public CallNode getRootNode() {
        return rootNode;
    }

    public void setRootNode(CallNode rootNode) {
        this.rootNode = rootNode;
    }

    public JSCallGraph(Configuration configuration, String directory) {
        this.configuration = configuration;
        this.directory = directory;
        callNodes = new ArrayList<>();
        rootNode = new CallNode("ROOTCall", "ROOTCall", "ROOTCall", -1, -1);
        callNodes.add(rootNode);
        functionCalls = new ArrayList<>();
    }

    public JSCallGraph(Configuration configuration, String commitHash, String directory) {
        this(configuration, directory);
        this.commitHash = commitHash;

    }

    private String directory;

    public void printJSCallGraph() throws IOException {

        CommandRunner commandRunner = new CommandRunner(configuration);
        commandRunner.setCallGraphRepository(directory);
        BufferedReader reader = commandRunner.getJSCallGraphOutput();
        String line;
        while ((line = reader.readLine()) != null) {

            String values[] = line.split("->");
            if (values.length >= 2) {
                System.out.printf("\"%s\" -> \"%s\"\n", values[0].split(":")[0].replaceAll("['*(*)*@*]", ""), values[1].split(":")[0].replaceAll("['*(*)*@*]", ""));
            }
        }
        System.out.println("DONE printing");


    }


    public void printJSCallGraphJSON() throws IOException, InterruptedException {

        CommandRunner commandRunner = new CommandRunner(configuration);
        commandRunner.setCallGraphRepository(directory);
        BufferedReader reader = commandRunner.getJSCallGraphJSONOutput();
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
        System.out.println("DONE printing");


    }

    public void createJSCallGraph() throws IOException, InterruptedException {
        CommandRunner commandRunner = new CommandRunner(configuration);
        commandRunner.setCallGraphRepository(directory);
        BufferedReader reader = commandRunner.getJSCallGraphJSONOutput();
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
            stringBuilder.append(System.lineSeparator());
        }
        //System.out.println(stringBuilder.toString());
        //now read json
        JSONArray jsonArray = new JSONArray(stringBuilder.toString());
        System.out.println("Total calls: " + jsonArray.length());

        try (ProgressBar pb = new ProgressBar("Function Call Generation:", jsonArray.length())) {

            for (int i = 0; i < jsonArray.length(); i++) {
                pb.step();

                JSONObject jsonObject = jsonArray.getJSONObject(i);
                JSCallGraphRunner runner = new JSCallGraphRunner(this, jsonObject);
                Future<?> future = executionRunner.submit(runner);
                executionRunner.addFuture(future);


                //System.out.println(i + " " + functionCall);

            }
        }
        executionRunner.waitForTaskToFinish();
        executionRunner.shutdown();
        writeDOTFile();
    }


    /**
     * Goes through commit hitory and creates call graph code base at each commit
     *
     * @throws IOException
     */
    public void writeDOTFile() throws IOException {


        //if dot files fodler doesnt exit, create it
        File dotFilesDirectory = Utilities.createOutputDirectory(String.format("%s\\%s\\%s", configuration.getAnalysisDirectory(),configuration.getCodeAbstractionLevel(), "dotFiles"),false);
        StringBuilder stringBuilder = new StringBuilder();
        for (FunctionCall functionCall : functionCalls) {
            stringBuilder.append(functionCall);
            stringBuilder.append(System.lineSeparator());
        }
        Utilities.writeStringFile(String.format("%s\\%s.txt", dotFilesDirectory.getAbsolutePath(), commitHash), stringBuilder.toString());


    }


}
