package se.gu.metrics.ged;

import se.gu.main.ProjectData;
import se.gu.main.ProjectLanguage;
import se.gu.metrics.ged.javascript.JSCallGraph;
import se.gu.utils.LocalExecutionRunner;

import java.io.IOException;

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
