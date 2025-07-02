package se.gu.metrics.ged;

import me.tongfei.progressbar.ProgressBar;
import se.gu.assets.Asset;
import se.gu.assets.AssetFunctionCallGenerator;
import se.gu.assets.AssetType;
import se.gu.main.ProjectData;
import se.gu.main.ProjectLanguage;
import se.gu.metrics.ged.javascript.JSCallGraph;
import se.gu.utils.LocalExecutionRunner;
import se.gu.utils.Utilities;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class CallGraphExtractor {
    private ProjectData projectData;
    private String commit;

    public CallGraphExtractor(ProjectData projectData, String commit) {
        this.projectData = projectData;
        this.commit = commit;
    }

    public void extractCallGraph() throws IOException, InterruptedException {
        ProjectLanguage projectLanguage = projectData.getConfiguration().getProjectLanguage();
        //&begin [GED::JavaScript]
        if(projectLanguage == ProjectLanguage.JavaScript){
            LocalExecutionRunner executionRunner = new LocalExecutionRunner();
            JSCallGraph jsCallGraph = new JSCallGraph(projectData.getConfiguration(),commit, projectData.getConfiguration().getProjectRepository().getAbsolutePath());
            jsCallGraph.setExecutionRunner(executionRunner);
            jsCallGraph.createJSCallGraph();
            projectData.setCurrentFunctionCalls(jsCallGraph.getFunctionCalls());
            projectData.setCallNodes(jsCallGraph.getCallNodes());


        }
        //&end [GED::JavaScript]
    }
}
